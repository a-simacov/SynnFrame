package com.synngate.synnframe.presentation.ui.taskx.entity

import kotlinx.serialization.Serializable

/**
 * Настраиваемая команда для шага визарда
 */
@Serializable
data class StepCommand(
    val id: String,
    val name: String,
    val description: String = "",
    val endpoint: String,
    val icon: String? = null,
    val buttonStyle: CommandButtonStyle = CommandButtonStyle.SECONDARY,
    val displayCondition: CommandDisplayCondition = CommandDisplayCondition.ALWAYS,
    val executionBehavior: CommandExecutionBehavior = CommandExecutionBehavior.SHOW_RESULT,
    val parameters: List<CommandParameter> = emptyList(),
    val confirmationRequired: Boolean = false,
    val confirmationMessage: String? = null,
    val order: Int = 0
)

/**
 * Параметр команды, который пользователь может ввести
 */
@Serializable
data class CommandParameter(
    val id: String,
    val name: String,
    val displayName: String,
    val type: CommandParameterType,
    val isRequired: Boolean = true,
    val defaultValue: String? = null,
    val placeholder: String? = null,
    val validation: ParameterValidation? = null,
    val options: List<ParameterOption>? = null, // Для SELECT типа
    val order: Int = 0,
    val booleanOptions: BooleanParameterOptions? = null
)

/**
 * Опция для параметра типа SELECT
 */
@Serializable
data class ParameterOption(
    val value: String,
    val displayName: String
)

/**
 * Валидация параметра
 */
@Serializable
data class ParameterValidation(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val pattern: String? = null, // Regex
    val errorMessage: String? = null
)

/**
 * Тип параметра команды
 */
@Serializable
enum class CommandParameterType {
    TEXT,           // Строковое значение
    NUMBER,         // Числовое значение (целое или дробное)
    INTEGER,        // Только целые числа
    DECIMAL,        // Только дробные числа
    BOOLEAN,        // Логическое значение (checkbox)
    SELECT,         // Выбор из списка
    DATE,           // Дата
    DATETIME,       // Дата и время
    PASSWORD,       // Пароль (скрытый ввод)
    TEXTAREA,       // Многострочный текст
    EMAIL,          // Email адрес
    PHONE          // Номер телефона
}

/**
 * Стиль кнопки команды
 */
@Serializable
enum class CommandButtonStyle {
    PRIMARY,        // Основная кнопка
    SECONDARY,      // Вторичная кнопка
    SUCCESS,        // Зеленая кнопка (успех)
    WARNING,        // Желтая кнопка (предупреждение)
    DANGER,         // Красная кнопка (опасность)
    OUTLINE         // Контурная кнопка
}

/**
 * Условие отображения команды
 */
@Serializable
enum class CommandDisplayCondition {
    ALWAYS,                 // Всегда показывать
    WHEN_OBJECT_SELECTED,   // Только когда объект выбран
    WHEN_OBJECT_NOT_SELECTED, // Только когда объект не выбран
    WHEN_STEP_COMPLETED,    // Только когда шаг завершен
    WHEN_STEP_NOT_COMPLETED // Только когда шаг не завершен
}

/**
 * Поведение после выполнения команды
 */
@Serializable
enum class CommandExecutionBehavior {
    SHOW_RESULT,            // Показать результат в snackbar
    REFRESH_STEP,           // Обновить текущий шаг
    GO_TO_NEXT_STEP,        // Перейти к следующему шагу
    GO_TO_PREVIOUS_STEP,    // Перейти к предыдущему шагу
    COMPLETE_ACTION,        // Завершить действие
    SILENT                  // Не показывать результат
}

@Serializable
data class BooleanParameterOptions(
    val displayType: BooleanDisplayType = BooleanDisplayType.CHECKBOX,
    val labelPair: BooleanLabelPair = BooleanLabelPair.DEFAULT
)