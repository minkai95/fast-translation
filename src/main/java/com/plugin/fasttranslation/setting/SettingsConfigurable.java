package com.plugin.fasttranslation.setting;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.WindowManager;
import com.plugin.fasttranslation.util.I18nUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsConfigurable implements Configurable {
    private SettingsComponent settingsComponent;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return I18nUtil.getMessage("setting.title");
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new SettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        String selectedLanguage = (String) settingsComponent.getLanguageComboBox().getSelectedItem();
        String selectedAfterTranslation = settingsComponent.getSelectedAfterTranslationValue();
        
        Locale selectedLocale = SettingsComponent.LANGUAGE_MAP.get(selectedLanguage);
        String currentLanguageCode = FastTranslationSettings.getInstance().nativeLanguage;
        String currentAfterTranslation = FastTranslationSettings.getInstance().afterTranslation;
        
        if (currentLanguageCode == null || currentLanguageCode.isEmpty()) {
            currentLanguageCode = "zh";
        }
        if (currentAfterTranslation == null || currentAfterTranslation.isEmpty()) {
            currentAfterTranslation = "popup";
        }
        
        return !selectedLocale.getLanguage().equals(currentLanguageCode)
                || !selectedAfterTranslation.equals(currentAfterTranslation);
    }

    @Override
    public void apply() {
        String selectedLanguage = (String) settingsComponent.getLanguageComboBox().getSelectedItem();
        String selectedAfterTranslation = settingsComponent.getSelectedAfterTranslationValue();
        
        Locale selectedLocale = SettingsComponent.LANGUAGE_MAP.get(selectedLanguage);
        FastTranslationSettings.getInstance().nativeLanguage = selectedLocale.getLanguage();
        if (selectedLocale == Locale.TRADITIONAL_CHINESE) {
            FastTranslationSettings.getInstance().nativeLanguage = selectedLocale.getLanguage() + "-" + selectedLocale.getCountry();
        }
        FastTranslationSettings.getInstance().afterTranslation = selectedAfterTranslation;
        
        // 应用设置后立即更新UI
        settingsComponent.updateUILanguage();
    }

    @Override
    public void reset() {
        settingsComponent.loadSettings(FastTranslationSettings.getInstance());
    }

    public static class SettingsComponent {
        private final ComboBox<String> languageComboBox;
        private final ComboBox<String> afterTranslationComboBox;
        private final JLabel codingLanguageLabel;
        private final JLabel codingLanguageValue;
        private final JLabel nativeLanguageLabel;
        private final JLabel afterTranslationLabel;
        private final JPanel mainPanel;

        // 映射语言名和对应的 Locale
        public static final Map<String, Locale> LANGUAGE_MAP = new LinkedHashMap<>() {
            {
                put("简体中文", Locale.SIMPLIFIED_CHINESE);
                put("繁体中文", Locale.TRADITIONAL_CHINESE);
                put("English", Locale.ENGLISH);
                put("日本語", Locale.JAPAN);
                put("한국어", Locale.KOREA);
                put("Français", Locale.FRANCE);
                put("Deutsch", Locale.GERMANY);
            }
        };

        private static final Map<String, String> AFTER_TRANSLATION_MAP = new HashMap<>() {
            {
                put("setting.after.translate.val.pop", "popup");
                put("setting.after.translate.val.replace", "replace");
            }
        };

        public SettingsComponent() {
            mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints labelConstraints = new GridBagConstraints();
            GridBagConstraints valueConstraints = new GridBagConstraints();

            // 配置标签列约束
            labelConstraints.gridx = 0;
            labelConstraints.anchor = GridBagConstraints.LINE_START;
            labelConstraints.insets = new Insets(0, 5, 5, 10);
            labelConstraints.fill = GridBagConstraints.HORIZONTAL;
            labelConstraints.weightx = 0.0;

            // 配置值列约束
            valueConstraints.gridx = 1;
            valueConstraints.anchor = GridBagConstraints.LINE_START;
            valueConstraints.insets = new Insets(0, 0, 5, 5);
            valueConstraints.fill = GridBagConstraints.HORIZONTAL;
            valueConstraints.weightx = 1.0;

            // 创建组件
            codingLanguageLabel = new JLabel(I18nUtil.getMessage("setting.code.lang.key") + ": ");
            codingLanguageValue = new JLabel(I18nUtil.getMessage("setting.code.lang.val"));
            nativeLanguageLabel = new JLabel(I18nUtil.getMessage("setting.your.native.lang.key") + ": ");
            afterTranslationLabel = new JLabel(I18nUtil.getMessage("setting.after.translate.key") + ": ");

            // 创建下拉框
            languageComboBox = createLanguageComboBox();
            afterTranslationComboBox = createAfterTranslationComboBox();

            // 设置自定义渲染器
            languageComboBox.setRenderer(new CustomComboBoxRenderer(languageComboBox));
            afterTranslationComboBox.setRenderer(new CustomComboBoxRenderer(afterTranslationComboBox));

            // 添加组件到面板
            labelConstraints.gridy = 0;
            valueConstraints.gridy = 0;
            mainPanel.add(codingLanguageLabel, labelConstraints);
            mainPanel.add(codingLanguageValue, valueConstraints);

            labelConstraints.gridy = 1;
            valueConstraints.gridy = 1;
            mainPanel.add(nativeLanguageLabel, labelConstraints);
            mainPanel.add(languageComboBox, valueConstraints);

            labelConstraints.gridy = 2;
            valueConstraints.gridy = 2;
            mainPanel.add(afterTranslationLabel, labelConstraints);
            mainPanel.add(afterTranslationComboBox, valueConstraints);

            // 添加垂直弹簧推动组件到顶部
            GridBagConstraints glueConstraints = new GridBagConstraints();
            glueConstraints.gridx = 0;
            glueConstraints.gridy = 3;
            glueConstraints.gridwidth = 2;
            glueConstraints.weighty = 1.0;
            glueConstraints.fill = GridBagConstraints.VERTICAL;
            mainPanel.add(Box.createVerticalGlue(), glueConstraints);

            // 添加水平弹簧防止组件被拉伸
            GridBagConstraints horizontalGlueConstraints = new GridBagConstraints();
            horizontalGlueConstraints.gridx = 2;
            horizontalGlueConstraints.gridy = 0;
            horizontalGlueConstraints.gridheight = 3;
            horizontalGlueConstraints.weightx = 0.0;
            horizontalGlueConstraints.fill = GridBagConstraints.HORIZONTAL;
            mainPanel.add(Box.createHorizontalGlue(), horizontalGlueConstraints);
        }

        private String findLongestString(JComboBox<String> comboBox) {
            String longest = "";
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                String item = comboBox.getItemAt(i).toString();
                if (item.length() > longest.length()) {
                    longest = item;
                }
            }
            return longest;
        }

        private void refreshIdeWindows() {
            SwingUtilities.invokeLater(() -> {
                // 刷新所有打开的项目窗口
                Project[] projects = ProjectManager.getInstance().getOpenProjects();
                for (Project project : projects) {
                    JFrame frame = WindowManager.getInstance().getFrame(project);
                    if (frame != null) {
                        frame.invalidate();
                        frame.validate();
                        frame.getContentPane().doLayout();
                        frame.update(frame.getGraphics());
                    }
                }
            });
        }

        public void updateUILanguage() {
            SwingUtilities.invokeLater(() -> {
                // Update label texts
                codingLanguageLabel.setText(I18nUtil.getMessage("setting.code.lang.key") + ": ");
                codingLanguageValue.setText(I18nUtil.getMessage("setting.code.lang.val"));
                nativeLanguageLabel.setText(I18nUtil.getMessage("setting.your.native.lang.key") + ": ");
                afterTranslationLabel.setText(I18nUtil.getMessage("setting.after.translate.key") + ": ");

                // Update combobox items
                updateAfterTranslationComboBoxItems();

                // Refresh UI
                mainPanel.revalidate();
                mainPanel.repaint();

                // Refresh IDE windows
                refreshIdeWindows();
            });
        }

        private ComboBox<String> createLanguageComboBox() {
            ComboBox<String> comboBox = new ComboBox<>(LANGUAGE_MAP.keySet().toArray(new String[0]));
            
            // 添加语言切换监听器
            comboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedLanguage = (String) e.getItem();
                    Locale newLocale = LANGUAGE_MAP.get(selectedLanguage);
                    I18nUtil.setLocale(newLocale);
                    updateUILanguage();
                }
            });

            return comboBox;
        }

        private ComboBox<String> createAfterTranslationComboBox() {
            ComboBox<String> comboBox = new ComboBox<>();
            AFTER_TRANSLATION_MAP.keySet().forEach(key -> {
                comboBox.addItem(I18nUtil.getMessage(key));
            });
            return comboBox;
        }

        private void updateAfterTranslationComboBoxItems() {
            String currentValue = getSelectedAfterTranslationValue();
            afterTranslationComboBox.removeAllItems();
            
            AFTER_TRANSLATION_MAP.keySet().forEach(key -> {
                afterTranslationComboBox.addItem(I18nUtil.getMessage(key));
            });
            
            if (currentValue != null) {
                final String finalCurrentValue = currentValue;
                AFTER_TRANSLATION_MAP.forEach((key, value) -> {
                    if (value.equals(finalCurrentValue)) {
                        afterTranslationComboBox.setSelectedItem(I18nUtil.getMessage(key));
                    }
                });
            }
        }

        public String getSelectedAfterTranslationValue() {
            String selectedText = (String) afterTranslationComboBox.getSelectedItem();
            if (selectedText == null) return null;
            
            // 通过遍历找到对应的值
            for (Map.Entry<String, String> entry : AFTER_TRANSLATION_MAP.entrySet()) {
                if (I18nUtil.getMessage(entry.getKey()).equals(selectedText)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        public JComponent getPanel() {
            return mainPanel;
        }

        public ComboBox<String> getLanguageComboBox() {
            return languageComboBox;
        }

        public void loadSettings(FastTranslationSettings settings) {
            String currentLanguageCode = settings.nativeLanguage;
            if (currentLanguageCode == null || currentLanguageCode.isEmpty()) {
                currentLanguageCode = "zh";
            }

            // 设置当前选中的语言
            for (Map.Entry<String, Locale> entry : LANGUAGE_MAP.entrySet()) {
                if (entry.getValue().getLanguage().equals(currentLanguageCode)) {
                    languageComboBox.setSelectedItem(entry.getKey());
                    break;
                }
            }

            // 设置当前翻译后行为
            String currentAfterTranslation = settings.afterTranslation;
            if (currentAfterTranslation == null || currentAfterTranslation.isEmpty()) {
                currentAfterTranslation = "popup";
            }

            final String finalCurrentAfterTranslation = currentAfterTranslation;
            AFTER_TRANSLATION_MAP.forEach((key, value) -> {
                if (value.equals(finalCurrentAfterTranslation)) {
                    afterTranslationComboBox.setSelectedItem(I18nUtil.getMessage(key));
                }
            });
        }

        private static class CustomComboBoxRenderer extends DefaultListCellRenderer {
            private int maxWidth = 0;
            private final JComboBox<?> comboBox;

            public CustomComboBoxRenderer(JComboBox<?> comboBox) {
                this.comboBox = comboBox;
                calculateMaxWidth();
            }

            private void calculateMaxWidth() {
                FontMetrics metrics = getFontMetrics(getFont());
                for (int i = 0; i < comboBox.getItemCount(); i++) {
                    Object item = comboBox.getItemAt(i);
                    int width = metrics.stringWidth(item.toString()) + 30; // Add padding
                    maxWidth = Math.max(maxWidth, width);
                }
            }

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setPreferredSize(new Dimension(maxWidth, getPreferredSize().height));
                return this;
            }
        }
    }
}
