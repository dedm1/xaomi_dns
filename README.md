# Private DNS Switcher Widget

Легковесное Android-приложение для быстрого переключения персонального DNS (Private DNS) через виджет 1х1 на рабочем столе.

## 🛠 Сборка проекта (Build)

```bash
./gradlew assembleDebug
```

Собранный APK-файл будет находиться по пути:
`app/build/outputs/apk/debug/app-debug.apk`

## 📦 Установка (Install)

Убедитесь, что ваше устройство подключено к компьютеру и включена **Отладка по USB**. Установите собранный APK через ADB:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### ⚠️ Важно: Выдача разрешений
Для того чтобы приложение могло управлять настройками Private DNS, ему требуется специальное системное разрешение `WRITE_SECURE_SETTINGS`. **Без него приложение не сможет переключать DNS и будет выдавать ошибку.**

Сразу после установки выполните команду для выдачи разрешения:
```bash
adb shell pm grant com.dedm.dns android.permission.WRITE_SECURE_SETTINGS
```

## 🗑 Удаление (Uninstall)

Чтобы удалить приложение с вашего устройства через ADB, выполните:

```bash
adb uninstall com.dedm.dns
```
