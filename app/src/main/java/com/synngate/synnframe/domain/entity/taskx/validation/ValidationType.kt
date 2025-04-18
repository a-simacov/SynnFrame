package com.synngate.synnframe.domain.entity.taskx.validation

enum class ValidationType {
    FROM_PLAN,    // Из плана
    NOT_EMPTY,    // Не пустое
    MATCHES_REGEX // Соответствует регулярному выражению
}