package com.synngate.synnframe.presentation.ui.taskx.examples

import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandButtonStyle
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandDisplayCondition
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandExecutionBehavior
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameter
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameterType
import com.synngate.synnframe.presentation.ui.taskx.entity.ParameterOption
import com.synngate.synnframe.presentation.ui.taskx.entity.ParameterValidation
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField

/**
 * Примеры настройки команд для различных шагов визарда
 */
object CommandExamples {

    /**
     * Команды для шага выбора товара
     */
    val productStepCommands = listOf(
        // Команда для проверки остатков товара
        StepCommand(
            id = "check_product_stock",
            name = "Проверить остатки",
            description = "Проверить остатки товара на складе",
            endpoint = "/api/commands/check-stock",
            icon = "info",
            buttonStyle = CommandButtonStyle.OUTLINE,
            displayCondition = CommandDisplayCondition.WHEN_OBJECT_SELECTED,
            executionBehavior = CommandExecutionBehavior.SHOW_RESULT,
            parameters = listOf(
                CommandParameter(
                    id = "warehouse_id",
                    name = "warehouseId",
                    displayName = "Склад",
                    type = CommandParameterType.SELECT,
                    isRequired = true,
                    options = listOf(
                        ParameterOption("WH001", "Основной склад"),
                        ParameterOption("WH002", "Резервный склад"),
                        ParameterOption("WH003", "Возвратный склад")
                    ),
                    order = 1
                )
            ),
            order = 1
        ),

        // Команда для создания нового товара
        StepCommand(
            id = "create_new_product",
            name = "Создать новый товар",
            description = "Создать новый товар в системе",
            endpoint = "/api/commands/create-product",
            icon = "build",
            buttonStyle = CommandButtonStyle.SUCCESS,
            displayCondition = CommandDisplayCondition.WHEN_OBJECT_NOT_SELECTED,
            executionBehavior = CommandExecutionBehavior.SHOW_RESULT,
            parameters = listOf(
                CommandParameter(
                    id = "product_name",
                    name = "productName",
                    displayName = "Название товара",
                    type = CommandParameterType.TEXT,
                    isRequired = true,
                    validation = ParameterValidation(
                        minLength = 3,
                        maxLength = 100,
                        errorMessage = "Название должно содержать от 3 до 100 символов"
                    ),
                    order = 1
                ),
                CommandParameter(
                    id = "product_id",
                    name = "productId",
                    displayName = "ID",
                    type = CommandParameterType.TEXT,
                    isRequired = true,
                    validation = ParameterValidation(
                        pattern = "[A-Z0-9]{5,20}",
                        errorMessage = "ID должен содержать только заглавные буквы и цифры (5-20 символов)"
                    ),
                    order = 2
                ),
                CommandParameter(
                    id = "category",
                    name = "category",
                    displayName = "Category",
                    type = CommandParameterType.SELECT,
                    isRequired = true,
                    options = listOf(
                        ParameterOption("ELECTRONICS", "Электроника"),
                        ParameterOption("CLOTHING", "Одежда"),
                        ParameterOption("FOOD", "Продукты питания"),
                        ParameterOption("BOOKS", "Книги")
                    ),
                    order = 3
                )
            ),
            confirmationRequired = true,
            confirmationMessage = "Создать новый товар в системе?",
            order = 2
        )
    )

    /**
     * Команды для шага выбора ячейки
     */
    val binStepCommands = listOf(
        // Команда для резервирования ячейки
        StepCommand(
            id = "reserve_bin",
            name = "Зарезервировать ячейку",
            description = "Зарезервировать выбранную ячейку для текущей операции",
            endpoint = "/api/commands/reserve-bin",
            icon = "check",
            buttonStyle = CommandButtonStyle.WARNING,
            displayCondition = CommandDisplayCondition.WHEN_OBJECT_SELECTED,
            executionBehavior = CommandExecutionBehavior.SHOW_RESULT,
            parameters = listOf(
                CommandParameter(
                    id = "reservation_duration",
                    name = "reservationDuration",
                    displayName = "Время резервирования (минуты)",
                    type = CommandParameterType.INTEGER,
                    isRequired = true,
                    defaultValue = "30",
                    validation = ParameterValidation(
                        minValue = 5.0,
                        maxValue = 240.0,
                        errorMessage = "Время резервирования должно быть от 5 до 240 минут"
                    ),
                    order = 1
                ),
                CommandParameter(
                    id = "reason",
                    name = "reason",
                    displayName = "Причина резервирования",
                    type = CommandParameterType.TEXTAREA,
                    isRequired = false,
                    placeholder = "Укажите причину резервирования ячейки",
                    order = 2
                )
            ),
            confirmationRequired = true,
            order = 1
        ),

        // Команда для поиска альтернативных ячеек
        StepCommand(
            id = "find_alternative_bins",
            name = "Найти альтернативы",
            description = "Найти альтернативные ячейки в той же зоне",
            endpoint = "/api/commands/find-alternatives",
            icon = "info",
            buttonStyle = CommandButtonStyle.OUTLINE,
            displayCondition = CommandDisplayCondition.ALWAYS,
            executionBehavior = CommandExecutionBehavior.SHOW_RESULT,
            parameters = listOf(
                CommandParameter(
                    id = "max_alternatives",
                    name = "maxAlternatives",
                    displayName = "Максимальное количество альтернатив",
                    type = CommandParameterType.INTEGER,
                    isRequired = true,
                    defaultValue = "5",
                    validation = ParameterValidation(
                        minValue = 1.0,
                        maxValue = 20.0
                    ),
                    order = 1
                ),
                CommandParameter(
                    id = "same_zone_only",
                    name = "sameZoneOnly",
                    displayName = "Только в той же зоне",
                    type = CommandParameterType.BOOLEAN,
                    isRequired = false,
                    defaultValue = "true",
                    order = 2
                )
            ),
            order = 2
        )
    )

    /**
     * Команды для шага ввода количества
     */
    val quantityStepCommands = listOf(
        // Команда для автоматического расчета количества
        StepCommand(
            id = "calculate_auto_quantity",
            name = "Автоматический расчет",
            description = "Рассчитать количество автоматически на основе данных системы",
            endpoint = "/api/commands/calculate-quantity",
            icon = "settings",
            buttonStyle = CommandButtonStyle.SECONDARY,
            displayCondition = CommandDisplayCondition.ALWAYS,
            executionBehavior = CommandExecutionBehavior.REFRESH_STEP,
            parameters = listOf(
                CommandParameter(
                    id = "calculation_method",
                    name = "calculationMethod",
                    displayName = "Метод расчета",
                    type = CommandParameterType.SELECT,
                    isRequired = true,
                    options = listOf(
                        ParameterOption("FIFO", "По принципу FIFO"),
                        ParameterOption("LIFO", "По принципу LIFO"),
                        ParameterOption("EXPIRY_DATE", "По сроку годности"),
                        ParameterOption("RANDOM", "Случайный выбор")
                    ),
                    order = 1
                ),
                CommandParameter(
                    id = "safety_margin",
                    name = "safetyMargin",
                    displayName = "Запас безопасности (%)",
                    type = CommandParameterType.DECIMAL,
                    isRequired = false,
                    defaultValue = "5.0",
                    validation = ParameterValidation(
                        minValue = 0.0,
                        maxValue = 50.0
                    ),
                    order = 2
                )
            ),
            order = 1
        ),

        // Команда для округления количества
        StepCommand(
            id = "round_quantity",
            name = "Округлить количество",
            description = "Округлить введенное количество до стандартных значений",
            endpoint = "/api/commands/round-quantity",
            icon = "edit",
            buttonStyle = CommandButtonStyle.OUTLINE,
            displayCondition = CommandDisplayCondition.WHEN_OBJECT_SELECTED,
            executionBehavior = CommandExecutionBehavior.REFRESH_STEP,
            parameters = listOf(
                CommandParameter(
                    id = "rounding_method",
                    name = "roundingMethod",
                    displayName = "Метод округления",
                    type = CommandParameterType.SELECT,
                    isRequired = true,
                    options = listOf(
                        ParameterOption("UP", "Вверх"),
                        ParameterOption("DOWN", "Вниз"),
                        ParameterOption("NEAREST", "К ближайшему"),
                        ParameterOption("TO_PACKAGE", "До упаковки")
                    ),
                    order = 1
                )
            ),
            order = 2
        )
    )

    /**
     * Универсальные команды для любого шага
     */
    val universalCommands = listOf(
        // Команда для отправки комментария
        StepCommand(
            id = "add_comment",
            name = "Добавить комментарий",
            description = "Добавить комментарий к текущему шагу",
            endpoint = "/api/commands/add-comment",
            icon = "edit",
            buttonStyle = CommandButtonStyle.OUTLINE,
            displayCondition = CommandDisplayCondition.ALWAYS,
            executionBehavior = CommandExecutionBehavior.SHOW_RESULT,
            parameters = listOf(
                CommandParameter(
                    id = "comment_text",
                    name = "commentText",
                    displayName = "Текст комментария",
                    type = CommandParameterType.TEXTAREA,
                    isRequired = true,
                    validation = ParameterValidation(
                        minLength = 10,
                        maxLength = 500,
                        errorMessage = "Комментарий должен содержать от 10 до 500 символов"
                    ),
                    order = 1
                ),
                CommandParameter(
                    id = "priority",
                    name = "priority",
                    displayName = "Приоритет",
                    type = CommandParameterType.SELECT,
                    isRequired = true,
                    defaultValue = "NORMAL",
                    options = listOf(
                        ParameterOption("LOW", "Низкий"),
                        ParameterOption("NORMAL", "Обычный"),
                        ParameterOption("HIGH", "Высокий"),
                        ParameterOption("URGENT", "Срочный")
                    ),
                    order = 2
                )
            ),
            order = 10
        ),

        // Команда для вызова супервизора
        StepCommand(
            id = "call_supervisor",
            name = "Вызвать супервизора",
            description = "Отправить уведомление супервизору о необходимости помощи",
            endpoint = "/api/commands/call-supervisor",
            icon = "warning",
            buttonStyle = CommandButtonStyle.DANGER,
            displayCondition = CommandDisplayCondition.ALWAYS,
            executionBehavior = CommandExecutionBehavior.SHOW_RESULT,
            parameters = listOf(
                CommandParameter(
                    id = "problem_type",
                    name = "problemType",
                    displayName = "Тип проблемы",
                    type = CommandParameterType.SELECT,
                    isRequired = true,
                    options = listOf(
                        ParameterOption("SYSTEM_ERROR", "Ошибка системы"),
                        ParameterOption("PRODUCT_ISSUE", "Проблема с товаром"),
                        ParameterOption("EQUIPMENT_FAILURE", "Поломка оборудования"),
                        ParameterOption("SAFETY_CONCERN", "Вопрос безопасности"),
                        ParameterOption("OTHER", "Другое")
                    ),
                    order = 1
                ),
                CommandParameter(
                    id = "urgency",
                    name = "urgency",
                    displayName = "Срочность",
                    type = CommandParameterType.SELECT,
                    isRequired = true,
                    defaultValue = "MEDIUM",
                    options = listOf(
                        ParameterOption("LOW", "Низкая"),
                        ParameterOption("MEDIUM", "Средняя"),
                        ParameterOption("HIGH", "Высокая"),
                        ParameterOption("CRITICAL", "Критическая")
                    ),
                    order = 2
                ),
                CommandParameter(
                    id = "description",
                    name = "description",
                    displayName = "Описание проблемы",
                    type = CommandParameterType.TEXTAREA,
                    isRequired = true,
                    placeholder = "Опишите проблему подробно",
                    validation = ParameterValidation(
                        minLength = 20,
                        maxLength = 1000
                    ),
                    order = 3
                )
            ),
            confirmationRequired = true,
            confirmationMessage = "Отправить вызов супервизору?",
            order = 20
        )
    )
}

/**
 * Пример конфигурации ActionStepTemplate с командами
 */
val exampleActionStepWithCommands = ActionStepTemplate(
    id = "step_select_product",
    order = 1,
    name = "Выбор товара",
    promptText = "Выберите товар для операции",
    factActionField = FactActionField.STORAGE_PRODUCT_CLASSIFIER,
    isRequired = true,
    commands = CommandExamples.productStepCommands + CommandExamples.universalCommands
)

/**
 * Пример JSON конфигурации команды для сервера
 */
val exampleCommandJson = """
{
  "id": "check_product_stock",
  "name": "Проверить остатки",
  "description": "Проверить остатки товара на складе",
  "endpoint": "/api/commands/check-stock",
  "icon": "info",
  "buttonStyle": "OUTLINE",
  "displayCondition": "WHEN_OBJECT_SELECTED",
  "executionBehavior": "SHOW_RESULT",
  "parameters": [
    {
      "id": "warehouse_id",
      "name": "warehouseId",
      "displayName": "Склад",
      "type": "SELECT",
      "isRequired": true,
      "options": [
        {"value": "WH001", "displayName": "Основной склад"},
        {"value": "WH002", "displayName": "Резервный склад"}
      ],
      "order": 1
    }
  ],
  "order": 1
}
"""

/**
 * Пример ответа сервера на выполнение команды
 */
val exampleCommandResponse = """
{
  "success": true,
  "message": "Остатки товара: 150 шт.",
  "resultData": {
    "totalStock": "150",
    "availableStock": "120",
    "reservedStock": "30",
    "lastUpdated": "2024-01-15T10:30:00"
  },
  "nextAction": "SHOW_DIALOG"
}
"""