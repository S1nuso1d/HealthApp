package com.example.healtapp.features.auth.presentation

data class DietaryExclusionOption(
    val id: String,
    val label: String,
    val hint: String,
)

object DietaryExclusionCatalog {
    val religious = listOf(
        DietaryExclusionOption("pork", "Без свинины", "Халяль, иудаизм и другие традиции"),
        DietaryExclusionOption("beef", "Без говядины", "Индуизм и личные убеждения"),
        DietaryExclusionOption("alcohol", "Без алкоголя", "Религиозные и этические ограничения"),
        DietaryExclusionOption("halal", "Только халяль", "Учитываем при рекомендациях блюд"),
        DietaryExclusionOption("kosher", "Кошерное", "Соответствие кашруту"),
        DietaryExclusionOption("fasting", "Пост / дни ограничений", "Учитываем в календаре питания"),
    )

    val ethical = listOf(
        DietaryExclusionOption("vegan", "Веганство", "Без продуктов животного происхождения"),
        DietaryExclusionOption("vegetarian", "Вегетарианство", "Без мяса и рыбы"),
        DietaryExclusionOption("seafood", "Без морепродуктов", "Рыба, моллюски, креветки"),
    )

    val other = listOf(
        DietaryExclusionOption("gluten", "Без глютена", "Пшеница, рожь и др."),
        DietaryExclusionOption("lactose", "Без лактозы", "Молочные продукты"),
        DietaryExclusionOption("eggs", "Без яиц", "Яйца и блюда с ними"),
    )

    val all: List<DietaryExclusionOption> = religious + ethical + other

    fun labelFor(id: String): String = all.firstOrNull { it.id == id }?.label ?: id
}
