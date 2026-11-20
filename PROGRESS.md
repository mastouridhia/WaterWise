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
| — | Push to GitHub | ⏳ To do (needs `gh auth login`) |
| 2 | My Plants list, add/edit plant, photo, plant detail | ✅ Done (2026-11-20) |
| 3 | Perenual API: species search + auto-fill + cache | ⬜ Next |
| 4 | Open-Meteo: city, forecast, Today screen, weather logic | ⬜ |
| 5 | Daily notifications (WorkManager) + Settings | ⬜ |
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

### Project structure (updated after day 2)
```
app/src/main/java/com/waterwise/app/
├── MainActivity.java              bottom navigation host
├── data/                          data layer
│   ├── AppDatabase.java           Room singleton database
│   ├── PlantRepository.java       single entry point for plant data
│   ├── Plant.java / PlantDao.java
│   ├── WateringLog.java / WateringLogDao.java
│   └── SpeciesCache.java / SpeciesDao.java
├── ui/
│   ├── today/TodayFragment.java
│   ├── plants/
│   │   ├── PlantsFragment.java + PlantsViewModel.java + PlantAdapter.java
│   │   ├── AddEditPlantActivity.java + AddEditPlantViewModel.java
│   │   └── PlantDetailActivity.java + PlantDetailViewModel.java + WateringLogAdapter.java
│   └── settings/SettingsFragment.java
└── util/
    ├── AppExecutors.java          background thread
    ├── WateringStatus.java        "Water in 3 days" + status colors
    ├── PhotoStorage.java          copy / camera file / delete photos
    ├── PlantImages.java           load photo or leaf placeholder (Glide)
    └── EdgeToEdgeUtil.java        padding for status/navigation bars
```

---

## ⏳ Right now – Push to GitHub

1. In a terminal: `gh auth login` → GitHub.com → HTTPS → login with browser.
2. Create the public repo and push:
   ```
   gh repo create WaterWise --public --source=. --push
   ```
3. Check on github.com that `local.properties` is **not** there (it holds your API key).

After that, just run `git push` at the end of each day.

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

## ⬜ Day 3 – Perenual API (species search)

Goal: type "aloe" → pick a species → the form fills itself.

- [ ] Retrofit client + `PerenualApi` interface + response models (Gson)
- [ ] Search: `GET /api/v2/species-list?key=…&q=aloe`
- [ ] Details: `GET /api/v2/species/details/{id}?key=…` → `watering`, `sunlight`, `watering_general_benchmark` (e.g. "7-10" days)
- [ ] Search dialog/screen with results list (name + thumbnail)
- [ ] Map watering level → days: Frequent = 3, Average = 7, Minimum = 14 (or use the benchmark if present)
- [ ] Save results in `species_cache`; if offline/API error → search the cache, else manual entry
- [ ] Error handling: no internet, quota exceeded, empty results (Snackbar/message, never crash)

⚠️ **Free plan limitation (already tested):** details only work for species **id ≤ 3000**.
For other species (e.g. Monstera, id 5257) the fields contain "Upgrade Plans…".
→ Detect this and let the user choose *Frequent / Average / Minimum* manually.
(Good point to mention in the defense: graceful fallback.)

Suggested commits:
1. `Add Retrofit client and Perenual API models`
2. `Add species search screen using Perenual API`
3. `Auto-fill plant form from species details`
4. `Cache species locally and handle API errors offline`

---

## ⬜ Day 4 – Weather (Open-Meteo) + Today screen

Goal: the "smart" part of the app.

- [ ] `OpenMeteoApi`: geocoding `https://geocoding-api.open-meteo.com/v1/search?name=Tunis`
      and forecast `https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..&daily=precipitation_sum,temperature_2m_max,temperature_2m_min&past_days=1&timezone=auto`
- [ ] Save chosen city + lat/lon in `SharedPreferences`
- [ ] `WeatherAdvisor` class (pure Java, easy to unit-test): input = forecast + plant, output = postpone / advance / frost alert
- [ ] Apply rules to outdoor plants and write logs (SKIPPED_RAIN / ADVANCED_HEAT)
- [ ] `TodayFragment`: weather card (city, temp, rain, alert banner) + list of plants due today with quick "Watered" button
- [ ] Unit tests for `WeatherAdvisor`

Suggested commits:
1. `Add Open-Meteo geocoding and forecast API`
2. `Add WeatherAdvisor rules for rain, heat and frost`
3. `Build Today screen with weather card and due plants`
4. `Add unit tests for WeatherAdvisor`

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
