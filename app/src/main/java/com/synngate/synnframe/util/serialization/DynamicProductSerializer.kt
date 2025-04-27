package com.synngate.synnframe.util.serialization

import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object DynamicProductSerializer : KSerializer<DynamicProduct> {
    // Используем сериализатор Base класса
    private val baseSerializer = DynamicProduct.Base.serializer()

    override val descriptor: SerialDescriptor = baseSerializer.descriptor

    override fun serialize(encoder: Encoder, value: DynamicProduct) {
        // Если это пустой объект, сериализуем пустую реализацию Base
        if (value is DynamicProduct.Empty) {
            val emptyBase = DynamicProduct.Base(
                id = "",
                name = "",
                accountingModel = "QTY",
                articleNumber = "",
                mainUnitId = "",
                units = emptyList()
            )
            baseSerializer.serialize(encoder, emptyBase)
        } else if (value is DynamicProduct.Base) {
            // Иначе сериализуем Base как есть
            baseSerializer.serialize(encoder, value)
        } else {
            // Для всех других реализаций создаем Base с их данными
            val base = DynamicProduct.Base(
                id = value.getId(),
                name = value.getName(),
                accountingModel = value.getAccountingModelString(),
                articleNumber = value.getArticleNumber(),
                mainUnitId = value.getMainUnitId(),
                units = value.getUnits()
            )
            baseSerializer.serialize(encoder, base)
        }
    }

    override fun deserialize(decoder: Decoder): DynamicProduct {
        // Всегда десериализуем в класс Base
        return baseSerializer.deserialize(decoder)
    }
}

/**
 * Модуль сериализации с поддержкой полиморфизма для DynamicProduct
 */
val dynamicProductModule = SerializersModule {
    polymorphic(DynamicProduct::class) {
        subclass(DynamicProduct.Base::class)
        // Пустая реализация не сериализуется, поэтому не регистрируем
    }
}

/**
 * JSON форматтер с поддержкой сериализации DynamicProduct
 */
val dynamicProductJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    serializersModule = dynamicProductModule
    encodeDefaults = true
    coerceInputValues = true
}