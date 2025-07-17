# Интеграция updateActionFieldEndpoint

## Описание функционала

Поле `updateActionFieldEndpoint` в шаге действия позволяет получать обновления для выбранных объектов с сервера. При выборе объекта в шаге происходит следующее:

1. Объект устанавливается в FactAction
2. Выполняется валидация объекта (включая API_REQUEST валидацию если настроена)
3. **Только после успешной валидации** отправляется запрос на updateActionFieldEndpoint
4. Полученные обновления применяются к объекту
5. Происходит переход на следующий шаг (auto-advance)

Это гарантирует, что запрос на обновление полей отправляется только для валидных объектов, которые уже установлены в FactAction.

## Конфигурация

### Пример конфигурации шага
```json
{
  "id": "select-product-step",
  "name": "Выберите товар",
  "factActionField": "STORAGE_PRODUCT",
  "updateActionFieldEndpoint": "/myTask/{taskId}/updateProduct"
}
```

### Поддерживаемые параметры в endpoint
- `{taskId}` - ID текущего задания
- `{tskId}` - альтернативный вариант для ID задания

## Валидация и updateActionFieldEndpoint

### Порядок выполнения
1. **Установка объекта**: Выбранный объект сразу устанавливается в FactAction
2. **Валидация**: Выполняется полная валидация объекта, включая:
   - Проверка соответствия плану
   - Валидация по правилам (ValidationRule)
   - API валидация (API_REQUEST) если настроена
3. **Запрос обновлений**: Только если валидация прошла успешно, отправляется запрос на updateActionFieldEndpoint
4. **Применение обновлений**: Полученные данные объединяются с объектом
5. **Auto-advance**: Происходит переход на следующий шаг

### Важные особенности
- **FactAction уже содержит объект** при выполнении запроса на updateActionFieldEndpoint
- **API валидация завершена** до отправки запроса на обновление
- **Контекст полный** - сервер получает актуальное состояние FactAction
- **Ошибки валидации** блокируют запрос на обновление полей

## Запрос на сервер

### HTTP параметры
- **Метод:** POST
- **Content-Type:** application/json
- **Заголовки:**
  - `Authorization: Basic <credentials>`
  - `User-Auth-Id: <user-id>`

### Тело запроса (FactActionRequestDto)
```json
{
  "id": "fact-action-id-123",
  "taskId": "task-456",
  "storageProduct": {
    "id": "task-product-id-789",
    "product": {
      "id": "product-id-001",
      "name": "Example Product",
      "articleNumber": "ART-001",
      "weight": 0.5,
      "accountingModel": "QTY",
      "mainUnitId": "kg",
      "units": []
    },
    "expirationDate": "2024-12-31T23:59:59",
    "status": "STANDARD"
  },
  "storageProductClassifier": null,
  "storagePallet": null,
  "storageBin": null,
  "wmsAction": "STORAGE_INBOUND",
  "quantity": 10.0,
  "placementPallet": null,
  "placementBin": null,
  "startedAt": "2024-01-15T10:30:00",
  "completedAt": "2024-01-15T10:35:00",
  "plannedActionId": "planned-action-123",
  "actionTemplateId": "template-456"
}
```

## Ответ от сервера

### Структура ответа
```json
{
  "success": true,
  "objectType": "TASK_PRODUCT",
  "errorMessage": null,
  "taskProductId": "fcf6aad2-37be-4fbb-9ab1-bd1f895cfe9a",
  "productId": "ITM013884",
  "productName": "Example Product",
  "productArticleNumber": "ART-001",
  "productWeight": 54,
  "productAccountingModel": "WEIGHT",
  "productMainUnitId": "kg",
  "expirationDate": "2024-12-31T23:59:59",
  "productStatus": "STANDARD"
}
```

### Поля ответа по типам объектов

#### Для TASK_PRODUCT
- `taskProductId` - ID товара задания (обязательно)
- `productId` - ID товара из классификатора (обязательно для обновления полей продукта)
- `productName` - название товара
- `productArticleNumber` - артикул товара
- `productWeight` - вес товара
- `productAccountingModel` - модель учета (QTY/WEIGHT)
- `productMainUnitId` - основная единица измерения
- `expirationDate` - дата истечения срока годности
- `productStatus` - статус товара (STANDARD/DAMAGED/EXPIRED)

#### Для STORAGE_PRODUCT_CLASSIFIER
- `productId` - ID товара
- `productName` - название товара
- `productArticleNumber` - артикул товара
- `productWeight` - вес товара
- `productAccountingModel` - модель учета
- `productMainUnitId` - основная единица измерения

#### Для BIN (STORAGE_BIN/ALLOCATION_BIN)
- `binCode` - код ячейки
- `binZone` - зона ячейки

#### Для PALLET (STORAGE_PALLET/ALLOCATION_PALLET)
- `palletCode` - код паллеты
- `palletIsClosed` - закрыта ли паллета

#### Для QUANTITY
- `quantity` - количество

## Логика объединения полей

### Принципы объединения:
1. **Непустые значения имеют приоритет** - если сервер вернул непустое значение, оно заменяет исходное
2. **Пустые значения игнорируются** - если сервер вернул пустое значение, остается исходное
3. **Специальные значения по умолчанию** - для веса (0.0f) и статуса (STANDARD) проверяется отличие от значения по умолчанию

### Примеры объединения:

#### Исходный объект:
```json
{
  "product": {
    "id": "product-001",
    "name": "Old Product Name",
    "weight": 0.5,
    "articleNumber": "OLD-001"
  }
}
```

#### Ответ сервера:
```json
{
  "productWeight": 2.5,
  "productName": ""
}
```

#### Результат объединения:
```json
{
  "product": {
    "id": "product-001",
    "name": "Old Product Name",
    "weight": 2.5,
    "articleNumber": "OLD-001"
  }
}
```

## Обработка ошибок

### Ошибки сервера
Если сервер вернул `"success": false`, приложение:
1. Логирует ошибку
2. Показывает снекбар с сообщением об ошибке
3. Устанавливает исходный объект без обновлений

### Ошибки сети
При сетевых ошибках приложение:
1. Логирует ошибку
2. Показывает снекбар с сообщением об ошибке
3. Устанавливает исходный объект без обновлений

## Минимальные требования к ответу

### Для обновления одного поля (например, weight):
```json
{
  "success": true,
  "objectType": "TASK_PRODUCT",
  "taskProductId": "fcf6aad2-37be-4fbb-9ab1-bd1f895cfe9a",
  "productId": "ITM013884",
  "productWeight": 54
}
```

**Важно:** Для TASK_PRODUCT требуются:
- `taskProductId` - ID товара задания для идентификации объекта
- `productId` - ID продукта из классификатора для получения базового продукта

### Для обновления нескольких полей:
```json
{
  "success": true,
  "objectType": "TASK_PRODUCT",
  "productWeight": 2.5,
  "productName": "Updated Product Name",
  "expirationDate": "2024-12-31T23:59:59"
}
```

### Ошибка:
```json
{
  "success": false,
  "errorMessage": "Product not found"
}
```

## Примеры использования

### 1. Обновление веса товара
**Endpoint:** `/api/product/{taskId}/updateWeight`

**Ответ:**
```json
{
  "success": true,
  "objectType": "TASK_PRODUCT",
  "productWeight": 2.5
}
```

### 2. Обновление статуса и даты истечения
**Endpoint:** `/api/product/{taskId}/updateStatus`

**Ответ:**
```json
{
  "success": true,
  "objectType": "TASK_PRODUCT",
  "productStatus": "EXPIRED",
  "expirationDate": "2024-01-01T00:00:00"
}
```

### 3. Обновление информации о ячейке
**Endpoint:** `/api/bin/{taskId}/updateLocation`

**Ответ:**
```json
{
  "success": true,
  "objectType": "BIN",
  "binCode": "A-01-02-03",
  "binZone": "Zone A"
}
```