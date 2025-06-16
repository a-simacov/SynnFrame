package com.synngate.synnframe.presentation.ui.products.model

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    ARTICLE_ASC,
    ARTICLE_DESC;

    fun getDisplayName(): String {
        return when(this) {
            NAME_ASC -> "By name (A-Z)"
            NAME_DESC -> "By name (Z-A)"
            ARTICLE_ASC -> "By article (asc.)"
            ARTICLE_DESC -> "By article (desc.)"
        }
    }
}