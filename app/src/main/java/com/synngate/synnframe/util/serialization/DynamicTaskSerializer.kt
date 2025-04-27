package com.synngate.synnframe.util.serialization

import com.synngate.synnframe.domain.entity.operation.DynamicTask
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object DynamicTaskSerializer : KSerializer<DynamicTask> {
    // Используем сериализатор Base класса
    private val baseSerializer = DynamicTask.Base.serializer()

    override val descriptor: SerialDescriptor = baseSerializer.descriptor

    override fun serialize(encoder: Encoder, value: DynamicTask) {
        // Если это пустой объект, сериализуем пустую реализацию Base
        if (value is DynamicTask.Empty) {
            val emptyBase = DynamicTask.Base(
                id = "",
                name = ""
            )
            baseSerializer.serialize(encoder, emptyBase)
        } else if (value is DynamicTask.Base) {
            // Иначе сериализуем Base как есть
            baseSerializer.serialize(encoder, value)
        } else {
            // Для всех других реализаций создаем Base с их данными
            val base = DynamicTask.Base(
                id = value.getId(),
                name = value.getName()
            )
            baseSerializer.serialize(encoder, base)
        }
    }

    override fun deserialize(decoder: Decoder): DynamicTask {
        // Всегда десериализуем в класс Base
        return baseSerializer.deserialize(decoder)
    }
}

/**
 * Модуль сериализации с поддержкой полиморфизма для DynamicTask
 */
val dynamicTaskModule = SerializersModule {
    polymorphic(DynamicTask::class) {
        subclass(DynamicTask.Base::class)
        // Пустая реализация не сериализуется, поэтому не регистрируем
    }
}

/**
 * JSON форматтер с поддержкой сериализации DynamicTask
 */
val dynamicTaskJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    serializersModule = dynamicTaskModule
    encodeDefaults = true
    coerceInputValues = true
}