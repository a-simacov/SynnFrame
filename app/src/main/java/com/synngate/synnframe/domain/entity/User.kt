package com.synngate.synnframe.domain.entity

data class User(
    val id: String,
    val name: String,
    val password: String,
    val userGroupId: String
)