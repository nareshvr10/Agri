package com.example.core

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.models.AppLanguage
import java.util.Locale

fun Context.localizedContext(language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.code)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

@Composable
fun ProvideLocalizedContext(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val localized = remember(baseContext, language) {
        baseContext.localizedContext(language)
    }
    CompositionLocalProvider(LocalContext provides localized) {
        content()
    }
}
