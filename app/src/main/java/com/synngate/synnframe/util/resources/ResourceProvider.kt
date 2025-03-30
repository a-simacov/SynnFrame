package com.synngate.synnframe.util.resources

// com.synngate.synnframe.util.resources.ResourceProvider.kt
interface ResourceProvider {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg formatArgs: Any): String
}