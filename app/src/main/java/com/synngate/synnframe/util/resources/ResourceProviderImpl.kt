package com.synngate.synnframe.util.resources

import android.content.Context

// com.synngate.synnframe.util.resources.ResourceProviderImpl.kt
class ResourceProviderImpl(
    private val context: Context
) : ResourceProvider {
    override fun getString(resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
}