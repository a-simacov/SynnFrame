package com.synngate.synnframe.util.resources

import android.content.Context
import androidx.annotation.StringRes
import com.synngate.synnframe.presentation.util.LocaleHelper
import java.util.Locale

class ResourceProviderImpl(
    private val context: Context
) : ResourceProvider {

    override fun getString(@StringRes resId: Int): String {
        // Получаем текущую локаль из системы
        val currentLocale = Locale.getDefault()
        // Создаем контекст с актуальной локалью
        val localizedContext = LocaleHelper.updateLocale(context, currentLocale.language)
        return localizedContext.getString(resId)
    }

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        // Получаем текущую локаль из системы
        val currentLocale = Locale.getDefault()
        // Создаем контекст с актуальной локалью
        val localizedContext = LocaleHelper.updateLocale(context, currentLocale.language)
        return localizedContext.getString(resId, *formatArgs)
    }
}