# Поддерживаемые версии Android

Одна оболочка — несколько версий Android. **Базовая (референсная) версия —
Android 10, API 29**: под неё пишется весь наш код, системные правки и модули.
Версии ниже и выше — цели переноса, а не отдельные проекты.

От Android 10 отсчитываются **поколения ZerOS** (10 → ZerOS I, 11 → II,
12 → III, 13 → IV). Правила нумерации — в [`versions.md`](versions.md).
Для этих аппаратов это значит: marmite на LineageOS 17.1 — **ZerOS I**,
a5xelte на LineageOS 20 — **ZerOS IV**. Всё, что ниже Android 10, в линию
ZerOS не входит.

Диапазоны взяты по спискам прошивок на 4PDA, а не «на глаз»: ниже перечислено
то, что реально существует для этих аппаратов.

## marmite — Wileyfox Swift 2 / 2 Plus / 2 X

Тема 4PDA: `4pda.to/forum/index.php?showtopic=894518` (неофициальные прошивки).

| Android | API | Что есть |
|---|---|---|
| 6.0.1 | 23 | CyanogenMod 13.0, LineageOS 13.0, Bliss ROM 6.4, Mokee 60.1, ResurrectionRemix-M 5.7.4 |
| 7.1.2 | 25 | LineageOS 14.1 (DinD_Ykt, Kirill_MegaFon), CrDroid, AIM ROM, Mokee 71.2, RR 5.8.5 |
| 8.1 | 27 | LineageOS 15.1 (Zent16, vm03), Pixel Experience 8.1, RR 6.1.0 |
| 9 | 28 | LineageOS 16.0, LineageOS Extended 16.0, AOSIP, DerpFest, AOSP Extended, Arrow OS, Bootleggers 4.3, crDroid 5.11/5.12, Evolution X, Havoc 2.9, ion OS, Pixel Experience 9, PE Plus 9, Superior OS |
| 10 | 29 | LineageOS 17.1 (official), crDroid 6.15, Pixel Experience 10 |
| 11 | 30 | LineageOS 18.1 |

**Диапазон: 6 → 11.** Android 5 на marmite нет: аппарат вышел на 6.0.1, ниже
шестёрки в теме ничего не лежит. Верхняя граница — 11, дальше устройство
официально не поехало.

## a5xelte — Samsung Galaxy A5 2016 (SM-A510F)

Тема 4PDA: `4pda.to/forum/index.php?showtopic=794247` (неофициальные прошивки).

| Android | API | Что есть |
|---|---|---|
| 5.1.1 | 22 | только сборки на стоковой базе (KF ROM 2016) — не AOSP |
| 6.0.1 | 23 | ResurrectionRemix, ENIGMA N7/S8, XtreStoLite, Kyubi, GeForce 5.1, DeluxeROM и др. |
| 7.0/7.1 | 24/25 | AOSP Extended 4.3, Revolution OS, Explosion-OS, LyOS, CryRom, SyneXOS, Centaurus II |
| 8.1 | 27 | LineageOS 15.1, RR 6.1, DotOS, AOKP |
| 9 | 28 | LineageOS 16.0, AICP, Bootleggers, crDroid 5.9, AOSP Extended, RR 7.0.2 / Pixel Experience, Mokee, Cesium, DotOS 3 |
| 10 | 29 | LineageOS 17.0 (official), LineageOS 17.1, RR 8.7.3, FreakyOS, AIMROM 4.0 |
| 11 | 30 | LineageOS 18.1, crDroid 7.16, Havoc 4.16, Project Sakura 5.2, /e/, Floko 4.0 |
| 12 | 31 | LineageOS 19.1 (unofficial alpha) |
| 13 | 33 | LineageOS 20 (unofficial) — стоит сейчас |

**Диапазон: 6 → 13.** Пятёрка — только стоковые сборки Samsung, AOSP-базы под
неё нет, поэтому нижняя граница — 6. Верхняя — 13, новее для этого аппарата
нет.

## Что значит «поддержка версии»

Три уровня, и они не равны:

| Уровень | Что работает | От чего зависит |
|---|---|---|
| **A. Оболочка** | HomeWatch, наши APK, `.zpk` | только от `minSdk` и публичных API |
| **B. Подмены** | брендинг, «О телефоне», обновлятор (Magisk) | от версии Android: пути, подписи, код системных APK |
| **C. Своя сборка** | своя ОС целиком | от ветки LineageOS и device tree |

Цель по всем версиям диапазона — уровень **B**, где это возможно. Уровень **C**
делается только для базовой версии: собирать семь разных ОС никто не будет.

## Что нельзя переносить между версиями

- **Системные APK** (`Settings`, `Updater`): Java-код и ресурсы меняются от
  версии к версии. Пересобранный под Android 10 `Settings.apk` на Android 13
  просто не установится. Патч-скрипт общий, сборка — на каждую ветку своя.
- **Подписи**: на 10-й — platform/testkey из AOSP; на 13-й сборка подписана
  `dev-keys`, ключи только у автора прошивки. Значит, подменять системные APK
  на 13-й можно лишь с root и не через обычную установку.
- **Пути**: на marmite системные приложения в `/system/product/priv-app`, на
  a5xelte — в `/system/system_ext/priv-app`. Модуль пишется под конкретную
  раскладку.
- **Лаунчер**: HomeWatch — `Launcher3QuickStep` из AOSP, у каждой ветки свой
  QuickStep-код. Один APK на все версии не соберётся, нужна сборка на ветку.
- **RRO-оверлеи**: имена и типы ресурсов SystemUI меняются; оверлей на 10-ю не
  применится к 13-й.
- **Разрешения**: с Android 13 появились `POST_NOTIFICATIONS`, ограничения
  фоновых сервисов и точные будильники — код должен их учитывать, а не падать.

## Правила для нашего кода

1. `minSdk = 23` (Android 6) для всего, что должно работать во всём диапазоне.
2. `targetSdk` — как у базовой версии (29), пока не решено иначе.
3. Новые API — только с проверкой версии или через `values-vXX`, без
   `reflection`-костылей.
4. Один APK на все версии, пока это возможно; на версию размножаются только
   системные модули и лаунчер.
5. Каждая сборка проверяется на **10-й** — это база. Версии 11 и 13 — вторые
   по важности, 6–9 — по остаточному принципу.

## Порядок охвата

1. Android 10 (API 29) — база, всё делается здесь.
2. Android 13 (a5xelte) и 11 (marmite) — современные ветки.
3. Android 9 и 8.1 — ещё живые сборки.
4. Android 7.1 и 6.0 — минимум, без гарантий по системным подменам.

Отметка «проверено» ставится только после запуска на устройстве и чтения
интерфейса через `uiautomator dump`, а не по факту сборки.
