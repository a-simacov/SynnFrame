package com.synngate.synnframe.util.resources

interface ResourceProvider {

    fun getString(resId: Int): String
    fun getString(resId: Int, vararg formatArgs: Any): String
}