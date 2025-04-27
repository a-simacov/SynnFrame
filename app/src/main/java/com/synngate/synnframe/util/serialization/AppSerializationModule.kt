package com.synngate.synnframe.util.serialization

import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val appSerializationModule = SerializersModule {
    // Регистрируем полиморфные сериализаторы
    polymorphic(DynamicProduct::class) {
        subclass(DynamicProduct.Base::class)
    }

    polymorphic(DynamicTask::class) {
        subclass(DynamicTask.Base::class)
    }

    // Регистрируем контекстуальные сериализаторы
    contextual(DynamicProduct::class, DynamicProductSerializer)
    contextual(DynamicTask::class, DynamicTaskSerializer)

    // Регистрируем другие общие сериализаторы если нужны
    // ...
}

/**
 * Стандартный JSON-форматтер приложения с поддержкой всех сериализаторов
 */
val appJson = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    useArrayPolymorphism = false  // В новых версиях kotlinx.serialization рекомендуется false
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
    serializersModule = appSerializationModule
}