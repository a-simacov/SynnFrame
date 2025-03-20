package com.synngate.synnframe.presentation.ui.products.model

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    ARTICLE_ASC,
    ARTICLE_DESC;

    fun getDisplayName(): String {
        return when(this) {
            NAME_ASC -> "По наименованию (А-Я)"
            NAME_DESC -> "По наименованию (Я-А)"
            ARTICLE_ASC -> "По артикулу (возр.)"
            ARTICLE_DESC -> "По артикулу (убыв.)"
        }
    }
}