# TripTab

An Android app for families travelling together to keep track of who ordered what — food, drinks and activities — and how much each person owes, including service charge and tax.

## Install

On your Android phone, tap **[Download TripTab-0.10.apk](https://github.com/nadaaygab-dotcom/Triptab/raw/main/apk/TripTab-0.10.apk)** (Android 8.0 or newer), then open the downloaded file, and allow installing from that source when asked. If Google Play Protect warns that it doesn't recognise the app, tap **More details → Install anyway**.

## What it does

- Create a trip (name and dates), add yourself and your family members.
- **Several families on one phone**: add another family (e.g. your sister's) to the same trip — each has its own people, orders and bill; the summary shows each family and the total, and the Excel bill keeps each family separate so the admin sees them as separate families.
- Pick who is ordering and which day, then tap a restaurant or activity to open its menu.
- Tap **+** to add items; the bill updates live with subtotal, service, tax and the amount due.
- **All days** sums every day of the trip; the summary shows totals per person, per place and per day, and can be shared on WhatsApp.
- Places, menus and prices can be added or edited in the app.
- **Scan menu**: photograph a menu and the items and prices are filled in for you to review. Works offline on the phone for English menus; with your own Anthropic API key (Settings), Claude reads Arabic and English menus in any layout.
- **Send bill as Excel** (Summary screen): exports the family's whole trip — every person, day and place — to share on WhatsApp.
- **Admin mode** (Settings): import the Excel bills families send you (Share / Open with → TripTab from WhatsApp, or Import bills) to see one compiled order with each family's and each person's bill, filter by day, add your own trip, and export the compiled order to Excel. A newer file from the same family replaces their older one.
- **Kitchen order** (admin, compiled order): totals how many of each dish everyone ordered for one restaurant and day (e.g. 3 × Grilled Kofta), in course order, and sends it to the restaurant as a WhatsApp message or Excel so they can prepare before the group arrives.
- **Packing checklist** for every trip, starting with a ready-made list (bedding & bath, clothes, health, food & kitchen, fun & tech, money): tick items off, add/edit/delete, hide packed items, uncheck all for the trip home, and share what's left to pack on WhatsApp.
- Works fully offline; data stays on the phone.

Comes with the Fayoum trip places (22–24 Oct 2026): Kom El Dikka (first menu, with the prices the restaurant charged on the 22 Oct receipt; +12% service, +14% tax), Ibis Restaurant (+12% service), Tibtunis (menu to be added) and Safari.

## Build from source

Requires JDK 17+ and the Android SDK (platform 35).

```
./gradlew assembleRelease     # APK in app/build/outputs/apk/release/
./gradlew testDebugUnitTest   # unit tests
```

Kotlin + Jetpack Compose. Storage sits behind `TripRepository`, so shared sync can be added later without changing the screens.
