package com.synngate.synnframe.util.resources

import android.content.Context
import androidx.annotation.StringRes

class ResourceProviderImpl(
    private val context: Context
) : ResourceProvider {

    override fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
}