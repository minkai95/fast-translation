package com.plugin.fasttranslation.setting;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

public class SettingsConfigurable implements Configurable {
    private ComboBox<String> languageComboBox;

    // 映射语言名和语言代码
    private static final Map<String, String> LANGUAGE_MAP = new HashMap<>(){
        {
            put("简体中文", "zh");
            put("繁体中文", "zh-TW");
            put("English", "en");
            put("日本語", "ja");
            put("한국어", "ko");
            put("Français", "fr");
            put("Deutsch", "de");
        }
    };

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Fast-Translation";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS)); // 主面板纵向排列

        // 第一行: Coding language: English
        JPanel codingLanguagePanel = new JPanel();
        codingLanguagePanel.setLayout(new BoxLayout(codingLanguagePanel, BoxLayout.X_AXIS));
        JLabel codingLanguageLabel = new JLabel("Coding language: ");
        JLabel codingLanguageValue = new JLabel("          English");
        codingLanguagePanel.add(codingLanguageLabel);
        codingLanguagePanel.add(codingLanguageValue);
        codingLanguagePanel.add(Box.createHorizontalGlue()); // 使控件靠左

        // 第二行: Your Native Language: + 下拉框
        JPanel nativeLanguagePanel = new JPanel();
        nativeLanguagePanel.setLayout(new BoxLayout(nativeLanguagePanel, BoxLayout.X_AXIS));
        JLabel nativeLanguageLabel = new JLabel("Your Native Language: ");
        JComboBox<String> nativeLanguageComboBox = createLanguageComboBox();
        nativeLanguageComboBox.setMaximumSize(new Dimension(150, 30)); // 限制下拉框大小
        nativeLanguageComboBox.setPreferredSize(new Dimension(150, 30)); // 首选大小

        nativeLanguagePanel.add(nativeLanguageLabel);
        nativeLanguagePanel.add(nativeLanguageComboBox);
        nativeLanguagePanel.add(Box.createHorizontalGlue()); // 使控件靠左

        // 添加到主面板
        mainPanel.add(codingLanguagePanel);
        mainPanel.add(Box.createVerticalStrut(10)); // 添加垂直间距
        mainPanel.add(nativeLanguagePanel);

        return mainPanel;
    }

    // 创建下拉框的方法
    private JComboBox<String> createLanguageComboBox() {
        languageComboBox = new ComboBox<>(LANGUAGE_MAP.keySet().toArray(new String[0]));

        // 加载当前配置
        String currentLanguageCode = FastTranslationSettings.getInstance().nativeLanguage;
        if (currentLanguageCode == null || currentLanguageCode.isEmpty()) {
            currentLanguageCode = "zh"; // 默认语言为中文
        }

        String finalCurrentLanguageCode = currentLanguageCode;
        LANGUAGE_MAP.forEach((name, code) -> {
            if (code.equals(finalCurrentLanguageCode)) {
                languageComboBox.setSelectedItem(name);
            }
        });

        return languageComboBox;
    }

    @Override
    public boolean isModified() {
        // 检查是否有未保存的更改
        String selectedLanguageCode = LANGUAGE_MAP.get(languageComboBox.getSelectedItem());
        return !selectedLanguageCode.equals(FastTranslationSettings.getInstance().nativeLanguage);
    }

    @Override
    public void apply() {
        // 保存设置
        String selectedLanguageCode = LANGUAGE_MAP.get(languageComboBox.getSelectedItem());
        FastTranslationSettings.getInstance().nativeLanguage = selectedLanguageCode;
    }
}
