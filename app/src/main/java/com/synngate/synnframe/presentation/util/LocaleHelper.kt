package com.synngate.synnframe.presentation.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

// Добавьте этот класс в вашем проекте, например в пакете utils
class LocaleHelper {
    companion object {
        fun updateLocale(context: Context, languageCode: String): Context {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)

            return context.createConfigurationContext(configuration)
        }
    }
}