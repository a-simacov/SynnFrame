package com.synngate.synnframe.domain.entity.operation

import kotlinx.serialization.Serializable

interface DynamicTask {

    fun getId(): String

    fun getName(): String

    object Empty : DynamicTask {
        override fun getId(): String = ""
        override fun getName(): String = ""
    }

    @Serializable
    data class Base(
        private val id: String,
        private val name: String
    ) : DynamicTask {
        override fun getId(): String = id
        override fun getName(): String = name
    }
}