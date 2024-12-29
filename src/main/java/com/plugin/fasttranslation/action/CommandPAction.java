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
import com.intellij.openapi.ui.popup.JBPopup;
import com.plugin.fasttranslation.util.I18nUtil;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.icons.AllIcons;

import java.io.StringReader;
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
            selectedText = parseAndFormat(selectedText);
            if (selectedText.length() > 5000) {
                showNotification(project, "Translation length cannot exceed 1000.");
                return;
            }
            // 自定义逻辑：将选中文本转换
            Pair<Boolean, String> transformedTextPair = RequestTencent.translateBatch(selectedText);
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

    public static String parseAndFormat(String rawComment) {
        if (rawComment == null || rawComment.trim().isEmpty()) {
            return "";
        }

        // 预处理注释，移除开头的/**和结尾的*/
        String processedComment = rawComment.trim()
                .replaceAll("^/\\*\\*\\s*", "")
                .replaceAll("\\s*\\*/$", "");

        String mockClass = String.format(
                "public class Mock {\n" +
                        "    /**\n" +
                        "%s\n" +
                        "    */\n" +
                        "    public void mockMethod() {}\n" +
                        "}", processedComment);

        JavaProjectBuilder builder = new JavaProjectBuilder();
        try {
            builder.addSource(new StringReader(mockClass));
            JavaClass javaClass = builder.getClasses().iterator().next();
            JavaMethod method = javaClass.getMethods().get(0);

            StringBuilder formatted = new StringBuilder();

            // 处理主要描述
            String mainDescription = formatDescription(method.getComment());
            if (!mainDescription.isEmpty()) {
                formatted.append(mainDescription).append("\n");
            }

            // 处理@param标签
            List<DocletTag> paramTags = method.getTagsByName("param");
            if (!paramTags.isEmpty()) {
                formatted.append("Params:\n");
                for (DocletTag param : paramTags) {
                    List<String> parameters = param.getParameters();
                    if (!parameters.isEmpty()) {
                        formatted.append("     ")
                                .append(parameters.get(0))
                                .append(" – ")
                                .append(formatTagContent(param.getValue().replaceFirst("^" + parameters.get(0) + "\\s+", "")))
                                .append("\n");
                    }
                }
            }

            // 处理@return标签
            DocletTag returnTag = method.getTagByName("return");
            if (returnTag != null) {
                formatted.append("Returns:\n")
                        .append("     ")
                        .append(formatTagContent(returnTag.getValue()))
                        .append("\n");
            }

            // 处理@throws标签
            List<DocletTag> throwsTags = method.getTagsByName("throws");
            if (!throwsTags.isEmpty()) {
                formatted.append("Throws:\n");
                for (DocletTag throwsTag : throwsTags) {
                    List<String> parameters = throwsTag.getParameters();
                    if (!parameters.isEmpty()) {
                        String className = parameters.get(0);
                        // 移除完整的包名，只保留类名
                        className = className.substring(className.lastIndexOf('.') + 1);
                        formatted.append("     ")
                                .append(className)
                                .append(" – ")
                                .append(formatTagContent(throwsTag.getValue().replaceFirst("^" + parameters.get(0) + "\\s+", "")))
                                .append("\n");
                    }
                }
            }

            // 处理@since标签
            DocletTag sinceTag = method.getTagByName("since");
            if (sinceTag != null) {
                formatted.append("Since:\n")
                        .append("     ")
                        .append(formatTagContent(sinceTag.getValue()))
                        .append("\n");
            }

            return formatted.toString().trim();
        } catch (Exception e) {
            return "Error parsing JavaDoc: " + e.getMessage();
        }
    }

    private static String formatDescription(String text) {
        if (text == null) return "";

        // 移除每行开头的*和空格
        text = text.replaceAll("(?m)^\\s*\\*\\s*", "");

        // 处理HTML标签
        text = text.replaceAll("<tt>|</tt>", "");
        text = text.replaceAll("<pre>\\s*", "\n     ");
        text = text.replaceAll("</pre>", "");
        text = text.replaceAll("</?p>", "\n");
        text = text.replaceAll("<cite>|</cite>", "");

        // 处理链接
        text = text.replaceAll("<a\\s+href=\"[^\"]*\">([^<]*)</a>", "$1");

        // 处理特殊字符
        text = text.replaceAll("&trade;", "™");

        // 合并多行文本，保留必要的换行
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inPreBlock = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("     ")) {
                // 这是预格式化的代码块
                if (!inPreBlock) {
                    result.append("\n");
                    inPreBlock = true;
                }
                result.append(line).append("\n");
            } else {
                if (inPreBlock) {
                    inPreBlock = false;
                } else if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(line);
            }
        }

        return result.toString().trim();
    }

    private static String formatTagContent(String text) {
        if (text == null) return "";

        // 移除HTML标签和特殊格式
        text = text.replaceAll("<[^>]+>", "");
        text = text.replaceAll("&trade;", "™");

        // 合并多行为单行
        return text.replaceAll("\\s+", " ").trim();
    }
}
