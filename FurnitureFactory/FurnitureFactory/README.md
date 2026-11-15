# Furniture Factory — Android-каталог мебели с интерактивным 3D

Нативное приложение на **Kotlin + Jetpack Compose (Material 3)**, архитектура **MVVM** (ViewModel + StateFlow),
3D-движок **Google Filament + gltfio**, модели **GLB/glTF** с настоящими анимационными клипами.

## Что внутри

| Раздел | Что работает |
|---|---|
| Главная | поиск (название, категория, артикул, тип), фильтры в BottomSheet, «Популярные модели», «Все →» |
| Каталог | 6 категорий-кнопок, сетка 2 колонки, сортировка (популярность / цена ↑↓ / новизна), избранное на карточке |
| Карточка товара | 3D-модель: вращение пальцем, масштаб двумя пальцами, двойное касание — сброс, «Вращение 360°», «Сброс», полноэкранный режим; «Показать трансформацию», воспроизведение/пауза/«С начала», ползунок времени, переключение клипов, «Начало»/«Конец»; характеристики, количество, «Добавить в корзину» |
| Корзина | миниатюры, счётчик, удаление, очистка, пересчёт итога, пустое состояние с «Перейти в каталог» |
| Оформление | форма с проверкой полей, список товаров, итог, демо-заказ с уникальным номером «FF-ггммдд-NNNN», экран «Спасибо за заказ!» |
| Профиль | аватар, имя, e-mail, настройки, «Мои заказы», «Избранное», «Сравнение», «Уведомления», «О приложении», «Поддержка», «Выйти», баннер «Вдохновение для вашего дома» |
| Хранение | корзина, избранное, сравнение, заказы, профиль и настройки сохраняются между запусками (DataStore) |

## Демонстрационные 3D-модели

В `app/src/main/assets/models/` уже лежат 8 GLB-моделей (сгенерированы скриптом `tools/model_generator`):

| Файл | Товар | Анимации (glTF-клипы) |
|---|---|---|
| `sofa_komfort.glb` | Диван «Комфорт» | **Раскладывание**, **Складывание** — сиденье выкатывается, спинка поднимается и ложится в спальное место |
| `table_elegant.glb` | Стол «Элегант» | Раздвинуть стол / Сдвинуть стол — половины разъезжаются, вставка поднимается |
| `dresser_praktik.glb` | Комод «Практик» | Открыть ящики / Закрыть ящики |
| `wardrobe_classic.glb` | Шкаф «Классик» | Открыть двери / Закрыть двери |
| `armchair_relax.glb` | Кресло «Релакс» | Разложить / Сложить — подножка и откидная спинка |
| `armchair_loft.glb` | Кресло «Лофт» | нет (показывается сообщение «нет механизма трансформации») |
| `sofa_skandi.glb` | Диван «Сканди» | нет |
| `chair_nord.glb` | Стул «Норд» | нет |

Анимации — настоящие keyframe-клипы внутри GLB (каналы translation/rotation), а не видео и не GIF.
Миниатюры для карточек (`assets/images/*.png`) отрендерены из тех же моделей.
Это простые процедурные модели для демонстрации механики — замените их своими из Blender (см. ниже).

## Запуск

1. Android Studio (Narwhal 2025.1.3 или новее), JDK 17+ (встроенный JBR подходит).
2. **File → Open** → папка `FurnitureFactory`. Studio скачает Gradle 8.14.3 и зависимости.
3. Если Studio предложит установить Android SDK Platform 36 — согласитесь.
4. Запустите конфигурацию **app** на устройстве или эмуляторе с Android 8.0+ (API 26).
   В эмуляторе выберите Graphics: *Hardware* — Filament нужен OpenGL ES 3.0.
5. APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, файл появится в
   `app/build/outputs/apk/debug/app-debug.apk`. Из терминала: `./gradlew assembleDebug`.
6. Юнит-тесты логики (поиск, фильтры, корзина, валидация, ограничения камеры): `./gradlew test`.

Версии зафиксированы в `gradle/libs.versions.toml` (AGP 8.13, Kotlin 2.2.20, Compose BOM 2025.09, Filament 1.79.0).
Если Studio предложит обновить AGP — можно согласиться: код от этого не зависит.

## Добавление своей модели из Blender

1. **Масштаб:** 1 единица = 1 метр, пол на Z=0 в Blender (в glTF это Y=0), лицевая сторона — к зрителю (−Y в Blender → +Z в glTF).
2. **Материалы:** Principled BSDF (Base Color, Roughness, Metallic, Normal Map). Текстуры — PNG/JPEG,
   для мобильных достаточно 1024×1024 (лучше 512–1024).
3. **Анимации трансформации:** каждую — отдельным Action (например, «Раскладывание», «Складывание»),
   анимируйте Location/Rotation подвижных частей (объекты или кости). Последний кадр = конечное положение.
   Для удобного переключения добавьте действия в NLA (Push Down) или включите их в экспорт.
4. **Экспорт:** File → Export → glTF 2.0, Format **glTF Binary (.glb)**, Include: Selected Objects (по желанию),
   Transform: **+Y Up**, Data → Mesh: Apply Modifiers, Normals, UVs; Animation: включить,
   Mode: **Actions** (каждый Action станет отдельным клипом). Сжатие Draco оставьте выключенным,
   пока не проверите такую модель в приложении.
5. Положите файл в `app/src/main/assets/models/<id>.glb`, миниатюру — в `assets/images/<id>.png`.
6. Добавьте товар в `data/local/DemoCatalog.kt`:

```kotlin
FurnitureProduct(
    id = "sofa_new",
    name = "Диван «Новый»",
    model3DPath = "models/sofa_new.glb",
    imageUrl = "images/sofa_new.png",
    animationNames = listOf("Раскладывание", "Складывание"), // порядок кнопок; имена = Actions из Blender
    // ...цена, размеры, материал и т.д.
)
```

Компонент просмотра универсален и не привязан к экрану:

```kotlin
val viewer = rememberFurniture3DViewerState()
Furniture3DViewer(modelPath = "models/sofa_komfort.glb", state = viewer)
Button(onClick = { viewer.playTransformation() }) { Text("Показать трансформацию") }
```

**Тень под мебелью.** Если в модели есть плоскость с именем, содержащим `ContactShadow`, она
не учитывается при кадрировании камеры (так сделано в демо-моделях). Можно обойтись и без неё.

**Облегчённая версия** для слабых устройств: положите `models/<id>_lite.glb` и укажите
`modelLitePath = "models/<id>_lite.glb"` — она будет загружаться на устройствах с малым объёмом памяти.

**Перегенерация демо-моделей:** `pip install numpy pillow scipy`, затем
`python3 tools/model_generator/generate.py` (с ключом `--frames <папка>` — раскадровка анимаций для проверки).

## Структура

```
app/src/main/java/com/example/furniturefactory/
  FurnitureApp.kt, MainActivity.kt        — приложение, контейнер зависимостей, AppConfig (название)
  data/model/                              — FurnitureProduct, CartItem, Order, UserProfile
  data/local/                              — DemoCatalog, AppPreferences (DataStore), кодеки, JSON заказов
  data/repository/                         — интерфейсы + локальные реализации (Furniture/Cart/Order/User)
  domain/ProductQuery.kt                   — поиск, фильтры, сортировка (чистый Kotlin)
  presentation/navigation/                 — маршруты, NavHost, нижняя панель
  presentation/components/                 — SearchBar, FilterButton/BottomSheet, ProductCard, BottomNavigationBar,
                                             Furniture3DViewer, ViewerControls (анимация), иконки мебели
  presentation/viewer/                     — FilamentSceneHost (движок), OrbitCameraController, состояние, кэш моделей
  presentation/screens/                    — home, catalog, product, cart, checkout, profile, favorites, compare
  ui/theme/                                — Color, Type, Theme
app/src/main/assets/models|images          — GLB-модели и миниатюры
tools/model_generator/                     — генератор демо-моделей и превью (Python)
```

## Подключение сервера фабрики

Экранам не важно, откуда данные: достаточно реализовать интерфейсы `FurnitureRepository`,
`CartRepository`, `OrderRepository`, `UserRepository` (например, через Retrofit) и подменить
реализации в `AppContainer` (`FurnitureApp.kt`). Модели можно загружать по URL в локальный
файл и передавать абсолютный путь в `Furniture3DViewer` — кэш и загрузчик это поддерживают.

## Почему Filament напрямую, а не SceneView

SceneView — Compose-обёртка над тем же Filament. Её API в 2026 году меняется очень быстро
(ветка 4.x требует AGP 9.4, Kotlin 2.4 и compileSdk 37), а точного управления временем glTF-анимации
(перемотка ползунком, пауза, остановка в конечном кадре) в документации SceneView нет.
Поэтому просмотрщик построен на Filament/gltfio напрямую: стабильный API (`AssetLoader`,
`ResourceLoader`, `Animator.applyAnimation`), полный контроль над камерой и анимацией,
меньше зависимостей. Весь 3D-код изолирован в `presentation/viewer` и `Furniture3DViewer.kt`.

## Освещение и производительность

* Студийный свет: направленный ключевой с тенями, заполняющий точечный, ровный рассеянный свет;
  тонмаппинг Filmic, MSAA 4× и SSAO (на слабых устройствах — FXAA и динамическое разрешение).
* Загрузка GLB — в фоне, текстуры — асинхронно через `ResourceLoader`; байты моделей кэшируются в памяти (LRU 48 МБ).
* Отрисовка ставится на паузу при сворачивании приложения; движок и все нативные ресурсы
  освобождаются при закрытии экрана товара.
* Ошибки (нет файла, битый GLB, нет OpenGL ES 3.0) показываются в 3D-области с кнопкой «Повторить» — без падения приложения.
