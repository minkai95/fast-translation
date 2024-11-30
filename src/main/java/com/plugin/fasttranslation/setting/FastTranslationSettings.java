package com.plugin.fasttranslation.setting;


import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "FastTranslationSettings",
        storages = @Storage("FastTranslationSettings.xml")
)
@Service(Service.Level.APP) // 或者 Service.Level.PROJECT
public final class FastTranslationSettings implements PersistentStateComponent<FastTranslationSettings> {

    public String nativeLanguage = "zh"; // 一个简单的配置字段
    public static FastTranslationSettings getInstance() {
        return ServiceManager.getService(FastTranslationSettings.class);
    }

    @Nullable
    @Override
    public FastTranslationSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull FastTranslationSettings state) {
        this.nativeLanguage = state.nativeLanguage;
    }
}
