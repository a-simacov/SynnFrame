package com.synngate.synnframe.domain.entity.operation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomListItem(
    val id: String,
    //@SerialName("name")
    val description: String = "",
    
    // Grid layout fields - максимум 4 части квадрата
    @SerialName("startPos")
    val start: String? = null,           // Левая часть (S)
    @SerialName("endPos")
    val end: String? = null,             // Правая часть (E)
    val top: String? = null,             // Верхняя часть (T)
    val bottom: String? = null,          // Нижняя часть (B)
    val topStart: String? = null,        // Верхний левый (TS)
    val topEnd: String? = null,          // Верхний правый (TE)
    val bottomStart: String? = null,     // Нижний левый (BS)
    val bottomEnd: String? = null        // Нижний правый (BE)
) {
    /**
     * Определяет тип макета на основе заполненных полей
     */
    fun getLayoutType(): CustomListLayoutType {
        val hasStart = !start.isNullOrEmpty()
        val hasEnd = !end.isNullOrEmpty()
        val hasTop = !top.isNullOrEmpty()
        val hasBottom = !bottom.isNullOrEmpty()
        val hasTopStart = !topStart.isNullOrEmpty()
        val hasTopEnd = !topEnd.isNullOrEmpty()
        val hasBottomStart = !bottomStart.isNullOrEmpty()
        val hasBottomEnd = !bottomEnd.isNullOrEmpty()
        
        return when {
            // Максимальная сетка 2x2: TS TE BS BE
            hasTopStart && hasTopEnd && hasBottomStart && hasBottomEnd -> 
                CustomListLayoutType.GRID_2x2
            
            // Левая колонка + правая разделена: S TE BE
            hasStart && hasTopEnd && hasBottomEnd && !hasTopStart && !hasBottomStart -> 
                CustomListLayoutType.START_WITH_SPLIT_END
            
            // Правая колонка + левая разделена: TS BS E
            hasTopStart && hasBottomStart && hasEnd && !hasTopEnd && !hasBottomEnd -> 
                CustomListLayoutType.SPLIT_START_WITH_END
            
            // Верхняя строка + нижняя разделена: TS TE B
            hasTopStart && hasTopEnd && hasBottom && !hasBottomStart && !hasBottomEnd -> 
                CustomListLayoutType.TOP_SPLIT_WITH_BOTTOM
            
            // Нижняя строка + верхняя разделена: T BS BE  
            hasTop && hasBottomStart && hasBottomEnd && !hasTopStart && !hasTopEnd -> 
                CustomListLayoutType.TOP_WITH_SPLIT_BOTTOM
            
            // Вертикальное разделение: S E
            hasStart && hasEnd && !hasTop && !hasBottom -> 
                CustomListLayoutType.START_END
            
            // Горизонтальное разделение: T B
            hasTop && hasBottom && !hasStart && !hasEnd -> 
                CustomListLayoutType.TOP_BOTTOM
            
            // Простое описание
            else -> CustomListLayoutType.SINGLE_DESCRIPTION
        }
    }
    
    /**
     * Парсит текст с выравниванием и весом (текст||выравнивание||вес)
     */
    fun parseTextWithAlignment(text: String?): Triple<String, CustomListAlignment, Float> {
        if (text.isNullOrEmpty()) return Triple("", CustomListAlignment.DEFAULT, 1f)
        
        val parts = text.split("||")
        val content = parts[0]
        
        val alignment = if (parts.size > 1) {
            CustomListAlignment.fromString(parts[1])
        } else {
            CustomListAlignment.DEFAULT
        }
        
        val weight = if (parts.size > 2) {
            parts[2].toFloatOrNull()?.takeIf { it > 0f } ?: 1f
        } else {
            1f
        }
        
        return Triple(content, alignment, weight)
    }
}

enum class CustomListLayoutType {
    SINGLE_DESCRIPTION,        // Только description
    START_END,                 // S E
    TOP_BOTTOM,               // T B
    START_WITH_SPLIT_END,     // S TE BE
    SPLIT_START_WITH_END,     // TS BS E
    TOP_SPLIT_WITH_BOTTOM,    // TS TE B
    TOP_WITH_SPLIT_BOTTOM,    // T BS BE
    GRID_2x2                  // TS TE BS BE
}

@Serializable
enum class CustomListAlignment(val horizontal: String, val vertical: String) {
    DEFAULT("Start", "Top"),
    
    // Горизонтальные выравнивания (центр по вертикали)
    START_CENTER("Start", "Center"),
    CENTER_CENTER("Center", "Center"), 
    END_CENTER("End", "Center"),
    
    // Угловые выравнивания
    START_TOP("Start", "Top"),         // ST
    CENTER_TOP("Center", "Top"),       // CT
    END_TOP("End", "Top"),             // ET
    START_BOTTOM("Start", "Bottom"),   // SB
    CENTER_BOTTOM("Center", "Bottom"), // CB
    END_BOTTOM("End", "Bottom");       // EB
    
    companion object {
        fun fromString(value: String): CustomListAlignment {
            return when (value.uppercase()) {
                "SC" -> START_CENTER
                "CC" -> CENTER_CENTER
                "EC" -> END_CENTER
                "ST" -> START_TOP
                "CT" -> CENTER_TOP
                "ET" -> END_TOP
                "SB" -> START_BOTTOM
                "CB" -> CENTER_BOTTOM
                "EB" -> END_BOTTOM
                else -> DEFAULT
            }
        }
    }
}