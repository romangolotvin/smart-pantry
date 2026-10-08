# Умный холодильник

Android-приложение на Kotlin + Jetpack Compose:

- главное меню с подборкой рецептов;
- сканер штрихкодов камерой (ML Kit + CameraX);
- сохранение продуктов и сроков годности;
- подбор рецептов из того, что есть в холодильнике.

Продукты по штрихкоду ищутся через [Open Food Facts](https://world.openfoodfacts.org/).

## Как открыть

1. Установите [Android Studio](https://developer.android.com/studio).
2. File → Open → папка `smart-pantry`.
3. Дождитесь синхронизации Gradle.
4. Запустите на телефоне или эмуляторе (API 26+).

## Сборка из терминала

```bash
.\gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Как пользоваться

1. Вкладка **Рецепты** — смотрите блюда и шаги приготовления.
2. **Холодильник** → кнопка сканера → наведите камеру на штрихкод.
3. Укажите срок годности и сохраните продукт.
4. Вкладка **Готовим** — рецепты, которые можно сделать из ваших продуктов (просроченные не учитываются).

## Автообновление

Как в Quiet City: при запуске приложение читает
`https://raw.githubusercontent.com/romangolotvin/smart-pantry/main/updates/version.json`
и предлагает скачать новый APK с GitHub Releases.

При релизе поднимай:
- `UpdateConfig.APP_VERSION`
- `app/build.gradle.kts` → `versionName` / `versionCode`
- `updates/version.json` → `version` + `notes`
