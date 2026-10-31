# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Heartwood-v<версия>.aab`.
  Первая версия для Play — **v1.1.1**: AAB добавляется к уже вышедшему релизу повторным запуском
  Actions → Release → Run workflow → `1.1.1`, без нового релиза и поста в Telegram. Секреты `SIGNING_*` в репозитории заданы;
  без них сборка подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.heartwood`.
  2. **Select key** → ключ с SHA-256 `dc8595a737b4f77504ec92c4b070ba8775ae92417c24f2eefe7af9b9d2249dd8`
     (сертификат CN=Heartwood, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке оставьте вариант «Google создаст ключ подписи».
  Ваш keystore станет ключом загрузки (upload key) — храните его.
  ⚠️ Из-за этого APK из GitHub и версия из Play будут подписаны разными ключами: поверх друг друга они
  не обновятся. Если нужно, чтобы совпадали, выберите «Использовать свой ключ» и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге).
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/heartwood/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Heartwood: Befriend a Tree` |
| Default language | English (United States) – en-US |
| App or game | **App** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из рендеров README (`docs/screenshots/<язык>`):
Play не берёт картинки длиннее двух ширин, а рендеры 1080×2341, поэтому экран уходит за нижний край
под подписью. Как пересобрать:

```bash
./gradlew testDebugUnitTest -Prender --tests '*RenderArtScreens*' && python tools/shots.py
python tools/play.py   # tree, grove, meet, hug, key, chronicle, herbarium, hollow
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | App → **Lifestyle** |
| Tags (до 5) | Nature, Outdoors, Journal, Walking, Plants (берите ближайшие из списка Play) |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/heartwood/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Это ритуал прогулок к дереву и дневник дружбы с ним — ближе всего Lifestyle.
Альтернатива — **Education** (определитель пород, гербарий), но там сильнее спрос на соответствие
политике Families, см. «Target audience».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*
(Встречи требуют быть в 50 м от дерева — это не ограничение доступа; при желании в поле
для ревьюера можно написать, что дерево можно посадить в любом месте, где находишься.)

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types**.
- Насилие, кровь, сексуальный контент, ненормативная лексика, наркотики, алкоголь, табак,
  азартные игры — везде **No**.
- **Users interact / exchange content** — **No**: чата, профилей и сервера нет. Передача дерева —
  ссылка или QR-код, которые пользователь сам отправляет через другие приложения.
- **Shares user's current location with other users** — **No**: в ссылке точка дерева, а не текущее
  положение, и отправляет её сам пользователь.
- Цифровые покупки — **No**.
- Ожидаемый рейтинг — Everyone / 3+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over** (политика: приложение не для детей младше 13).
- *Could your store listing unintentionally appeal to children?* → **No**: гравюры, старая топосъёмка,
  тетрадь лесника, без мультяшных героев. Из текстов Play убрано «у школы» из README.
  Если Play всё же отнесёт к детской аудитории, понадобится соответствие политике Families
  (в т.ч. геолокация у детей) — тогда пишите, разберём.

**News apps** — **No.**

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**

Здесь отличие от lot: приложение ходит в сеть, и координаты дерева (округлённые до 3 знаков, ~100 м)
уходят погодным сервисам, а запросы тайлов OSM тоже выдают район. По определению Google это
сбор **приблизительного местоположения**, поэтому честно объявляем его.

- *Does your app collect or share any of the required user data types?* → **Yes.**
- *Is all of the user data collected by your app encrypted in transit?* → **Yes** (все запросы по HTTPS).
- *Do you provide a way for users to request that their data is deleted?* → **No** — на серверах
  разработчика ничего не хранится, аккаунтов нет; удаление приложения стирает всё.
- Типы данных: только **Location → Approximate location**:
  - Collected: **Yes**; Shared: **No** (передача сервис-провайдерам — Open-Meteo / MET Norway / OSM —
    по правилам Play не считается «sharing»).
  - *Is this data processed ephemerally?* → **Yes** (сервисы отвечают погодой/тайлом, разработчик ничего не хранит).
  - *Is this data required or optional?* → **Required** (без сети нет карты и погоды, отключить нельзя).
  - Purpose: **App functionality**.
- Всё остальное **не** объявляется: точное местоположение используется только на устройстве
  (проверка 50 м), фото, записи с микрофона, записки и встречи хранятся только на телефоне.
  Ссылки на сообщество и GitHub в настройках открывают Telegram или браузер по нажатию — это не сбор
  данных разработчиком. Системный бэкап Android (`allowBackup="true"`) не декларируется.
- В карточке будет «Location — collected», «No data shared with third parties», «Data is encrypted in transit».

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — No (объятие с сердцебиением — не измерение пульса, а вибрация).

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Permissions** — специальных деклараций не нужно: `ACCESS_FINE/COARSE_LOCATION` только в
активном приложении (фонового `ACCESS_BACKGROUND_LOCATION` нет), `RECORD_AUDIO`,
`POST_NOTIFICATIONS`, `INTERNET`, `VIBRATE` — обычные разрешения без форм. Камеры нет: фото снимает
системное приложение камеры.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Heartwood-v1.1.1.aab` из GitHub Release.
3. Release name: `1.1.1`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу; карта грузится, погода у встречи записывается, разрешения на местоположение,
   микрофон и уведомления спрашиваются.

## Следующие версии

- `release_notes.txt` переписать под новую версию (для первой там «First release on Google Play»).
- Скриншоты пересобирать, только если поменялись показанные экраны.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.heartwood`.
