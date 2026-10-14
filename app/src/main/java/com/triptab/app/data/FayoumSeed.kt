package com.triptab.app.data

/**
 * Starting places for the Fayoum trip (22–24 Oct 2026).
 * Kom El Dikka: transcribed from its PDF menu (prices in EGP; menu states +12% service, +14% taxes).
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

    val komElDikka = Place(
        name = "Kom El Dikka",
        nameAr = "كوم الدكة",
        type = PlaceType.RESTAURANT,
        servicePct = 12.0,
        taxPct = 14.0,
        note = "Agri Lodge. Menu says 12% service and 14% taxes are added to the final bill.",
        items = menu {
            cat("Salads")
            i("Baladi salad", "سلطة بلدي", 42)
            i("Pickled eggplant", "باذنجان مخلل", 44)
            i("Tabbouleh", "تبولة", 78)
            i("Rocket salad", "سلطة جرجير", 114)
            i("Tahini", "طحينة", 72)
            i("Fattoush", "فتوش", 104)
            i("Pickles", "مخلل", 25)
            i("Baba ghanoush", "بابا غنوج", 66)

            cat("Appetizers")
            i("Sambousek", "سمبوسك", 150)
            i("Chicken strips", "ستربس فراخ", 282)
            i("Shrimp with spinach", "سبانخ جمبري", 342)
            i("Mombar", "ممبار", 138)
            i("Sausage (sogo')", "سجق", 252)
            i("Alexandrian liver", "كبدة اسكندراني", 234)
            i("Beef mint", "بيف منت", 252)
            i("Spicy potatoes", "بطاطس حارة", 72)
            i("Hummus fatteh", "فتة حمص", 210)
            i("Vine leaves", "ورق عنب", 90)
            i("Kobeba", "كوبيبه", 282)

            cat("Soups")
            i("Vegetable soup", "شوربه خضار", 90)
            i("Mushroom soup", "شوربه مشروم", 125)
            i("Tomato soup", "شوربه طماطم", 100)
            i("Chicken soup", "شوربه فراخ", 140)

            cat("Main dishes")
            i("Fillet with mushroom", "فليه مشروم", 540)
            i("Freekeh tagine (plain)", "طاجن فريك سادة", 300)
            i("Grilled kofta", "كفته مشوية", 360)
            i("Half deboned chicken", "نصف فراخ مسحب", 378)
            i("Shish tawook", "شيش طاووق", 371)
            i("Mixed grill", "مكس جريل", 822)
            i("Veal kebab", "كباب بتلو", 720)
            i("Veal ribs", "رايش بتلو", 780)
            i("Rosemary liver", "كبده روزماري", 342)
            i("Roz me'ammar (rice casserole)", "ارز معمر", 480)
            i("Roz me'ammar with pigeon", "ارز معمر حمام", 425)
            i("Roz me'ammar (plain)", "ارز معمر ساده", 240)
            i("Escalope pané", "اسكال بانيه", 540)
            i("Grilled sea bream", "سمك قروص مشوي", 540)
            i("Bayad fish", "سمك قشر بياض", 360)
            i("Freekeh tagine with meat", "طاجن فريك باللحمة", 510)

            cat("Vegetable dishes")
            i("Molokhia", "ملوخية", 81)
            i("Moussaka tagine", "طاجن مسقعة", 81)
            i("Okra tagine (plain)", "طاجن بامية سادة", 120)
            i("Moussaka tagine with meat", "طاجن مسقعة باللحمة", 243)

            cat("Country (Reefi)")
            i("Half duck", "نصف بط", 516)
            i("Pair of pigeons", "جوز حمام", 540)
            i("Baladi chicken", "فراخ بلدي", 510)

            cat("Pasta")
            i("Pasta tomato sauce", "باستا صوص طماطم", 150)
            i("Pasta basil", "باستا ريحان", 280)
            i("Pasta bolognese", "باستا بولونيز", 280)
            i("Pasta chicken & mushroom sauce", "باستا صوص فراخ ومشروم", 280)
            i("Pasta with vegetables", "باستا بالخضار", 150)

            cat("Pizza")
            i("Chicken pizza", "بتزا فراخ", 300)
            i("Shrimp pizza", "بتزا جمبري", 375)
            i("Salami pizza", "بتزا سلامي", 350)
            i("Vegetable pizza", "بتزا خضار", 225)
            i("Margherita pizza", "بتزا مارجريتا", 200)
            i("Mixed cheese pizza", "بتزا مكس جبن", 300)
            i("Sausage pizza", "بتزا سجق", 350)

            cat("Feteer")
            i("Sausage feteer", "فطيرة سجق", 375)
            i("Mixed cheese feteer", "فطيرة ميكس جبن", 325)
            i("Basterma feteer", "فطيرة بسطرمة", 320)
            i("Chicken feteer", "فطيرة فراخ", 320)
            i("Nutella feteer", "فطيرة نوتيلا", 250)
            i("Sugar & milk feteer", "فطيرة سكر ولبن", 150)

            cat("Sandwiches")
            i("Melted cheese sandwich", "سندوتش جبنه سايحه", 138)
            i("Cheese & tomato sandwich", "سندوتش جبنه بالطماطم", 86)
            i("Chicken strips sandwich", "سندوتش استربس فراخ", 144)
            i("Burger sandwich", "سندوتش بورجر", 431)
            i("Shish tawook sandwich", "سندوتش شيش طاووق", 164)
            i("Kofta sandwich", "سندوتش كفته", 249)

            cat("Sides")
            i("Sautéed vegetables", "خضار سوتية", 60)
            i("Grilled vegetables", "خضار مشوي", 72)
            i("French fries", "بطاطس مقلية", 78)
            i("White rice", "ارز ابيض", 54)

            cat("Desserts")
            i("Om Ali", "ام علي", 150)
            i("Nutella crepe", "كريب نوتيلا", 144)
            i("Rice pudding", "ارز باللبن", 90)
            i("Ice cream", "ايس كريم", 125)
            i("Marshmallow", "مارشميللو", 110)
            i("Sweet feteer", "فطير", 231)

            cat("Hot drinks")
            i("Turkish coffee", "قهوة تركي", 66)
            i("Double Turkish coffee", "قهوة تركي دبل", 72)
            i("Espresso", "اسبريسو", 75)
            i("Double espresso", "دبل اسبريسو", 102)
            i("Nescafé", "نسكافيه", 96)
            i("Latte", "لاتيه", 110)
            i("Hot chocolate", "هوت شوكولاته", 84)
            i("Hot cider", "هوت سيدر", 57)
            i("French coffee", "قهوة فرنسي", 84)
            i("Hazelnut coffee", "قهوة بندق", 90)
            i("Macchiato", "مكياتو", 80)
            i("Cortado", "كورتادو", 100)
            i("Flat white", "فلات وايت", 125)

            cat("Tea & herbs")
            i("Tea / green tea / anise / karkadeh", "شاي احمر - اخضر - ينسون - كركديه", 42)
            i("Tea with milk", "شاي بالحليب", 50)

            cat("Water & soft drinks")
            i("Mineral water small", "مياه معدنيه صغيره", 26)
            i("Mineral water large", "مياه معدنيه كبيره", 38)
            i("Pepsi / 7UP / Mirinda", "بيبسي - سفن - ميرندا", 46)
            i("Red Bull", "ريدبول", 96)

            cat("Cold drinks")
            i("Lemon mint", "ليمون نعناع", 65)
            i("Fresh juice", "عصير فريش", 117)
            i("Iced coffee", "ايس كوفي", 133)
            i("Iced latte", "ايس لاتيه", 133)
            i("Spanish latte", "اسبانش لاتيه", 140)
            i("Iced chocolate", "ايس شوكلت", 133)
            i("Iced tea", "ايس تي", 70)
            i("Frappuccino", "فرابتشينو", 152)
            i("Frappe", "فرابيه", 125)
            i("Milkshake", "ميلك شيك", 190)

            cat("Mocktails")
            i("Classic mojito", "موهيتو كلاسيك", 76)
            i("Mojito of your choice", "موهيتو من اختيارك", 100)
            i("Smoothie of your choice", "اسموزي من اختيارك", 100)
            i("Passion fruit", "باشيون فروت", 76)
            i("Sunshine", "صن شاين", 76)
            i("Sunrise", "صن رايس", 114)
            i("Cherry cola", "شيري كولا", 95)
            i("Piña colada", "بينا كولادا", 133)
            i("Birell", "بيريل", 57)

            cat("Shisha")
            i("Fruit shisha", "شيشه فواكه", 138)
            i("Plain shisha (salloum)", "شيشه سلوم", 96)
        },
    )

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
        note = "Menu not added yet — open this place and tap Edit to add items.",
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
