## Сборка APK файла

Чтобы увеличить версию приложение, необходимо обновить номер версии в build.gradle.kts приложения

```
versionCode = 4
versionName = "1.0.3"
```

Подготовить ключи, которыми будет подписана сборка. Для релиза и для тестовой сборки ключи отличаются.

Для релизных сборок:

Исходное приложение на устройстве должно быть подписанным релизным ключом
APK обновления должен быть подписан тем же самым релизным ключом
Только тогда система Android позволит установить обновление

Для отладочных сборок:

При разработке Android Studio автоматически подписывает debug-сборки стандартным отладочным ключом
Если на устройстве установлена отладочная версия приложения, то и обновление должно быть подписано тем же отладочным ключом
Для этого достаточно выполнить "Build APK" или "Run" в Android Studio

Важные нюансы:

Невозможно обновить релизную версию отладочной и наоборот - подписи ключей не будут совпадать
Для тестирования полного процесса обновления лучше создать отдельный "тестовый релизный ключ", чтобы не использовать настоящий production-ключ
В Android Studio можно настроить разные buildTypes (например, "staging" или "test") с разными ключами для различных сценариев тестирования

## Генерация ключей

Команды для генерации ключей:

Релизный ключ

`keytool -genkey -v -keystore synnframe-release.keystore -alias synnframe_release -keyalg RSA -keysize 2048 -validity 10000`

Тестовый ключ

`keytool -genkey -v -keystore synnframe-test.keystore -alias synnframe_test -keyalg RSA -keysize 2048 -validity 10000`

Полученные ключи положить в защищенное хранилище. Это будут файлы `synnframe-test.keystore` и `synnframe-release.keystore`.

В корне проекта создать файл `keystore.properties` со следующими данными:

```
releaseKeystorePath=../keystores/synnframe-release.keystore
releaseKeystorePassword=ваш_пароль_хранилища
releaseKeyAlias=synnframe_release
releaseKeyPassword=ваш_пароль_ключа

testKeystorePath=../keystores/synnframe-test.keystore
testKeystorePassword=ваш_тестовый_пароль_хранилища
testKeyAlias=synnframe_test
testKeyPassword=ваш_тестовый_пароль_ключа
```

## Команды сборки

Создание подписанных APK

Для основной релизной версии
`./gradlew assembleRelease`

Для тестовой "релизной" версии
`./gradlew assembleStagingRelease`

Для отладочной версии
`./gradlew assembleDebug`

После сборки APK файлы будут доступны в следующих директориях:

Отладочная: app/build/outputs/apk/debug/app-debug.apk
Релизная: app/build/outputs/apk/release/app-release.apk
Тестовая релизная: app/build/outputs/apk/stagingRelease/app-stagingRelease.apk

Дополнительные полезные команды

Сборка всех вариантов
`gradlew.bat assemble`

Установка APK на подключенное устройство
`gradlew.bat installDebug`
`gradlew.bat installRelease`
`gradlew.bat installStagingRelease`

Очистка проекта (при возникновении проблем)
`gradlew.bat clean`

Комбинированные команды (сборка после очистки)
`gradlew.bat clean assembleRelease`

Просмотр всех доступных задач
`gradlew.bat tasks`

Проверка подписи APK
Чтобы убедиться, что APK правильно подписан, вы можете проверить информацию о подписи:
`jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk`

## Дополнительные артефакты и настройка обновления

При сборке релизной версии (и тестовой) выполняется также:
* создание копии полученного apk при сборке, но с именем synnframe-1.0.4.apk
* создание файла version.json, в котором находится информация о версии и дате.

эти файлы автоматически копируются в директорию, которая указана в файле update-config.properties. Их необходимо разместить в директорию, из которой веб-сервер сможет их загрузить по запросу на хост активного сервера, но путь зависит от настроек веб-сервера.

Пример настройки Apache 2.4

Файлы с обновлением размещаются в "C:/Apache24/htdocs/synnframe/update" (см. в настройках Апача), поэтому запрос для получения файла будет таким:
GET http://localhost:8000/synnframe/update/synnframe-1.0.2.apk

Раскомментировать следующие модули:

LoadModule rewrite_module modules/mod_rewrite.so
LoadModule headers_module modules/mod_headers.so

Добавить в httpd.conf в конец следующее:

```
# Конфигурация для обновления приложения SynnFrame
<Directory "C:/Apache24/htdocs/synnframe/update">
    Options Indexes FollowSymLinks
    AllowOverride None
    Require all granted
    
    # Установка правильной кодировки
    AddDefaultCharset UTF-8
    AddType "text/html; charset=UTF-8" .html
    
    # Установка MIME-типов
    AddType application/json .json
    AddType application/vnd.android.package-archive .apk
    
    # Настройка CORS
    Header set Access-Control-Allow-Origin "*"
    Header set Access-Control-Allow-Methods "GET, OPTIONS"
    Header set Access-Control-Allow-Headers "Authorization, Content-Type"
    
    # Настройка для .apk файлов: обеспечивает скачивание с фиксированным именем
    <Files "*.apk">
        Header set Content-Type "application/vnd.android.package-archive"
        Header set Content-Disposition "attachment; filename=synnframe.apk"
    </Files>
    
    # Правила перезаписи URL
    <IfModule mod_rewrite.c>
        RewriteEngine On
        
        # Базовый URL без параметров -> отдаем version.json
        RewriteCond %{QUERY_STRING} ^$
        RewriteRule ^$ version.json [L]
        
        # URL с параметром version -> отдаем соответствующий APK файл
        RewriteCond %{QUERY_STRING} ^version=([0-9.]+)$
        RewriteRule ^$ synnframe-%1.apk [L]
    </IfModule>
</Directory>
```

## Первая установка

В директорию с файлами релиза apk и версией можно добавить файл download.html, тогда по адресу

http://localhost:8000/synnframe/update/download.html

можно скачать тот файл apk, версия которого указана в version.json

Содержимое html

```
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Download SynnFrame</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .download-btn {
            display: inline-block;
            background-color: #4CAF50;
            color: white;
            padding: 12px 20px;
            text-align: center;
            text-decoration: none;
            font-size: 16px;
            border-radius: 4px;
            margin-top: 20px;
        }
        .version-info {
            background-color: #f5f5f5;
            padding: 15px;
            border-radius: 4px;
            margin-top: 20px;
        }
        .loading {
            color: #999;
            font-style: italic;
        }
    </style>
</head>
<body>
    <h1>Download SynnFrame</h1>
    
    <div class="version-info">
        <h2>Version <span id="version">Loading...</span></h2>
        <p>Release date: <span id="releaseDate">Loading...</span></p>
        <p>Description: Updated version of SynnFrame application with new features and improvements.</p>
    </div>
    
    <a href="#" id="downloadBtn" class="download-btn" download>Download APK</a>
    
    <p style="margin-top: 20px;">
        <small>If you encounter any download issues, please contact the administrator.</small>
    </p>

    <script>
        // Fetch version information from the JSON file
        fetch('/synnframe/update/version.json')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                // Update version information
                document.getElementById('version').textContent = data.lastVersion;
                document.getElementById('releaseDate').textContent = data.releaseDate;
                
                // Update download link with the correct version
                const downloadBtn = document.getElementById('downloadBtn');
                const apkUrl = `/synnframe/update/synnframe-${data.lastVersion}.apk`;
                
                // Set the initial href for browsers that don't support the custom download approach
                downloadBtn.href = apkUrl;
                
                // Custom download handler
                downloadBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    
                    // Show user feedback
                    downloadBtn.textContent = 'Preparing download...';
                    
                    // Fetch the APK file
                    fetch(apkUrl)
                        .then(response => response.blob())
                        .then(blob => {
                            // Create a temporary URL for the blob
                            const url = window.URL.createObjectURL(blob);
                            
                            // Create a temporary anchor to trigger the download
                            const tempLink = document.createElement('a');
                            tempLink.href = url;
                            tempLink.download = `synnframe-${data.lastVersion}.apk`;
                            document.body.appendChild(tempLink);
                            
                            // Trigger the download
                            tempLink.click();
                            
                            // Clean up
                            window.URL.revokeObjectURL(url);
                            document.body.removeChild(tempLink);
                            
                            // Reset button text
                            downloadBtn.textContent = 'Download APK';
                        })
                        .catch(error => {
                            console.error('Download error:', error);
                            downloadBtn.textContent = 'Download failed';
                            setTimeout(() => {
                                downloadBtn.textContent = 'Try Again';
                            }, 2000);
                        });
                });
            })
            .catch(error => {
                console.error('Error fetching version information:', error);
                document.getElementById('version').textContent = 'Error loading version';
                document.getElementById('releaseDate').textContent = 'Error loading date';
            });
    </script>
</body>
</html>
```

упрощенная версия

```
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Download SynnFrame</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .download-btn {
            display: inline-block;
            background-color: #4CAF50;
            color: white;
            padding: 12px 20px;
            text-align: center;
            text-decoration: none;
            font-size: 16px;
            border-radius: 4px;
            margin-top: 20px;
        }
        .version-info {
            background-color: #f5f5f5;
            padding: 15px;
            border-radius: 4px;
            margin-top: 20px;
        }
        .loading {
            color: #999;
            font-style: italic;
        }
    </style>
</head>
<body>
    <h1>Download SynnFrame</h1>
    
    <div class="version-info">
        <h2>Version <span id="version">Loading...</span></h2>
        <p>Release date: <span id="releaseDate">Loading...</span></p>
        <p>Description: Updated version of SynnFrame application with new features and improvements.</p>
    </div>
    
    <a href="#" id="downloadBtn" class="download-btn">Download APK</a>
    
    <p style="margin-top: 20px;">
        <small>If you encounter any download issues, please contact the administrator.</small>
    </p>

    <script>
        // Fetch version information from the JSON file
        fetch('/synnframe/update/update')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                // Update version information
                document.getElementById('version').textContent = data.lastVersion;
                document.getElementById('releaseDate').textContent = data.releaseDate;
                
                // Update download link with the correct version
                const downloadBtn = document.getElementById('downloadBtn');
                downloadBtn.href = `/synnframe/update/synnframe-${data.lastVersion}.apk`;
            })
            .catch(error => {
                console.error('Error fetching version information:', error);
                document.getElementById('version').textContent = 'Error loading version';
                document.getElementById('releaseDate').textContent = 'Error loading date';
            });
    </script>
</body>
</html>
```