# Video Quality Picker — MargyT Plugin

Плагин для [MargyT](https://github.com/narezany/MargyT), который добавляет выбор качества видео перед скачиванием в TikTok.

## Структура

```
margyt.videoqualitypicker/
├── manifest.json
├── icon.png
└── java/
    └── VideoQualityPicker.java
```

## Как это работает

| Шаг | Хук | Что происходит |
|-----|-----|----------------|
| 1 | `onFeed` | Перехватывает список постов до отрисовки. Через reflection находит в каждом Aweme-объекте список `bitrateInfo` и кэширует варианты качества (gearName, bitrate, URL) по ID видео |
| 2 | `onScreen` | При каждом открытии экрана проверяет, является ли он экраном просмотра видео (по классовому имени). Если да — показывает плавающую кнопку через margyt().offer() |
| 3 | Нажатие кнопки | Строит AlertDialog с вариантами качества из кэша: 1080p, 720p, и т.д. |
| 4 | Выбор качества | Запускает margyt().away() для фонового скачивания через margyt().fetch() |
| 5 | Сохранение | Сохраняет mp4 в папку Загрузки через MediaStore (Android 10+) |

## Сборка

Из корня MargyT репозитория:

```bash
python3 -m margyt.plugin /путь/к/margyt.videoqualitypicker
```

## Установка

1. TikTok → Setting MargyT → Plugins → Install a plugin
2. Выбрать .mtp файл, включить переключатель

## Использование

1. Пролистать ленту (плагин кэширует видео)
2. Открыть видео в полноэкранном режиме
3. Нажать плавающую кнопку "Download Quality"
4. Выбрать качество из диалога
5. Видео сохранится в Downloads
