package com.triptab.app.data

/**
 * Kom El Dikka (Agri Lodge) menu.
 *
 * Version 2 = the October 2026 menu (English only, new sections, ~20% higher prices).
 * Version 1 = the earlier bilingual menu the app first shipped with.
 *
 * Phones that already have a trip keep their orders: [upgrade] updates each old dish in place
 * (same item id, so orders follow it), adds new dishes, and drops removed dishes nobody ordered.
 */
object KomElDikkaMenu {
    const val NAME = "Kom El Dikka"
    /**
     * 4 = first menu, with the prices the restaurant actually charged on the 22 Oct 2026 receipt ([RECEIPT_PRICES]).
     * 3 = back to the first (bilingual) menu with its original prices (user's choice, 22 Oct 2026).
     * 2 = the October 2026 menu as printed. 0/1 = the first menu with its original prices.
     */
    const val VERSION = 4

    /** A dish on the first menu: section, English, Arabic, original price. */
    data class V1Item(val category: String, val nameEn: String, val nameAr: String, val oldPrice: Int)

    val V1_MENU: List<V1Item> = listOf(
        V1Item("Salads", "Baladi salad", "سلطة بلدي", 42),
        V1Item("Salads", "Pickled eggplant", "باذنجان مخلل", 44),
        V1Item("Salads", "Tabbouleh", "تبولة", 78),
        V1Item("Salads", "Rocket salad", "سلطة جرجير", 114),
        V1Item("Salads", "Tahini", "طحينة", 72),
        V1Item("Salads", "Fattoush", "فتوش", 104),
        V1Item("Salads", "Pickles", "مخلل", 25),
        V1Item("Salads", "Baba ghanoush", "بابا غنوج", 66),
        V1Item("Appetizers", "Sambousek", "سمبوسك", 150),
        V1Item("Appetizers", "Chicken strips", "ستربس فراخ", 282),
        V1Item("Appetizers", "Shrimp with spinach", "سبانخ جمبري", 342),
        V1Item("Appetizers", "Mombar", "ممبار", 138),
        V1Item("Appetizers", "Sausage (sogo')", "سجق", 252),
        V1Item("Appetizers", "Alexandrian liver", "كبدة اسكندراني", 234),
        V1Item("Appetizers", "Beef mint", "بيف منت", 252),
        V1Item("Appetizers", "Spicy potatoes", "بطاطس حارة", 72),
        V1Item("Appetizers", "Hummus fatteh", "فتة حمص", 210),
        V1Item("Appetizers", "Vine leaves", "ورق عنب", 90),
        V1Item("Appetizers", "Kobeba", "كوبيبه", 282),
        V1Item("Soups", "Vegetable soup", "شوربه خضار", 90),
        V1Item("Soups", "Mushroom soup", "شوربه مشروم", 125),
        V1Item("Soups", "Tomato soup", "شوربه طماطم", 100),
        V1Item("Soups", "Chicken soup", "شوربه فراخ", 140),
        V1Item("Main dishes", "Fillet with mushroom", "فليه مشروم", 540),
        V1Item("Main dishes", "Freekeh tagine (plain)", "طاجن فريك سادة", 300),
        V1Item("Main dishes", "Grilled kofta", "كفته مشوية", 360),
        V1Item("Main dishes", "Half deboned chicken", "نصف فراخ مسحب", 378),
        V1Item("Main dishes", "Shish tawook", "شيش طاووق", 371),
        V1Item("Main dishes", "Mixed grill", "مكس جريل", 822),
        V1Item("Main dishes", "Veal kebab", "كباب بتلو", 720),
        V1Item("Main dishes", "Veal ribs", "رايش بتلو", 780),
        V1Item("Main dishes", "Rosemary liver", "كبده روزماري", 342),
        V1Item("Main dishes", "Roz me'ammar (rice casserole)", "ارز معمر", 480),
        V1Item("Main dishes", "Roz me'ammar with pigeon", "ارز معمر حمام", 425),
        V1Item("Main dishes", "Roz me'ammar (plain)", "ارز معمر ساده", 240),
        V1Item("Main dishes", "Escalope pané", "اسكال بانيه", 540),
        V1Item("Main dishes", "Grilled sea bream", "سمك قروص مشوي", 540),
        V1Item("Main dishes", "Bayad fish", "سمك قشر بياض", 360),
        V1Item("Main dishes", "Freekeh tagine with meat", "طاجن فريك باللحمة", 510),
        V1Item("Vegetable dishes", "Molokhia", "ملوخية", 81),
        V1Item("Vegetable dishes", "Moussaka tagine", "طاجن مسقعة", 81),
        V1Item("Vegetable dishes", "Okra tagine (plain)", "طاجن بامية سادة", 120),
        V1Item("Vegetable dishes", "Moussaka tagine with meat", "طاجن مسقعة باللحمة", 243),
        V1Item("Country (Reefi)", "Half duck", "نصف بط", 516),
        V1Item("Country (Reefi)", "Pair of pigeons", "جوز حمام", 540),
        V1Item("Country (Reefi)", "Baladi chicken", "فراخ بلدي", 510),
        V1Item("Pasta", "Pasta tomato sauce", "باستا صوص طماطم", 150),
        V1Item("Pasta", "Pasta basil", "باستا ريحان", 280),
        V1Item("Pasta", "Pasta bolognese", "باستا بولونيز", 280),
        V1Item("Pasta", "Pasta chicken & mushroom sauce", "باستا صوص فراخ ومشروم", 280),
        V1Item("Pasta", "Pasta with vegetables", "باستا بالخضار", 150),
        V1Item("Pizza", "Chicken pizza", "بتزا فراخ", 300),
        V1Item("Pizza", "Shrimp pizza", "بتزا جمبري", 375),
        V1Item("Pizza", "Salami pizza", "بتزا سلامي", 350),
        V1Item("Pizza", "Vegetable pizza", "بتزا خضار", 225),
        V1Item("Pizza", "Margherita pizza", "بتزا مارجريتا", 200),
        V1Item("Pizza", "Mixed cheese pizza", "بتزا مكس جبن", 300),
        V1Item("Pizza", "Sausage pizza", "بتزا سجق", 350),
        V1Item("Feteer", "Sausage feteer", "فطيرة سجق", 375),
        V1Item("Feteer", "Mixed cheese feteer", "فطيرة ميكس جبن", 325),
        V1Item("Feteer", "Basterma feteer", "فطيرة بسطرمة", 320),
        V1Item("Feteer", "Chicken feteer", "فطيرة فراخ", 320),
        V1Item("Feteer", "Nutella feteer", "فطيرة نوتيلا", 250),
        V1Item("Feteer", "Sugar & milk feteer", "فطيرة سكر ولبن", 150),
        V1Item("Sandwiches", "Melted cheese sandwich", "سندوتش جبنه سايحه", 138),
        V1Item("Sandwiches", "Cheese & tomato sandwich", "سندوتش جبنه بالطماطم", 86),
        V1Item("Sandwiches", "Chicken strips sandwich", "سندوتش استربس فراخ", 144),
        V1Item("Sandwiches", "Burger sandwich", "سندوتش بورجر", 431),
        V1Item("Sandwiches", "Shish tawook sandwich", "سندوتش شيش طاووق", 164),
        V1Item("Sandwiches", "Kofta sandwich", "سندوتش كفته", 249),
        V1Item("Sides", "Sautéed vegetables", "خضار سوتية", 60),
        V1Item("Sides", "Grilled vegetables", "خضار مشوي", 72),
        V1Item("Sides", "French fries", "بطاطس مقلية", 78),
        V1Item("Sides", "White rice", "ارز ابيض", 54),
        V1Item("Desserts", "Om Ali", "ام علي", 150),
        V1Item("Desserts", "Nutella crepe", "كريب نوتيلا", 144),
        V1Item("Desserts", "Rice pudding", "ارز باللبن", 90),
        V1Item("Desserts", "Ice cream", "ايس كريم", 125),
        V1Item("Desserts", "Marshmallow", "مارشميللو", 110),
        V1Item("Desserts", "Sweet feteer", "فطير", 231),
        V1Item("Hot drinks", "Turkish coffee", "قهوة تركي", 66),
        V1Item("Hot drinks", "Double Turkish coffee", "قهوة تركي دبل", 72),
        V1Item("Hot drinks", "Espresso", "اسبريسو", 75),
        V1Item("Hot drinks", "Double espresso", "دبل اسبريسو", 102),
        V1Item("Hot drinks", "Nescafé", "نسكافيه", 96),
        V1Item("Hot drinks", "Latte", "لاتيه", 110),
        V1Item("Hot drinks", "Hot chocolate", "هوت شوكولاته", 84),
        V1Item("Hot drinks", "Hot cider", "هوت سيدر", 57),
        V1Item("Hot drinks", "French coffee", "قهوة فرنسي", 84),
        V1Item("Hot drinks", "Hazelnut coffee", "قهوة بندق", 90),
        V1Item("Hot drinks", "Macchiato", "مكياتو", 80),
        V1Item("Hot drinks", "Cortado", "كورتادو", 100),
        V1Item("Hot drinks", "Flat white", "فلات وايت", 125),
        V1Item("Tea & herbs", "Tea / green tea / anise / karkadeh", "شاي احمر - اخضر - ينسون - كركديه", 42),
        V1Item("Tea & herbs", "Tea with milk", "شاي بالحليب", 50),
        V1Item("Water & soft drinks", "Mineral water small", "مياه معدنيه صغيره", 26),
        V1Item("Water & soft drinks", "Mineral water large", "مياه معدنيه كبيره", 38),
        V1Item("Water & soft drinks", "Pepsi / 7UP / Mirinda", "بيبسي - سفن - ميرندا", 46),
        V1Item("Water & soft drinks", "Red Bull", "ريدبول", 96),
        V1Item("Cold drinks", "Lemon mint", "ليمون نعناع", 65),
        V1Item("Cold drinks", "Fresh juice", "عصير فريش", 117),
        V1Item("Cold drinks", "Iced coffee", "ايس كوفي", 133),
        V1Item("Cold drinks", "Iced latte", "ايس لاتيه", 133),
        V1Item("Cold drinks", "Spanish latte", "اسبانش لاتيه", 140),
        V1Item("Cold drinks", "Iced chocolate", "ايس شوكلت", 133),
        V1Item("Cold drinks", "Iced tea", "ايس تي", 70),
        V1Item("Cold drinks", "Frappuccino", "فرابتشينو", 152),
        V1Item("Cold drinks", "Frappe", "فرابيه", 125),
        V1Item("Cold drinks", "Milkshake", "ميلك شيك", 190),
        V1Item("Mocktails", "Classic mojito", "موهيتو كلاسيك", 76),
        V1Item("Mocktails", "Mojito of your choice", "موهيتو من اختيارك", 100),
        V1Item("Mocktails", "Smoothie of your choice", "اسموزي من اختيارك", 100),
        V1Item("Mocktails", "Passion fruit", "باشيون فروت", 76),
        V1Item("Mocktails", "Sunshine", "صن شاين", 76),
        V1Item("Mocktails", "Sunrise", "صن رايس", 114),
        V1Item("Mocktails", "Cherry cola", "شيري كولا", 95),
        V1Item("Mocktails", "Piña colada", "بينا كولادا", 133),
        V1Item("Mocktails", "Birell", "بيريل", 57),
        V1Item("Shisha", "Fruit shisha", "شيشه فواكه", 138),
        V1Item("Shisha", "Plain shisha (salloum)", "شيشه سلوم", 96),
    )

    /** A dish on the menu the app uses now, and where its price came from. */
    data class Current(val category: String, val nameEn: String, val nameAr: String, val price: Double,
                       /** The same dish's name on the October menu, if it's there. */ val octoberName: String?) {
        val repriced get() = octoberName != null
    }

    /**
     * Dishes the restaurant charged at a higher price than the first menu (receipt of 22 Oct 2026).
     * Keyed by English name; also covers October-menu names a phone may still carry for the same dish.
     */
    val RECEIPT_PRICES: Map<String, Double> = mapOf(
        "Pasta tomato sauce" to 180.0,
        "Pasta bolognese" to 335.0,
        "Pasta chicken & mushroom sauce" to 335.0,
        "Penne Alfredo" to 335.0,
        "Fattoush" to 125.0,
        "Chicken soup" to 165.0,
        "Vine leaves" to 110.0,
        "Chicken strips" to 340.0,
        "Mombar" to 165.0,
        "Spicy potatoes" to 90.0,
        "Grilled vegetables" to 75.0,
        "Sautéed vegetables" to 75.0,
        "Pepsi / 7UP / Mirinda" to 50.0,
        "Pepsi Diet" to 50.0,
    )
    private val receiptByKey by lazy { RECEIPT_PRICES.mapKeys { MenuMerge.key(it.key) } }
    fun receiptPrice(nameEn: String): Double? = receiptByKey[MenuMerge.key(nameEn)]

    /** The first menu with its original prices, except where the receipt shows a higher one. [Current.octoberName] lets phones that had the October menu keep their orders. */
    val current: List<Current> by lazy {
        val octByV1 = entries.filter { it.was != null }.associateBy { it.was!! }
        V1_MENU.map { v ->
            Current(v.category, v.nameEn, v.nameAr, receiptPrice(v.nameEn) ?: v.oldPrice.toDouble(), octByV1[v.nameEn]?.nameEn)
        }
    }

    /** Version-1 dish names (English) and their Arabic names, as first shipped. */
    val V1: Map<String, String> = linkedMapOf(
        "Baladi salad" to "سلطة بلدي",
        "Pickled eggplant" to "باذنجان مخلل",
        "Tabbouleh" to "تبولة",
        "Rocket salad" to "سلطة جرجير",
        "Tahini" to "طحينة",
        "Fattoush" to "فتوش",
        "Pickles" to "مخلل",
        "Baba ghanoush" to "بابا غنوج",
        "Sambousek" to "سمبوسك",
        "Chicken strips" to "ستربس فراخ",
        "Shrimp with spinach" to "سبانخ جمبري",
        "Mombar" to "ممبار",
        "Sausage (sogo')" to "سجق",
        "Alexandrian liver" to "كبدة اسكندراني",
        "Beef mint" to "بيف منت",
        "Spicy potatoes" to "بطاطس حارة",
        "Hummus fatteh" to "فتة حمص",
        "Vine leaves" to "ورق عنب",
        "Kobeba" to "كوبيبه",
        "Vegetable soup" to "شوربه خضار",
        "Mushroom soup" to "شوربه مشروم",
        "Tomato soup" to "شوربه طماطم",
        "Chicken soup" to "شوربه فراخ",
        "Fillet with mushroom" to "فليه مشروم",
        "Freekeh tagine (plain)" to "طاجن فريك سادة",
        "Grilled kofta" to "كفته مشوية",
        "Half deboned chicken" to "نصف فراخ مسحب",
        "Shish tawook" to "شيش طاووق",
        "Mixed grill" to "مكس جريل",
        "Veal kebab" to "كباب بتلو",
        "Veal ribs" to "رايش بتلو",
        "Rosemary liver" to "كبده روزماري",
        "Roz me'ammar (rice casserole)" to "ارز معمر",
        "Roz me'ammar with pigeon" to "ارز معمر حمام",
        "Roz me'ammar (plain)" to "ارز معمر ساده",
        "Escalope pané" to "اسكال بانيه",
        "Grilled sea bream" to "سمك قروص مشوي",
        "Bayad fish" to "سمك قشر بياض",
        "Freekeh tagine with meat" to "طاجن فريك باللحمة",
        "Molokhia" to "ملوخية",
        "Moussaka tagine" to "طاجن مسقعة",
        "Okra tagine (plain)" to "طاجن بامية سادة",
        "Moussaka tagine with meat" to "طاجن مسقعة باللحمة",
        "Half duck" to "نصف بط",
        "Pair of pigeons" to "جوز حمام",
        "Baladi chicken" to "فراخ بلدي",
        "Pasta tomato sauce" to "باستا صوص طماطم",
        "Pasta basil" to "باستا ريحان",
        "Pasta bolognese" to "باستا بولونيز",
        "Pasta chicken & mushroom sauce" to "باستا صوص فراخ ومشروم",
        "Pasta with vegetables" to "باستا بالخضار",
        "Chicken pizza" to "بتزا فراخ",
        "Shrimp pizza" to "بتزا جمبري",
        "Salami pizza" to "بتزا سلامي",
        "Vegetable pizza" to "بتزا خضار",
        "Margherita pizza" to "بتزا مارجريتا",
        "Mixed cheese pizza" to "بتزا مكس جبن",
        "Sausage pizza" to "بتزا سجق",
        "Sausage feteer" to "فطيرة سجق",
        "Mixed cheese feteer" to "فطيرة ميكس جبن",
        "Basterma feteer" to "فطيرة بسطرمة",
        "Chicken feteer" to "فطيرة فراخ",
        "Nutella feteer" to "فطيرة نوتيلا",
        "Sugar & milk feteer" to "فطيرة سكر ولبن",
        "Melted cheese sandwich" to "سندوتش جبنه سايحه",
        "Cheese & tomato sandwich" to "سندوتش جبنه بالطماطم",
        "Chicken strips sandwich" to "سندوتش استربس فراخ",
        "Burger sandwich" to "سندوتش بورجر",
        "Shish tawook sandwich" to "سندوتش شيش طاووق",
        "Kofta sandwich" to "سندوتش كفته",
        "Sautéed vegetables" to "خضار سوتية",
        "Grilled vegetables" to "خضار مشوي",
        "French fries" to "بطاطس مقلية",
        "White rice" to "ارز ابيض",
        "Om Ali" to "ام علي",
        "Nutella crepe" to "كريب نوتيلا",
        "Rice pudding" to "ارز باللبن",
        "Ice cream" to "ايس كريم",
        "Marshmallow" to "مارشميللو",
        "Sweet feteer" to "فطير",
        "Turkish coffee" to "قهوة تركي",
        "Double Turkish coffee" to "قهوة تركي دبل",
        "Espresso" to "اسبريسو",
        "Double espresso" to "دبل اسبريسو",
        "Nescafé" to "نسكافيه",
        "Latte" to "لاتيه",
        "Hot chocolate" to "هوت شوكولاته",
        "Hot cider" to "هوت سيدر",
        "French coffee" to "قهوة فرنسي",
        "Hazelnut coffee" to "قهوة بندق",
        "Macchiato" to "مكياتو",
        "Cortado" to "كورتادو",
        "Flat white" to "فلات وايت",
        "Tea / green tea / anise / karkadeh" to "شاي احمر - اخضر - ينسون - كركديه",
        "Tea with milk" to "شاي بالحليب",
        "Mineral water small" to "مياه معدنيه صغيره",
        "Mineral water large" to "مياه معدنيه كبيره",
        "Pepsi / 7UP / Mirinda" to "بيبسي - سفن - ميرندا",
        "Red Bull" to "ريدبول",
        "Lemon mint" to "ليمون نعناع",
        "Fresh juice" to "عصير فريش",
        "Iced coffee" to "ايس كوفي",
        "Iced latte" to "ايس لاتيه",
        "Spanish latte" to "اسبانش لاتيه",
        "Iced chocolate" to "ايس شوكلت",
        "Iced tea" to "ايس تي",
        "Frappuccino" to "فرابتشينو",
        "Frappe" to "فرابيه",
        "Milkshake" to "ميلك شيك",
        "Classic mojito" to "موهيتو كلاسيك",
        "Mojito of your choice" to "موهيتو من اختيارك",
        "Smoothie of your choice" to "اسموزي من اختيارك",
        "Passion fruit" to "باشيون فروت",
        "Sunshine" to "صن شاين",
        "Sunrise" to "صن رايس",
        "Cherry cola" to "شيري كولا",
        "Piña colada" to "بينا كولادا",
        "Birell" to "بيريل",
        "Fruit shisha" to "شيشه فواكه",
        "Plain shisha (salloum)" to "شيشه سلوم",
    )

    /** A dish on the current menu. [was] = its version-1 name, if it replaces one. */
    data class Entry(val category: String, val nameEn: String, val price: Double, val was: String?, val nameAr: String)

    private class B {
        val list = mutableListOf<Entry>()
        var cat = ""
        /** Same dish as before (keeps its Arabic name). */
        fun same(en: String, price: Int, was: String) {
            list += Entry(cat, en, price.toDouble(), was, V1[was] ?: error("unknown v1 dish $was"))
        }
        /** Replaces a version-1 dish, but the dish changed, so the old Arabic name is dropped. */
        fun renamed(en: String, price: Int, was: String) {
            require(was in V1) { "unknown v1 dish $was" }
            list += Entry(cat, en, price.toDouble(), was, "")
        }
        /** New on this menu. */
        fun new(en: String, price: Int) { list += Entry(cat, en, price.toDouble(), null, "") }
    }

    val entries: List<Entry> = B().apply {
        cat = "Feteer"
        same("Sausage Pie", 450, "Sausage feteer")
        same("Mixed Cheese Pie", 400, "Mixed cheese feteer")
        same("Pastrami Pie", 390, "Basterma feteer")
        same("Chicken Pie", 390, "Chicken feteer")
        same("Nutella Pie", 300, "Nutella feteer")
        same("Sugar & Milk Pie", 180, "Sugar & milk feteer")
        new("Lotus Pie", 423)
        new("Cream & Honey Pie", 450)
        new("Mixed Meat Pie", 423)

        cat = "Sandwiches"
        same("White Cheese & Tomato Sandwich", 105, "Cheese & tomato sandwich")
        renamed("White Cheese Sandwich", 165, "Melted cheese sandwich")
        same("Chicken Strips Sandwich", 225, "Chicken strips sandwich")
        renamed("Cheese Burger (With Fries)", 520, "Burger sandwich")
        same("Sheesh Tawook Sandwich", 255, "Shish tawook sandwich")
        same("Kofta Sandwich", 345, "Kofta sandwich")

        cat = "Pizza"
        same("Salami Pizza", 670, "Salami pizza")
        new("Tuna Pizza", 670)
        new("Pepperoni Pizza", 575)
        same("Vegetable Pizza", 590, "Vegetable pizza")
        same("Margherita Pizza", 685, "Margherita pizza")
        renamed("Quattro Fromaggi Pizza", 705, "Mixed cheese pizza")
        new("Smoked Turkey Pizza", 645)
        new("Mushroom Pizza", 810)

        cat = "Salads"
        same("Baladi Salad", 50, "Baladi salad")
        same("Marinated Eggplant Salad", 55, "Pickled eggplant")
        same("Tabbouleh Salad", 95, "Tabbouleh")
        same("Arugula Salad", 140, "Rocket salad")
        same("Tehina Salad", 85, "Tahini")
        same("Pickle Salad", 35, "Pickles")
        same("Fatoush Salad", 125, "Fattoush")
        same("Baba Ghanoug", 80, "Baba ghanoush")

        cat = "Appetizers"
        renamed("Cheese Sambosak", 180, "Sambousek")
        same("Breaded Chicken Strips", 340, "Chicken strips")
        same("Oven Baked Shrimps and Spinach", 410, "Shrimp with spinach")
        same("Mombar", 165, "Mombar")
        same("Oriental Sausage (150g)", 305, "Sausage (sogo')")
        same("Alexandria Liver (180g)", 280, "Alexandrian liver")
        same("Beef Mint", 305, "Beef mint")
        same("Spicy Diced Potatoes", 90, "Spicy potatoes")
        same("Hummus Fattah", 255, "Hummus fatteh")
        same("Kobeiba", 340, "Kobeba")
        same("Stuffed Vine Leaves", 110, "Vine leaves")

        cat = "Soup"
        same("Vegetable Soup", 105, "Vegetable soup")
        same("Mushroom Soup", 150, "Mushroom soup")
        same("Tomato Soup", 120, "Tomato soup")
        same("Chicken Soup", 165, "Chicken soup")

        cat = "Pasta"
        same("Penne Red Sauce", 180, "Pasta tomato sauce")
        same("Penne Pesto", 335, "Pasta basil")
        new("Penne Alfredo", 335)
        same("Spaghetti Bolognaise", 335, "Pasta bolognese")
        same("Vegetable Pasta", 215, "Pasta with vegetables")

        cat = "Sides"
        same("Sautéed Vegetables", 75, "Sautéed vegetables")
        same("Grilled Vegetables", 90, "Grilled vegetables")
        same("French Fries", 95, "French fries")
        same("White Rice", 70, "White rice")

        cat = "Main Course"
        renamed("Fillet with Rice", 650, "Fillet with mushroom")
        same("Grilled Kofta", 435, "Grilled kofta")
        renamed("BBQ Half Chicken", 455, "Half deboned chicken")
        same("Grilled Chicken Shish Tawook", 485, "Shish tawook")
        same("Veal Kebab", 870, "Veal kebab")
        same("Veal Chops", 935, "Veal ribs")
        same("Liver with Rosemary", 410, "Rosemary liver")
        same("Mix Grilled (Fries, Sauté)", 990, "Mixed grill")
        same("Oven Baked Rice & Milk (Pigeon)", 580, "Roz me'ammar with pigeon")
        same("Oven Baked Rice & Milk (Meat)", 615, "Roz me'ammar (rice casserole)")
        same("Oven Baked Rice & Milk", 290, "Roz me'ammar (plain)")
        same("Scallop Pane", 650, "Escalope pané")
        same("Grilled Sea Bass", 650, "Grilled sea bream")
        same("Nile Perch Steak", 435, "Bayad fish")

        cat = "Farm Dishes"
        same("Roasted Half Duck with Rice", 620, "Half duck")
        renamed("Stuffed Pigeon (2 pieces) with Fries", 650, "Pair of pigeons")
        renamed("Fried Farm Whole Chicken", 615, "Baladi chicken")
        same("Molokheya", 100, "Molokhia")
        same("Musaqaa", 100, "Moussaka tagine")
        same("Musaqaa (with meat)", 295, "Moussaka tagine with meat")
        same("Okra", 145, "Okra tagine (plain)")

        cat = "Dessert"
        same("Om Aly (with mixed nuts)", 180, "Om Ali")
        same("Crepe Nutella", 175, "Nutella crepe")
        same("Rice & Milk Pudding", 210, "Rice pudding")
        same("Ice Cream (one scoop)", 150, "Ice cream")

        cat = "Hot Drinks"
        same("Turkish Coffee", 80, "Turkish coffee")
        same("Turkish Coffee (Double)", 85, "Double Turkish coffee")
        same("Espresso", 90, "Espresso")
        same("Double Espresso", 125, "Double espresso")
        same("Nescafe", 115, "Nescafé")
        renamed("Latte / Cappuccino", 135, "Latte")
        same("Hot Chocolate", 100, "Hot chocolate")
        same("Hot Cider", 70, "Hot cider")
        same("French Coffee", 100, "French coffee")
        same("Hazelnut Coffee", 110, "Hazelnut coffee")
        same("Macchiato", 95, "Macchiato")
        same("Cortado", 120, "Cortado")
        same("Flat White", 150, "Flat white")
        new("Spanish Latte", 150)

        cat = "Cold Drinks"
        renamed("Lemon Juice", 80, "Lemon mint")
        same("Fresh Juice", 140, "Fresh juice")
        same("Iced Coffee", 160, "Iced coffee")
        same("Iced Latte", 160, "Iced latte")
        same("Iced Spanish", 170, "Spanish latte")
        same("Iced Chocolate", 160, "Iced chocolate")
        same("Iced Tea", 85, "Iced tea")
        same("Frappuccino", 185, "Frappuccino")
        same("Frappe", 150, "Frappe")
        same("Milkshake", 230, "Milkshake")

        cat = "Mocktails"
        same("Classic Mojito", 90, "Classic mojito")
        same("Mojito of your choice", 120, "Mojito of your choice")
        same("Smoothie of your choice", 120, "Smoothie of your choice")
        // No price is printed for this one on the new menu — shown as "Price not set" until it's entered.
        renamed("Passion Fruit Strawberry / Mixed Berries", 0, "Passion fruit")
        same("SunShine", 90, "Sunshine")
        same("Sunrise", 135, "Sunrise")
        same("Cherry Cola", 115, "Cherry cola")
        same("Pina Colada", 160, "Piña colada")
        same("Beril", 70, "Birell")

        cat = "Shisha"
        same("Shisha Fakher", 165, "Fruit shisha")
        same("Shisha Salloum", 115, "Plain shisha (salloum)")

        cat = "Soft Drinks & Water"
        same("Small Mineral Water", 35, "Mineral water small")
        same("Large Mineral Water", 45, "Mineral water large")
        renamed("Pepsi / 7UP / Fanta", 55, "Pepsi / 7UP / Mirinda")
        new("Pepsi Diet", 60)
        same("Red Bull", 115, "Red Bull")

        cat = "Tea & Herbal Infusions"
        renamed("Tea / Anise / Mint / Hibiscus / Green Tea", 60, "Tea / green tea / anise / karkadeh")
        same("Tea With Milk", 60, "Tea with milk")
    }.list

    const val NOTE = "Agri Lodge. Menu says 12% service and 14% taxes are added to the final bill."

    fun place(): Place = Place(
        name = NAME,
        nameAr = "كوم الدكة",
        type = PlaceType.RESTAURANT,
        servicePct = 12.0,
        taxPct = 14.0,
        note = NOTE,
        menuVersion = VERSION,
        items = current.map { MenuItem(category = it.category, nameEn = it.nameEn, nameAr = it.nameAr, price = it.price) },
    )

    /**
     * Brings an existing trip's Kom El Dikka menu up to date. Safe to run repeatedly.
     * - A dish that replaces an old one keeps the old item's id (so orders follow it) and gets the new name/price.
     * - Old dishes that left the menu are removed, unless someone already ordered them (then they stay,
     *   under "No longer on the menu", so totals don't change).
     * - Items the user added themselves are left alone.
     */
    fun upgrade(trip: Trip): Trip {
        val idx = trip.places.indexOfFirst { it.name.trim().equals(NAME, ignoreCase = true) && it.menuVersion < VERSION }
        if (idx < 0) return trip
        val old = trip.places[idx]
        if (old.menuVersion == 3) {
            // Already on the first menu: only reprice the dishes the receipt showed, keep everything else as it is.
            val place = old.copy(menuVersion = VERSION, items = old.items.map { item ->
                receiptPrice(item.nameEn)?.let { item.copy(price = it) } ?: item
            })
            return trip.copy(places = trip.places.toMutableList().also { it[idx] = place })
        }
        val ordered = trip.orders.filter { it.placeId == old.id }.map { it.itemId }.toSet()
        val byName = old.items.associateBy { MenuMerge.key(it.nameEn) }
        val used = mutableSetOf<String>()

        // Each dish keeps its existing item (and so its orders), whether the phone had the first or the October names.
        val updated = current.map { e ->
            val match = listOfNotNull(e.nameEn, e.octoberName).firstNotNullOfOrNull { byName[MenuMerge.key(it)] }
                ?.takeIf { it.id !in used }
            if (match != null) {
                used += match.id
                match.copy(category = e.category, nameEn = e.nameEn, nameAr = e.nameAr, price = e.price)
            } else MenuItem(category = e.category, nameEn = e.nameEn, nameAr = e.nameAr, price = e.price)
        }
        val builtIn = (V1.keys + entries.map { it.nameEn }).map(MenuMerge::key).toSet()
        val leftovers = old.items.filter { it.id !in used }.mapNotNull { item ->
            when {
                MenuMerge.key(item.nameEn) !in builtIn -> item                     // user's own item
                item.id in ordered -> item.copy(category = "No longer on the menu",  // keep for existing orders
                    price = receiptPrice(item.nameEn) ?: item.price)
                else -> null
            }
        }
        val place = old.copy(
            items = updated + leftovers,
            menuVersion = VERSION,
            note = if (old.note.isBlank() || old.note.startsWith("Agri Lodge")) NOTE else old.note,
        )
        return trip.copy(places = trip.places.toMutableList().also { it[idx] = place })
    }
}
