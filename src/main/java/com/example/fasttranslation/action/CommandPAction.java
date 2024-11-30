package com.example.fasttranslation.action;

import com.example.fasttranslation.setting.FastTranslationSettings;
import com.example.fasttranslation.thirdparty.RequestTencent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandPAction extends AnAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandPAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前的项目和编辑器

        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (editor != null) {
            // 获取选中的文本
            SelectionModel selectionModel = editor.getSelectionModel();
            String selectedText = selectionModel.getSelectedText();
            if (StringUtils.isBlank(selectedText)) {
                showNotification(project, "Please select the translated text.");
                return;
            }
            selectedText = selectedText.trim();
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
                // 如果文档不可写，弹窗显示翻译后的文本
                Messages.showInfoMessage(translatedText, "Translation Result");
            } else {
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
            }

        }
    }

    private void showNotification(Project project, String message) {
        // 创建并显示通知
        Notification notification = new Notification(
                "com.example.fasttranslation",
                "提示", // 通知的内容
                message, // 弹出的提示内容
                NotificationType.INFORMATION // 通知的类型
        );
        Notifications.Bus.notify(notification, project);
    }

}
