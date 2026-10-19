package com.triptab.app.data

/**
 * Starting places for the Fayoum trip (22–24 Oct 2026).
 * Kom El Dikka: see KomElDikkaMenu (October 2026 menu; +12% service, +14% taxes).
 * Ibis: transcribed from the printed menu photos (menu states +12% service; no tax line printed).
 * Tibtunis and Safari: placeholders until menu / price are known — editable in the app.
 */
object FayoumSeed {

    private class MenuBuilder {
        val items = mutableListOf<MenuItem>()
        private var cat = ""
        fun cat(name: String) { cat = name }
        fun i(en: String, ar: String, price: Number) {
            items += MenuItem(category = cat, nameEn = en, nameAr = ar, price = price.toDouble())
        }
    }

    private fun menu(block: MenuBuilder.() -> Unit) = MenuBuilder().apply(block).items.toList()

    /** Kom El Dikka's current menu (see [KomElDikkaMenu]). */
    val komElDikka: Place get() = KomElDikkaMenu.place()

    val ibis = Place(
        name = "Ibis Restaurant",
        nameAr = "مطعم ايبيس",
        type = PlaceType.RESTAURANT,
        servicePct = 12.0,
        taxPct = 0.0,
        note = "Restaurant & cooking school. Menu says 12% service is added; no tax line printed.",
        items = menu {
            cat("Breakfast & sweets")
            i("Egyptian breakfast", "فطار مصري", 350)
            i("Carrot cake / banana cake", "كيكة جزر / كيكة موز", 95)
            i("Waffle / crepe (basic topping)", "وافل / كريب", 90)
            i("Waffle / crepe (premium topping)", "وافل / كريب", 125)
            i("Apple crumble with vanilla ice cream", "كرامبل التفاح مع آيس كريم فانيليا", 200)

            cat("Soups & salads")
            i("Soup special", "شوربة مخصوص", 110)
            i("Green garden salad", "سلطة خضراء مع زيت زيتون وليمون", 260)
            i("Caesar salad", "سلطة سيزر", 290)
            i("Caesar salad with chicken", "سلطة سيزر بالدجاج", 380)
            i("Green salad, apple, walnut & blue cheese", "سلطة خضراء مع تفاح وعين جمل", 320)
            i("Ibis house salad", "سلطة ايبيس المميزة", 330)

            cat("Main dishes")
            i("Shish tawook with fries", "شيش طاووق مع فرنش فرايز", 370)
            i("Chicken liver with baladi bread & tahina", "كبده دجاج مع خبز بلدي وطحينة", 320)
            i("Chicken pané with fries", "دجاج بانيه مع بطاطس", 300)
            i("Bratwurst with onion sauce & rösti", "نقانق مشوية مع صوص البصل والروستي", 330)
            i("Chicken piccata with lemon sauce", "بيكاتا دجاج مع كريمه بالليمون", 440)
            i("Fish with lemon caper / toasted almonds", "سمك مع ليمون وقبار / لوز محمص", 510)
            i("French fries", "بطاطس فرنش فرايز", 100)

            cat("Pasta")
            i("Spaghetti bolognese", "بولونيز سباجيتي", 320)
            i("Sun-dried tomato pasta", "بالطماطم المجففة", 270)
            i("Basil pesto pasta", "صوص الريحان", 285)
            i("Mushroom sauce pasta", "صوص المشروم", 290)
            i("Chicken mushroom sauce pasta", "صوص دجاج بالمشروم", 380)

            cat("Pizza")
            i("Margherita", "بيتزا مارجريتا", 210)
            i("Funghi", "بيتزا فونجي", 240)
            i("Vegetables", "بيتزا بالخضروات", 260)
            i("Salami", "بيتزا سلامي", 250)
            i("Goat cheese", "بيتزا بجبن الماعز", 300)
            i("Chicken", "بيتزا بالدجاج", 320)

            cat("Hot drinks")
            i("Espresso double", "اسبريسو دوبل", 100)
            i("Espresso", "اسبريسو", 90)
            i("Cappuccino", "كابتشينو", 105)
            i("Café latte", "لاتيه", 115)
            i("Americano", "أمريكانو", 100)
            i("Macchiato", "اسبريسو ماكياتو", 100)
            i("Nescafé / Nescafé with milk", "نسكافيه / نسكافيه باللبن", 85)
            i("French coffee", "قهوة فرنساوي", 70)
            i("Turkish coffee", "قهوة تركية", 65)
            i("Black tea (opt. mint)", "شاي أسود / بالنعناع", 55)
            i("Green tea (opt. mint)", "شاي أخضر / بالنعناع", 55)
            i("Anise tea", "يانسون", 50)
            i("Chocolate milk", "شيكولاتة باللبن", 100)
            i("Mint / cinnamon", "نعناع / قرفة", 65)
            i("Mint / star anise", "نعناع / يانسون", 65)

            cat("Cold drinks")
            i("Iced coffee", "آيس كوفي", 115)
            i("Pepsi / Pepsi Diet", "بيبسي / بيبسي دايت", 50)
            i("7UP / 7UP Zero", "سفن أب / سفن أب زيرو", 50)
            i("Tonic / soda water", "تونيك / ماء صودا", 60)
            i("Karkadeh (hibiscus)", "كركديه", 70)
            i("Lemon juice / with mint", "عصير ليمون / بالنعناع", 70)
            i("Lemon juice with karkadeh", "عصير ليمون مع كركديه", 70)
            i("Orange juice", "عصير برتقال", 90)
            i("Mango juice", "عصير مانجو", 100)
            i("Mineral water small", "مياه معدنية صغيرة", 20)
            i("Mineral water large", "مياه معدنية كبيرة", 30)
        },
    )

    val tibtunis = Place(
        name = "Tibtunis",
        nameAr = "تبتونس",
        type = PlaceType.RESTAURANT,
        note = "Menu not added yet — tap Scan menu to photograph it, or add items by hand.",
    )

    val safari = Place(
        name = "Safari",
        nameAr = "سفاري",
        type = PlaceType.ACTIVITY,
        note = "Price not set yet — tap Edit to set the per-person price.",
        items = menu {
            cat("Activity")
            i("Safari (per person)", "سفاري (للفرد)", 0)
        },
    )

    /** Fresh copies with new IDs, so each trip owns its own editable places. */
    fun places(): List<Place> = listOf(komElDikka, ibis, tibtunis, safari).map { p ->
        p.copy(id = newId(), items = p.items.map { it.copy(id = newId()) })
    }
}
