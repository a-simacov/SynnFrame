package com.synngate.synnframe.domain.entity.taskx

enum class ProductStatus {
    STANDARD,
    DEFECTIVE,
    EXPIRED;

    companion object {

        fun fromString(value: String): ProductStatus {
            return when (value.uppercase()) {
                "STANDARD", "КОНДИЦИЯ", "СТАНДАРТ" -> STANDARD
                "DEFECTIVE", "БРАК" -> DEFECTIVE
                "EXPIRED", "ПРОСРОЧЕН" -> EXPIRED
                else -> STANDARD
            }
        }
    }

    fun format(): String = when (this) {
        STANDARD -> "Кондиция (стандарт)"
        DEFECTIVE -> "Брак"
        EXPIRED -> "Просрочен"
    }
}