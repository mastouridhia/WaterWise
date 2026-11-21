# WaterWise – Oral defense guide

Use this to prepare the presentation and the questions. Open each file mentioned and
make sure you can explain it in your own words.

---

## 1. Pitch (1 minute)

**EN:** People forget to water their plants, or water them on a fixed schedule that ignores
the weather. WaterWise reminds you when each plant needs water and adapts the schedule of
outdoor plants to the real weather: rain counts as a watering, a heat wave brings watering
forward, and frost triggers an alert. It uses two external APIs: Perenual for plant data and
Open-Meteo for the weather.

**FR :** Les gens oublient d'arroser leurs plantes, ou les arrosent à date fixe sans tenir
compte de la météo. WaterWise rappelle quand arroser chaque plante et adapte le calendrier
des plantes d'extérieur à la météo réelle : la pluie compte comme un arrosage, une canicule
avance l'arrosage, et le gel déclenche une alerte. L'application utilise deux API
externes : Perenual (données des plantes) et Open-Meteo (météo).

**What is original:** most watering apps use fixed timers. WaterWise combines a plant
database with a live forecast and changes the schedule automatically, then explains why in
the plant's history ("Skipped: it rained 6 mm").

---

## 2. Demo script (3–4 minutes)

Before the exam: check on [open-meteo.com](https://open-meteo.com) which city has rain today
(e.g. London, Brest) and which has frost (e.g. Bergen). Have internet on the phone.

1. **Today screen** → show the weather card, alerts, plants to water. Tap 💧 on one plant.
2. **My Plants → +** → nickname "Demo aloe" → 🔍 search "aloe vera" → pick *Aloe vera*
   → the form fills itself (every 9 days, Full sun, photo). Set *Outdoor* ON,
   *Last watered* = 5 days ago → **Save**.
3. Search "monstera" → *Swiss cheese plant* shows "Choose watering manually"
   → explain the free-plan limit and the fallback dialog.
4. **Today → Change** → choose the rainy city → the demo aloe is postponed,
   banner "1 outdoor plant was adjusted" → open the plant → history "Skipped: it rained".
5. Change to the frosty city → frost alert.
6. **Settings** → show reminder time + sliders → **Check now and send the notification**
   → pull down the notification shade.
7. Turn on airplane mode → pull to refresh → "Offline: last update …" → turn it off.

---

## 3. Architecture — where things are

```
UI (Activity/Fragment + XML)  →  ViewModel (LiveData)  →  Repository  →  Room DAO / Retrofit API
```

| Question | Answer + file |
|---|---|
| Entry point? | `WaterWiseApp` (creates notification channel, schedules the daily job) → `MainActivity` (bottom navigation, 3 fragments) |
| Where is the database? | `data/AppDatabase.java` (Room, version 2), entities `Plant`, `WateringLog`, `SpeciesCache` |
| Where are the API calls? | `network/ApiClient.java` (Retrofit/OkHttp), `PerenualApi.java`, `OpenMeteoApi.java` |
| Where is the weather logic? | `weather/WeatherAdvisor.java` (pure Java, unit-tested) |
| Who applies it? | `weather/WeatherRepository.fetchAndApplyRules()` — used by the Today screen **and** the daily worker |
| Daily reminder? | `notifications/DailyCheckWorker.java` + `ReminderScheduler.java` (WorkManager) |
| Status "Water in 3 days"? | `util/WateringStatus.java` |
| API data → days? | `util/CareMapper.java` ("7-10" → 9 days) |

---

## 4. Likely questions and short answers

**Activity vs Fragment?** An Activity is a full screen with its own lifecycle; a Fragment is
a reusable part of a screen inside an Activity. MainActivity hosts 3 Fragments switched by the
bottom navigation; forms and details are separate Activities.

**Why a ViewModel?** It survives screen rotations, so data and running requests are not lost.
The UI only observes it.

**What is LiveData?** An observable value that knows the lifecycle: the screen is updated
automatically when the data changes, and only while it is visible. Room returns LiveData, so
the plant list refreshes by itself after an insert/update/delete.

**Why Room instead of raw SQLite?** Less code, SQL checked at compile time, LiveData support,
and migrations. Example: `MIGRATION_1_2` adds a column to `species_cache` without deleting the
user's plants.

**Why can't you query the database on the main thread?** It would freeze the UI (Android
throws an exception). Writes run on `AppExecutors.io()`; LiveData queries run in background
automatically.

**How does Retrofit work?** You declare an interface with annotations (`@GET`, `@Query`,
`@Path`); Retrofit generates the code; Gson converts JSON to Java objects. `enqueue()` = async
with a callback on the main thread; `execute()` = blocking, used only in background threads.

**Where is the API key? Is it safe?** In `local.properties` (git-ignored), copied into
`BuildConfig.PERENUAL_API_KEY` at build time, and added to every request by an OkHttp
interceptor. It is never in the GitHub repo. (An APK can still be decompiled — for a real
product the key would sit on a server.)

**What happens without internet?** Species search falls back to the Room cache; the weather
shows the last saved forecast; reminders still work (local data). Errors show a message,
never a crash.

**Explain the rain rule.** Outdoor plant + ≥ 5 mm rain yesterday+today + due within 1 day +
not watered since yesterday → the rain counts as a watering: last watered = now, next = now +
interval, and a `SKIPPED_RAIN` log is added. Applying it twice does nothing because "last
watered" is now today (tested in `rainIsAppliedOnlyOnce`).

**Explain the heat rule.** If max temp ≥ 32 °C, next watering = last watering + half the
interval (never in the past), only if this moves it to an earlier day.

**Why WorkManager and not AlarmManager or a Service?** WorkManager is the recommended API for
deferrable background work: it survives app close and reboots, respects battery saving, and
guarantees execution. Unique periodic work (`daily_check`) avoids duplicates.

**Notification permission?** Android 13+ requires `POST_NOTIFICATIONS` at runtime;
MainActivity asks for it, Settings shows a warning and a shortcut if it is refused. A
notification channel is required since Android 8.

**How do photos work without storage permission?** Gallery uses the Android Photo Picker (no
permission). The camera app writes into our private folder through a `FileProvider`
(temporary `content://` access). Photos are copied into `files/photos/` so they keep working
if deleted from the gallery.

**Why download the species picture instead of saving the URL?** Perenual URLs are signed and
expire after 24 hours.

**What are your tests?** 17 JUnit tests: `CareMapperTest` (API data → days) and
`WeatherAdvisorTest` (rules, idempotence, indoor plants untouched). Run with
`./gradlew testDebugUnitTest`.

**How is the APK signed?** With a release keystore (`keytool`), configured in
`build.gradle.kts` from the git-ignored `keystore.properties`.

**Did you use AI?** Yes, as allowed by the exam rules, as a programming assistant. I
designed the idea, chose the features, tested everything on the emulator and my phone, and I
can explain every class (this document is my revision sheet).

---

## 5. Limits and future improvements (shows maturity)

- Weather uses one city for all plants (could use GPS per plant).
- Perenual free plan: care data only for ids ≤ 3000 (handled with the manual fallback).
- The API key is inside the APK (a backend proxy would hide it).
- Ideas: home-screen widget, French translation, soil-moisture sensor (Bluetooth), sharing a
  plant with a family member, statistics (watering streaks).

---

## 6. Checklist before the exam

- [ ] Install `WaterWise-v1.0.apk` on your phone (from the GitHub release) and run the demo once
- [ ] Phone has internet; notifications allowed
- [ ] Know the rainy/frosty demo cities for that day
- [ ] Open the GitHub repo page: README + commit history
- [ ] Re-read sections 3 and 4
