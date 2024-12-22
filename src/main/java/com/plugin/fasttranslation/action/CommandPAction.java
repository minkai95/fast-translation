package com.plugin.fasttranslation.action;

import com.intellij.openapi.ui.popup.IconButton;
import com.plugin.fasttranslation.setting.FastTranslationSettings;
import com.plugin.fasttranslation.thirdparty.RequestTencent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import javax.swing.*;
import java.awt.*;
import com.intellij.openapi.ui.popup.JBPopup;
import com.plugin.fasttranslation.util.I18nUtil;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.icons.AllIcons;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.intellij.util.IconUtil;

public class CommandPAction extends AnAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandPAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前的项目和编辑器

        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (editor != null) {
            // 获取选中的文本，包括折叠区域
            SelectionModel selectionModel = editor.getSelectionModel();
            FoldingModel foldingModel = editor.getFoldingModel();
            int caretOffset = editor.getCaretModel().getOffset();

            String selectedText = null;
            // 首先检查是否在折叠区域内
            FoldRegion[] foldRegions = foldingModel.getAllFoldRegions();
            for (FoldRegion region : foldRegions) {
                if (region.isValid() && !region.isExpanded()) {
                    int startOffset = region.getStartOffset();
                    int endOffset = region.getEndOffset();
                    if (caretOffset >= startOffset && caretOffset <= endOffset) {
                        selectedText = editor.getDocument().getText(TextRange.create(startOffset, endOffset));
                        break;
                    }
                }
            }

            // 如果不在折叠区域内，使用普通的选择
            if (selectedText == null) {
                int selectionStart = selectionModel.getSelectionStart();
                int selectionEnd = selectionModel.getSelectionEnd();
                if (selectionStart != selectionEnd) {
                    selectedText = editor.getDocument().getText(TextRange.create(selectionStart, selectionEnd));
                }
            }

            if (StringUtils.isBlank(selectedText)) {
                showNotification(project, "Please select the translated text.");
                return;
            }
            selectedText = selectedText.trim();
            selectedText = removeMultilineComment(selectedText);
            if (selectedText.length() > 5000) {
                showNotification(project, "Translation length cannot exceed 1000.");
                return;
            }
            // 自定义逻辑：将选中文本转换
            Pair<Boolean, String> transformedTextPair = RequestTencent.translate(selectedText);
            if (!transformedTextPair.getFirst()) {
                showNotification(project, "Translation failed,please try again.");
                return;
            }
            String translatedText = transformedTextPair.getSecond();
            // 判断文档是否可写
            if (!editor.getDocument().isWritable()) {
                // 创建自定义的非模态窗口显示翻译结果
                showTranslationPopup(translatedText, editor);
            } else {
                if ("replace".equals(FastTranslationSettings.getInstance().afterTranslation)) {
                    // 使用 WriteCommandAction 来修改文档
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        // 执行替换操作
                        editor.getDocument().replaceString(
                                selectionModel.getSelectionStart(),
                                selectionModel.getSelectionEnd(),
                                translatedText
                        );
                        // 确保光标位于替换后的文本位置
                        editor.getCaretModel().moveToOffset(selectionModel.getSelectionStart() + translatedText.length());
                    });
                    // 取消选中
                    selectionModel.removeSelection();
                } else {
                    showTranslationPopup(translatedText, editor);
                }
            }
        }
    }

    private void showNotification(Project project, String message) {
        // 创建并显示通知
        Notification notification = new Notification(
                "com.plugin.fasttranslation",
                "Tips", // 通知的内容
                message, // 弹出的提示内容
                NotificationType.INFORMATION // 通知的类型
        );
        Notifications.Bus.notify(notification, project);
    }

    /**
     * 去掉多行注释符号，保留注释内容
     * @param input 输入字符串
     * @return 去掉注释符号的内容
     */
    public static String removeMultilineComment(String input) {
        // 匹配Java多行注释
        String commentRegex = "/\\*\\*|/\\*|\\*/";
        Pattern pattern = Pattern.compile(commentRegex);
        Matcher matcher = pattern.matcher(input);

        // 去除所有注释符号
        String processed = matcher.replaceAll("");

        // 去除每行开头的 '*' 符号
        String[] lines = processed.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            line = line.trim(); // 去掉行首和行尾的空格
            if (line.startsWith("*")) {
                line = line.substring(1).trim(); // 去掉每行开头的 '*' 符号
            }
            result.append(line).append("\n");
        }

        // 返回处理后的字符串，去掉末尾多余的换行符
        return result.toString().trim();
    }

    /**
     * 显示翻译结果的弹出窗口
     * @param translatedText 翻译后的文本
     * @param editor 编辑器实例
     */
    private void showTranslationPopup(String translatedText, Editor editor) {
        // 创建一个面板来容纳文本
        JPanel panel = new JPanel(new BorderLayout());
        
        // 使用JTextArea来显示文本，支持自动换行
        JTextArea textArea = new JTextArea(translatedText);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setBackground(null);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        
        // 计算合适的大小
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int maxWidth = 600; // 最大宽度
        String[] lines = translatedText.split("\n");
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, fm.stringWidth(line) + 20);
        }
        width = Math.min(width, maxWidth);
        
        // 根据文本内容计算所需的行数
        int lineHeight = fm.getHeight();
        int totalHeight = 0;
        for (String line : lines) {
            int lineWidth = fm.stringWidth(line);
            int wrappedLines = (int) Math.ceil((double) lineWidth / width);
            totalHeight += wrappedLines * lineHeight;
        }
        
        // 设置首选大小
        Dimension preferredSize = new Dimension(width, Math.min(totalHeight + 20, 400));
        textArea.setPreferredSize(preferredSize);
        
        panel.add(textArea, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建弹出窗口
        Icon closeIcon = IconUtil.scale(AllIcons.Actions.Close, null, 1.5f);
        Icon hoveredIcon = IconUtil.scale(AllIcons.Actions.CloseHovered, null, 1.5f);
        
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, textArea)
                .setTitle(I18nUtil.getMessage("translate.result.pop.title"))
                .setMovable(true)
                .setRequestFocus(true)
                .setResizable(true)
                .setMinSize(new Dimension(200, 100))
                .setCancelOnClickOutside(true)
                .setCancelButton(new IconButton(I18nUtil.getMessage("translate.result.pop.close.tip"), closeIcon, hoveredIcon))
                .createPopup();

        // 显示在编辑器的当前位置附近
        popup.showInBestPositionFor(editor);
    }
}
