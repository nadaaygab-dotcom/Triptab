package com.triptab.app.data

/**
 * Blooms & Beans, Lake Town branch — from the café's PDF menu (shared 26 Nov 2026).
 * English only. "All prices excluded 14% VAT and 12% Service Tax" → +12% service, +14% tax.
 * Cappuccino and latte come in two sizes (R/L) → one item per size.
 * Obvious typos fixed: Mozarella, Slated, Foccacia, Cantaloup, Buneo.
 */
object BloomsAndBeansMenu {
    const val NAME = "Blooms & Beans (Lake Town)"

    /** Section, English name, price (EGP, before service and tax). */
    val ITEMS: List<Triple<String, String, Int>> = buildList {
        fun s(cat: String, vararg items: Pair<String, Int>) = items.forEach { add(Triple(cat, it.first, it.second)) }

        s("Breakfast",
            "Truffle morning scramble" to 350,
            "Croissant egg royal" to 480,
            "Mozzarella pastrami omelette" to 340,
            "Scamorza mushroom omelette" to 365,
            "Breakfast bagel" to 330,
            "Provolone spinach omelette" to 375)
        s("Sandwiches",
            "Salmon avocado bagel" to 450,
            "Halloumi bagel" to 320,
            "Blooming tuna melt" to 330,
            "Classic club sandwich" to 400,
            "Signature roast beef ciabatta" to 430,
            "Sweet halloumi ciabatta" to 350,
            "Signature steak sandwich" to 450,
            "Bresaola and truffle baguette" to 450,
            "Ciabatta chicken sandwich" to 375,
            "Mozzarella pesto focaccia" to 420)
        s("Appetizers",
            "Chicken fingers" to 285,
            "Chicken quesadillas" to 365,
            "House chips" to 60,
            "French fries" to 70)
        s("Soups",
            "Mushroom soup" to 150,
            "Pumpkin soup" to 120)
        s("Salads",
            "Buffalo chicken salad" to 400,
            "Truffled beef tenderloin salad" to 500,
            "Smoked salmon salad" to 500,
            "Goat cheese salad" to 385,
            "Signature shrimp salad" to 500,
            "Mediterranean halloumi salad" to 320,
            "Chicken ranch salad" to 400)
        s("Pastas",
            "Rigatoni pomodoro" to 345,
            "Pasta al tartufo" to 474,
            "Linguine chicken Alfredo" to 440,
            "Spaghetti funghi aglio e olio" to 265,
            "Linguine shrimp Alfredo" to 520,
            "Spaghetti carbonara" to 390,
            "Tagliatelle al ragù" to 420,
            "Creamy shrimp spaghetti" to 520)
        s("Main courses",
            "Lemon butter salmon fillet" to 820,
            "Crispy chicken parm" to 495,
            "Amelie smashed burger" to 465,
            "BBQ smashed burger" to 445,
            "Veal Milanese" to 665,
            "Mozzarella stuffed chicken breast" to 520,
            "Mushroom fillet" to 690)
        s("Dessert",
            "Mini cheesecake" to 100,
            "Mini lemon meringue" to 100,
            "Lotus cheesecake" to 235,
            "Mini pecan bites" to 105,
            "Lemon berry cake" to 250,
            "Carrot cake" to 195,
            "Sweet potato pie" to 185,
            "Date cake" to 195,
            "Brookie" to 130,
            "Mini chocolate chip cookie" to 85,
            "Mini double chocolate chip" to 85,
            "Chocolate praline cake" to 245)
        s("Hot coffees",
            "Espresso single shot" to 80,
            "Espresso double shot" to 85,
            "Espresso macchiato" to 90,
            "Double espresso macchiato" to 95,
            "Caramel macchiato" to 160,
            "Cortado" to 110,
            "Cappuccino (regular)" to 120,
            "Cappuccino (large)" to 130,
            "Latte (regular)" to 120,
            "Latte (large)" to 130,
            "Hazelnut Irish latte" to 160,
            "Caramel cinnamon latte" to 160,
            "Salted caramel latte" to 160,
            "Flat white" to 110,
            "Americano" to 100,
            "Dirty chai" to 160,
            "White mocha" to 165,
            "Pumpkin spiced latte" to 145,
            "Pistachio latte" to 190,
            "Mocha" to 150,
            "Spanish latte" to 145,
            "V60 pour-over brewed coffee" to 170,
            "Dirty espresso" to 185,
            "Bueno frappe" to 200)
        s("Cold coffees",
            "Iced Spanish latte" to 155,
            "Iced latte" to 145,
            "Coffee frappe" to 180,
            "Salted caramel frappe" to 195,
            "Iced dirty chai" to 190,
            "Caramel coffee frappe" to 195,
            "Chocolate coffee frappe" to 195,
            "Vanilla coffee frappe" to 195,
            "Iced white mocha" to 165,
            "Iced salted caramel coffee" to 165,
            "Iced pistachio" to 195,
            "Pistachio coffee frappe" to 220,
            "Iced V60 pour-over brewed" to 170)
        s("Hot drinks",
            "Hot chocolate" to 185,
            "Apple cider" to 120,
            "Tea" to 70,
            "Green tea" to 70,
            "Hot turmeric" to 105,
            "Chai latte" to 150,
            "Mint" to 70,
            "Matcha latte" to 175)
        s("Cold drinks",
            "Iced tea peach" to 145,
            "Iced tea lemon" to 110,
            "Iced chocolate" to 150,
            "Iced salted caramel" to 145,
            "Iced chai" to 170,
            "Iced matcha latte" to 185,
            "Matcha frappe" to 185,
            "Strawberry matcha" to 200)
        s("Shakes",
            "Peanut butter shake" to 175,
            "Caramel frappe" to 160,
            "Vanilla frappe" to 155)
        s("Mocktails",
            "Classic mojito" to 150,
            "Strawberry mojito" to 175,
            "Passion fruit mojito" to 175,
            "Blueberry mojito" to 175)
        s("Juices",
            "Orange" to 100,
            "Lemon" to 110,
            "Lemon mint" to 120,
            "Avocado" to 170,
            "Orange lemon" to 105,
            "Kiwi smoothie" to 145,
            "Pink lemonade" to 145,
            "Guava mint" to 100,
            "Creamy frozen lemonade" to 160,
            "Watermelon" to 120,
            "Watermelon lemonade" to 120,
            "Cantaloupe" to 100)
        s("Soft drinks",
            "Water" to 25,
            "Sparkling water" to 65,
            "Coke" to 65,
            "Coke Zero" to 65,
            "Sprite" to 65,
            "Red Bull" to 140)
        s("Extras",
            "Extra espresso shot" to 50,
            "Plant-based milk (almond / coconut / oat)" to 50,
            "Flavour (vanilla / caramel / hazelnut / cinnamon / Irish)" to 40)
    }

    fun place(): Place = Place(
        name = NAME,
        type = PlaceType.RESTAURANT,
        servicePct = 12.0,
        taxPct = 14.0,
        note = "Open daily 10am–3am. Breakfast weekdays 10am–2pm, weekends 10am–3pm. " +
            "Rigatoni pomodoro comes as a half portion for kids (ask for the price). Prices exclude 12% service and 14% VAT.",
        items = ITEMS.map { (cat, en, price) -> MenuItem(category = cat, nameEn = en, price = price.toDouble()) },
    )
}

/** Built-in menus that can be added to any trip from "Add a place". */
object SavedMenus {
    val all: List<Pair<String, () -> Place>> = listOf(
        BloomsAndBeansMenu.NAME to BloomsAndBeansMenu::place,
        KomElDikkaMenu.NAME to { FayoumSeed.komElDikka },
        FayoumSeed.ibis.name to { FayoumSeed.ibis },
    )

    /** A fresh copy (new item ids) of a saved menu, with the given place id. */
    fun copy(make: () -> Place, placeId: String): Place {
        val p = make()
        return p.copy(id = placeId, items = p.items.map { it.copy(id = newId()) })
    }
}
