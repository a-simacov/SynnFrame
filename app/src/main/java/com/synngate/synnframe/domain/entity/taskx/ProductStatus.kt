package com.synngate.synnframe.domain.entity.taskx

enum class ProductStatus {
    STANDARD,
    DEFECTIVE,
    EXPIRED;

    companion object {

        fun fromString(value: String): ProductStatus {
            return when (value.uppercase()) {
                "STANDARD" -> STANDARD
                "DEFECTIVE" -> DEFECTIVE
                "EXPIRED" -> EXPIRED
                else -> STANDARD
            }
        }
    }

    fun format(): String = when (this) {
        STANDARD -> "Standard"
        DEFECTIVE -> "Defective"
        EXPIRED -> "Expired"
    }
}