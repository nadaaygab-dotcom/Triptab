# TripTab

An Android app for families travelling together to keep track of who ordered what — food, drinks and activities — and how much each person owes, including service charge and tax.

## Install

On your Android phone, tap **[Download TripTab-0.2.apk](https://github.com/nadaaygab-dotcom/Triptab/raw/main/apk/TripTab-0.2.apk)** (Android 8.0 or newer), then open the downloaded file, and allow installing from that source when asked. If Google Play Protect warns that it doesn't recognise the app, tap **More details → Install anyway**.

## What it does

- Create a trip (name and dates), add yourself and your family members.
- Pick who is ordering and which day, then tap a restaurant or activity to open its menu.
- Tap **+** to add items; the bill updates live with subtotal, service, tax and the amount due.
- **All days** sums every day of the trip; the summary shows totals per person, per place and per day, and can be shared on WhatsApp.
- Places, menus and prices can be added or edited in the app.
- Works fully offline; data stays on the phone.

Comes with the Fayoum trip places (22–24 Oct 2026): Kom El Dikka (+12% service, +14% tax), Ibis Restaurant (+12% service), Tibtunis (menu to be added) and Safari.

## Build from source

Requires JDK 17+ and the Android SDK (platform 35).

```
./gradlew assembleRelease     # APK in app/build/outputs/apk/release/
./gradlew testDebugUnitTest   # unit tests
```

Kotlin + Jetpack Compose. Storage sits behind `TripRepository`, so shared sync can be added later without changing the screens.
