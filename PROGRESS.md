# WaterWise – Project Roadmap & Progress

> Plant care assistant that adapts watering reminders to the **real weather**.
> Android · Java · XML · Room · Retrofit · WorkManager

**Deadline:** ~2026-11-27 (1 week from 2026-11-20)
**Rule for grading:** commit after every finished step, spread over several days (2 pts for regular history).

---

## 📊 Status at a glance

| Day | Topic | Status |
|-----|-------|--------|
| 1 | Project setup, dependencies, theme + navigation, database | ✅ Done (2026-11-20) |
| — | Push to GitHub | ✅ https://github.com/mastouridhia/WaterWise (run `git push` after each day) |
| 2 | My Plants list, add/edit plant, photo, plant detail | ✅ Done (2026-11-20) |
| 3 | Perenual API: species search + auto-fill + cache | ✅ Done (2026-11-20) |
| 4 | Open-Meteo: city, forecast, Today screen, weather logic | ✅ Done (2026-11-20) |
| 5 | Daily notifications (WorkManager) + Settings | ⬜ Next |
| 6 | Polish, README, signed APK, GitHub release | ⬜ |
| 7 | Buffer: bug fixes, test on phone, prepare oral defense | ⬜ |

---

## 🎯 The idea (for your presentation)

**Problem:** people forget to water plants, or water them on a fixed schedule that
ignores the weather. Outdoor plants get watered right after it rained, and nobody
warns you about frost or a heat wave.

**Solution:** WaterWise reminds you when each plant needs water, and adjusts
the schedule automatically using a weather forecast:

| Weather event | Rule (outdoor plants) | Result |
|---------------|-----------------------|--------|
| Rain ≥ 5 mm (configurable) | Postpone next watering | "Skipped – it rained 8 mm" |
| Heat wave ≥ 32 °C | Move next watering earlier | "Water earlier – heat wave" |
| Frost ≤ 2 °C | Alert | "Bring your plants inside tonight" |

**External APIs (2 pts):**
- **Perenual** (key required): plant species search and care data (watering, sunlight).
- **Open-Meteo Forecast** (no key): rain, temperature min/max.
- **Open-Meteo Geocoding** (no key): city name → latitude/longitude.

---

## ✅ Day 1 – Done (2026-11-20)

Commits:
```
0ea5c06 Add Room database: Plant, WateringLog and SpeciesCache entities with DAOs
bb9cdab Add green/blue theme and bottom navigation with Today, Plants, Settings screens
af9453e Add dependencies: Room, Retrofit, WorkManager, Glide, Lifecycle
8cf3a93 Initial project setup: Gradle wrapper, app module and launcher icon
```

What exists now:
- Gradle project (AGP 9.0.1, Gradle 9.1, Java 17, minSdk 26, targetSdk 36), ViewBinding on.
- API key stored **only** in `local.properties` (git-ignored) → read as `BuildConfig.PERENUAL_API_KEY`.
- Material 3 theme (green + water blue), dark mode, custom app icon (drop + leaf).
- `MainActivity` with bottom navigation → `TodayFragment`, `PlantsFragment`, `SettingsFragment` (placeholders).
- Room database `waterwise.db`:
  - `plants`: nickname, species, photo, indoor/outdoor, watering interval, sunlight, last/next watering
  - `watering_logs`: history (WATERED / SKIPPED_RAIN / ADVANCED_HEAT), deleted with the plant (CASCADE)
  - `species_cache`: Perenual results saved locally (offline search, saves API quota)

### Project structure (updated after day 3 – day 4 added `weather/` and `ui/weather/`)
```
weather/   DailyWeather, WeatherAdvisor (rules), WeatherRepository, WeatherReport, WeatherCodes
ui/today/  TodayFragment + TodayViewModel      ui/weather/CityPicker   util/AppSettings
```
```
app/src/main/java/com/waterwise/app/
├── MainActivity.java              bottom navigation host
├── network/                       external API (Retrofit)
│   ├── ApiClient.java             OkHttp + Retrofit, adds the API key
│   ├── PerenualApi.java           endpoints: species-list, species/details/{id}
│   └── model/                     JSON classes (Gson)
├── data/                          data layer
│   ├── AppDatabase.java           Room singleton database (v2 + migration)
│   ├── PlantRepository.java       single entry point for plant data
│   ├── SpeciesRepository.java     Perenual API + species cache + errors
│   ├── Plant.java / PlantDao.java
│   ├── WateringLog.java / WateringLogDao.java
│   └── SpeciesCache.java / SpeciesDao.java
├── ui/
│   ├── today/TodayFragment.java
│   ├── plants/
│   │   ├── PlantsFragment.java + PlantsViewModel.java + PlantAdapter.java
│   │   ├── AddEditPlantActivity.java + AddEditPlantViewModel.java
│   │   └── PlantDetailActivity.java + PlantDetailViewModel.java + WateringLogAdapter.java
│   ├── species/                   SpeciesSearchActivity + ViewModel + SpeciesAdapter
│   └── settings/SettingsFragment.java
└── util/                          (+ CareMapper.java: API data → watering days, unit-tested)
    ├── AppExecutors.java          background thread
    ├── WateringStatus.java        "Water in 3 days" + status colors
    ├── PhotoStorage.java          copy / camera file / delete photos
    ├── PlantImages.java           load photo or leaf placeholder (Glide)
    └── EdgeToEdgeUtil.java        padding for status/navigation bars
```

---

## ✅ GitHub

Repo: https://github.com/mastouridhia/WaterWise — `local.properties` (API key) is **not** in the
repo or its history (checked). Run `git push` at the end of each day.

---

## ✅ Day 2 – My Plants (CRUD) – Done (2026-11-20)

Goal: add, see, edit, delete plants and mark them as watered.

- [x] `PlantRepository` (runs DB writes on a background `ExecutorService`)
- [x] `PlantsViewModel` exposing `LiveData<List<Plant>>`
- [x] `PlantsFragment`: RecyclerView + `PlantAdapter` (photo, name, "water in 2 days", status color: green OK / orange today / red overdue), empty state, FAB "+"
- [x] `AddEditPlantActivity`: nickname, species (typed by hand for now), indoor/outdoor switch, watering every N days, sunlight, last watered date (DatePicker), notes, validation
- [x] Photo: pick from gallery (Photo Picker) or take with camera, shown with Glide
- [x] `PlantDetailActivity`: info, **Water now** button (updates dates + adds a log), history list, edit, delete with confirmation

Commits:
```
c7e515d Add PlantDetailActivity with Water now, history, edit and delete
3c29548 Add plant photo from gallery or camera
0eedaee Add AddEditPlantActivity form with validation and date picker
67b925d Show plants list with status colors in RecyclerView
2f0c79f Add PlantRepository, PlantsViewModel and watering status helper
```

Tested on the Pixel_9 emulator: add plant → appears in list ("Water in 10 days", green)
→ detail → Water now (history + snackbar) → edit (form pre-filled) → delete (confirmation)
→ list empty again. No crashes.
⚠️ Still to test by hand on your phone: **Gallery** and **Camera** photo buttons.

### How it works (for the oral defense)
- **Status color:** `WateringStatus` compares the next-watering *date* with today
  (`daysUntil < 0` → red overdue, `0` → orange today, `> 0` → green).
- **Water now:** `Plant.markWatered()` sets `lastWateredAt = now` and
  `nextWateringAt = now + interval days`, then a `WateringLog` row is inserted.
- **Photos:** the Photo Picker needs no storage permission; the picture is copied into
  the app's private `files/photos/` folder. The camera writes into that folder through a
  `FileProvider` (a secure `content://` link), so no CAMERA permission is needed either.
- **Delete:** the `ForeignKey(onDelete = CASCADE)` removes the history automatically,
  and the photo file is deleted too.
- **Rotation-safe:** data lives in ViewModels; form state (date, photo) is saved in
  `onSaveInstanceState`.

---

## ✅ Day 3 – Perenual API (species search) – Done (2026-11-20)

Goal: type "aloe" → pick a species → the form fills itself.

- [x] Retrofit client + `PerenualApi` interface + response models (Gson)
- [x] Search: `GET /api/v2/species-list?q=aloe` (key added by an OkHttp interceptor)
- [x] Details: `GET /api/v2/species/details/{id}` → `watering`, `sunlight`, `watering_general_benchmark` (e.g. "7-10" days)
- [x] Search screen with results list (photo, names, "Care data available" / "Choose watering manually" badge)
- [x] `CareMapper`: benchmark "7-10" → 9 days; else Frequent = 3, Average = 7, Minimum = 14 (+ 7 unit tests)
- [x] Results saved in `species_cache` (Room migration v1 → v2); offline → search the cache
- [x] Error handling: offline, quota, locked species, empty results → clear message, never crash
- [x] Species picture downloaded into app storage when the user has no photo

Commits:
```
4603ce5 Auto-fill plant form from Perenual species details
3e7cd4e Add species search screen using Perenual API
57b7eb0 Add SpeciesRepository with local cache and offline fallback
05ca198 Add CareMapper to convert Perenual care data, with unit tests
d9b4f33 Add Retrofit client and Perenual API models
```

Tested on the emulator:
- "aloe vera" → pick *Aloe vera* → form filled: **every 9 days · Full sun** + photo ✅
- "monstera" → *Swiss cheese plant* (locked) → watering-level dialog, **no API request wasted** ✅
- Airplane mode: "aloe" → cached results + "You are offline" message; "cactus" → "No internet connection" ✅
- `./gradlew test` → 7/7 tests pass ✅

### Things learned about the Perenual free plan (good for the oral defense)
| Fact | How the app handles it |
|------|------------------------|
| Details only for species **id ≤ 3000**; others answer **HTTP 429** with a text body | `FREE_PLAN_MAX_ID`: locked species are never requested → manual Frequent/Average/Minimum dialog |
| ~100 requests/day | Search only on "search" key (not every letter) + details cached per species |
| Image urls are **signed and expire after 24 h** | The picture is **downloaded** into the app's private folder, not saved as a url |
| Sometimes slow (~20 s) | Read timeout raised to 30 s + progress bar |
| `sunlight` format may vary | Parsed as `JsonElement` (string or list) so parsing can't crash |

### How it works (data flow)
```
SpeciesSearchActivity ──► SpeciesSearchViewModel ──► SpeciesRepository ──► Retrofit (PerenualApi)
                                                         │   ▲                   │ fails?
                                                         ▼   │                   ▼
                                                     Room species_cache ◄── fallback search
AddEditPlantActivity ◄── chosen species ── getCareInfo(): cache first, then API, else manual dialog
```

---

## ✅ Day 4 – Weather (Open-Meteo) + Today screen – Done (2026-11-20)

Goal: the "smart" part of the app.

- [x] `OpenMeteoApi`: geocoding (`search?name=Tunis`) + forecast (yesterday → day after tomorrow)
- [x] City + lat/lon + thresholds saved in `SharedPreferences` (`AppSettings`)
- [x] `WeatherAdvisor` (pure Java): rain → postpone, heat → advance, frost → alert
- [x] Rules applied to outdoor plants, history entries SKIPPED_RAIN / ADVANCED_HEAT
- [x] `TodayFragment`: weather card + alert banners + plants due today with quick 💧 button, pull to refresh
- [x] 10 unit tests for `WeatherAdvisor` (17 tests in total, all pass)
- [x] Offline: last forecast saved and shown ("Offline: last update …")

Commits:
```
bc439c4 Build Today screen with weather card, alerts and plants due today
58bc47b Add WeatherRepository and city picker
a8ef6eb Add WeatherAdvisor rules for rain, heat and frost, with unit tests
241229f Add Open-Meteo geocoding and forecast API and app settings
```

Tested on the emulator **with real weather**:
- **London** (6 mm rain): outdoor "Balcony rose" due today → postponed to Nov 23,
  history "Skipped: it rained · It rained 6 mm", banner "1 outdoor plant was adjusted" ✅
- Pull to refresh again → **not applied twice** ✅
- **Bergen** (−3 °C): frost alert banner ✅
- Airplane mode → "Offline: last update …" with the saved forecast ✅

### The weather rules (for the oral defense)
Only for plants marked **Outdoor**. Thresholds: rain 5 mm, heat 32 °C, frost 2 °C.

| Rule | Condition | Effect | Why it is not applied twice |
|------|-----------|--------|-----------------------------|
| 🌧️ Rain | rain yesterday + today ≥ 5 mm, plant due within 1 day, not watered since yesterday | rain counts as a watering: next = now + interval | after it, "last watered" = today, so the condition is false |
| 🔥 Heat | max temp today/tomorrow ≥ 32 °C | next = last watered + **half** the interval (never in the past) | applied only if it moves the watering to an earlier **day** |
| ❄️ Frost | min temp today/tomorrow ≤ 2 °C | alert banner only | no change to the schedule |

Demo tip: choose a city where it is raining today (check on open-meteo.com) to show the rain rule live.

---

## ⬜ Day 5 – Notifications + Settings

- [ ] Notification channel + `POST_NOTIFICATIONS` permission request (Android 13+)
- [ ] `DailyCheckWorker` (WorkManager, periodic 24 h): fetch weather → apply rules → notify "3 plants need water today" / frost alert
- [ ] Tapping the notification opens the Today screen
- [ ] `SettingsFragment`: city search, notification time, rain threshold, heat threshold, enable/disable notifications
- [ ] Re-schedule worker when settings change

Suggested commits:
1. `Add notification channel and permission request`
2. `Add DailyCheckWorker with WorkManager`
3. `Add settings screen for city, time and thresholds`

---

## ⬜ Day 6 – Polish + delivery

- [ ] Fix UI details (margins, empty states, icons, texts in `strings.xml`)
- [ ] Test on emulator **and** on your phone (rotation, no internet, delete plant, etc.)
- [ ] `README.md`: description, features, screenshots, APIs used, how to build (add your own key in `local.properties`)
- [ ] Generate signed release APK: Android Studio → *Build → Generate Signed App Bundle / APK → APK* (keep the `.jks` file **out of git**)
- [ ] GitHub Release `v1.0` with the APK attached:
      `gh release create v1.0 app/release/app-release.apk --title "WaterWise v1.0"`

Optional extras if time remains: watering streak / plant health score, home-screen widget, French translation.

---

## ⬜ Day 7 – Buffer + oral defense prep

Be ready to explain:
- **Architecture:** Activity/Fragment → ViewModel (LiveData) → Repository → Room DAO / Retrofit API
- **Why Room:** local SQLite, works offline, LiveData updates the UI automatically
- **Why WorkManager:** reliable background work even if the app is closed or phone restarts
- **Why the key is in `local.properties`:** never publish secrets on a public repo
- **Weather rules:** show `WeatherAdvisor` and its tests
- **API fallback:** Perenual free plan limit → cache + manual choice
- Demo scenario: add an outdoor plant → set city → show rain postponing the watering → notification

---

## 🛠 Useful commands

```bash
./gradlew assembleDebug          # build debug APK → app/build/outputs/apk/debug/
./gradlew installDebug           # install on connected phone/emulator
./gradlew test                   # run unit tests
git add -A && git commit -m "…"  # commit a finished step
git push                         # send to GitHub
```
