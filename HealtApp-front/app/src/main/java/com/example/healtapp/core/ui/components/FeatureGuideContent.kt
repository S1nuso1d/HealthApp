package com.example.healtapp.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Mic

object FeatureGuideContent {

    val dashboard = listOf(
        FeatureGuidePage(
            title = "Главная — сцена дня",
            lead = "Сверху приветствие и индекс, затем четыре опоры. Фокус дня внизу — один главный пункт и короткий список.",
            bullets = listOf(
                "Плитки показывают, сколько ещё осталось до цели.",
                "Вечером сверху появляется риск сна, плитки становятся спокойнее.",
                "Итоги недели — в «Разборе недели», на главной только в воскресенье.",
            ),
            icon = Icons.Filled.MonitorHeart,
        ),
        FeatureGuidePage(
            title = "Четыре опоры дня",
            lead = "Сон, вода, питание и движение — основа рекомендаций. Нажмите на карточку, чтобы перейти к записи.",
            bullets = listOf(
                "Сон — часы и качество; цель задаётся в профиле.",
                "Вода — миллилитры за день; можно добавить из виджета.",
                "Питание — калории и кофеин из дневника еды.",
                "Активность — шаги, минуты и сожжённые калории.",
            ),
            icon = Icons.Filled.Favorite,
            miniIcons = listOf(
                Icons.Filled.Bedtime,
                Icons.Filled.WaterDrop,
                Icons.Filled.Restaurant,
                Icons.AutoMirrored.Filled.DirectionsWalk,
            ),
        ),
        FeatureGuidePage(
            title = "Фокус на сегодня",
            lead = "Отметка самочувствия помогает видеть связь со сном и стрессом. На главной один список дел — без повторов одних и тех же советов.",
            bullets = listOf(
                "Кратко отметьте настроение, энергию и стресс — это займёт несколько секунд.",
                "«Фокус на сегодня» объединяет план и советы. Закрытая цель исчезает из списка.",
                "Кнопка «ИИ чат» и «Сообщество» — быстрый доступ к персональному разбору и ленте.",
            ),
            icon = Icons.Filled.AutoAwesome,
        ),
        FeatureGuidePage(
            title = "Цели и неделя",
            lead = "Семь точек — эта неделя. Полный месяц открывается по кнопке. Разбор недели живёт отдельным экраном.",
            bullets = listOf(
                "Зелёная точка — все цели дня выполнены.",
                "«Разбор недели» — средние и сравнение, когда неделя закрыта.",
                "Показатели с часов (Health Connect) дополняют ручной ввод.",
            ),
            icon = Icons.Filled.CalendarMonth,
        ),
    )

    val sleep = listOf(
        FeatureGuidePage(
            title = "Зачем вести сон",
            lead = "Регулярный сон влияет на энергию, аппетит и восстановление. HealthApp считает среднюю длительность и сравнивает с целью из профиля.",
            bullets = listOf(
                "Цель сна настраивается в профиле — «Цели и привычки».",
                "Чем больше ночей в истории, тем точнее график и советы.",
                "Качество сна (0–100) помогает замечать тренды.",
            ),
            icon = Icons.Filled.Bedtime,
        ),
        FeatureGuidePage(
            title = "Карточка и неделя",
            lead = "Вверху — сон за сегодня и среднее за период. График недели показывает, укладываетесь ли вы в цель.",
            bullets = listOf(
                "Синяя линия на графике — ваша цель в часах.",
                "Если данных мало, добавьте первую ночь вручную.",
                "Health Connect может подтянуть сон с телефона или часов.",
            ),
            icon = Icons.Filled.MonitorHeart,
        ),
        FeatureGuidePage(
            title = "Добавить ночь",
            lead = "Ручной ввод нужен, если нет автоматической синхронизации или вы спали без трекера.",
            bullets = listOf(
                "Укажите дату, время засыпания и пробуждения.",
                "Оцените качество — от 0 до 100.",
                "Заметка необязательна: «бессонница», «поздний ужин» и т.п.",
            ),
            icon = Icons.Filled.Timer,
        ),
        FeatureGuidePage(
            title = "Диктофон и ИИ-сводка",
            lead = "Во время сна можно записывать звуки — храп, разговоры. После сессии ИИ составит краткую сводку (нужен запущенный сервер с нейросетью).",
            bullets = listOf(
                "Нажмите «Начать запись» перед сном — нужны разрешения на микрофон.",
                "Остановите утром — клипы сохранятся локально.",
                "«Сгенерировать сводку» — текстовый разбор ночи по звукам.",
            ),
            icon = Icons.Filled.Mic,
        ),
    )

    val activity = listOf(
        FeatureGuidePage(
            title = "Шаги и калории",
            lead = "Раздел «Активность» объединяет шаги за день, сожжённые калории и тренировки. Цель шагов — из профиля.",
            bullets = listOf(
                "Кольцо прогресса показывает, сколько до дневной нормы.",
                "Данные Health Connect дополняют ручной ввод.",
                "Нажмите «Изменить цель», чтобы перейти в профиль.",
            ),
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        ),
        FeatureGuidePage(
            title = "Синхронизация",
            lead = "Подключите Health Connect в профиле или на экране интеграций — шаги и тренировки подтянутся автоматически.",
            bullets = listOf(
                "Кнопка синхронизации шагов — обновить счётчик с телефона.",
                "Отдельно можно импортировать тренировки из Health Connect.",
                "Без разрешений остаётся только ручной ввод.",
            ),
            icon = Icons.Filled.Sync,
        ),
        FeatureGuidePage(
            title = "Тренировки вручную",
            lead = "Запишите прогулку, зал или кардио — так учитываются минуты и калории, которых нет в шагах.",
            bullets = listOf(
                "Выберите тип: ходьба, бег, силовая, йога и др.",
                "Укажите длительность; калории и дистанция — по желанию.",
                "Интенсивность помогает точнее оценить нагрузку.",
            ),
            icon = Icons.Filled.LocalFireDepartment,
        ),
        FeatureGuidePage(
            title = "График недели",
            lead = "Столбцы — шаги по дням. Сравнивайте с целью и ищите «провалы», чтобы планировать прогулки.",
            bullets = listOf(
                "Сегодняшний день обновляется при синхронизации.",
                "История тренировок — в блоке «История» ниже.",
                "Записи можно редактировать и удалять.",
            ),
            icon = Icons.Filled.Flag,
        ),
    )

    val nutrition = listOf(
        FeatureGuidePage(
            title = "Раздел «Питание»",
            lead = "Четыре вкладки: дневник еды, свои блюда, вода и голодание. Всё в одном месте, чтобы не прыгать по экранам.",
            bullets = listOf(
                "«Питание» — записи за день и КБЖУ.",
                "«Мои блюда» — сохранённые рецепты и порции.",
                "«Вода» — быстрые добавления стаканов.",
                "«Голодание» — таймер интервального голодания.",
            ),
            icon = Icons.Filled.Restaurant,
            miniIcons = listOf(
                Icons.Filled.Restaurant,
                Icons.Filled.MenuBook,
                Icons.Filled.WaterDrop,
                Icons.Filled.Timer,
            ),
        ),
        FeatureGuidePage(
            title = "Дневник питания",
            lead = "Записи за день помогают видеть реальные калории и БЖУ. Чем чаще вносите еду, тем точнее цели и рекомендации.",
            bullets = listOf(
                "Фиксируйте всё, что съели и выпили в течение дня.",
                "Не ждите идеальной точности — лучше примерная запись, чем пропуск.",
            ),
            icon = Icons.Filled.Restaurant,
        ),
        FeatureGuidePage(
            title = "Что указывать в записи",
            lead = "Для каждого продукта или блюда укажите название и вес порции — от этого считаются калории и БЖУ.",
            bullets = listOf(
                "Название: «Овсянка на молоке», «Куриная грудка», «Яблоко».",
                "Вес порции в граммах — взвесьте или оцените по упаковке.",
                "При необходимости уточните белки, жиры, углеводы и кофеин.",
            ),
            icon = Icons.Filled.Scale,
        ),
        FeatureGuidePage(
            title = "Масло и соусы",
            lead = "Всё, что добавляете при готовке, нужно учитывать. Масло на сковороде часто забывают — без него калории занижаются.",
            bullets = listOf(
                "Масло, маргарин, соусы — отдельной строкой с весом в граммах.",
                "Если часть масла осталась в сковороде, укажите только съеденное количество.",
            ),
            icon = Icons.Filled.LocalFireDepartment,
        ),
        FeatureGuidePage(
            title = "Как быстро найти продукт",
            lead = "Ищите по названию, штрихкоду или сохранённым блюдам — не обязательно вводить КБЖУ вручную.",
            bullets = listOf(
                "Поиск по названию — для продуктов из базы.",
                "Сканер штрихкода — для упакованных товаров.",
                "«Мои блюда» — для домашних рецептов.",
            ),
            icon = Icons.Filled.Search,
            miniIcons = listOf(Icons.Filled.Search, Icons.Filled.QrCodeScanner, Icons.Filled.MenuBook),
        ),
    )

    val profile = listOf(
        FeatureGuidePage(
            title = "Профиль по вкладкам",
            lead = "Шапка с фото всегда на виду. Формы открываются снизу, а не разворачиваются пачкой на одном экране.",
            bullets = listOf(
                "«Профиль» — цели, рост/вес и строки, которые открывают редактирование.",
                "«Здоровье» — таблетки, цикл и уведомления.",
                "«Ещё» — сообщество, импорт, часы и выход.",
            ),
            icon = Icons.Filled.Person,
        ),
        FeatureGuidePage(
            title = "Питание и аллергии",
            lead = "Укажите вегетарианство и аллергии — ИИ-план питания и советы будут их учитывать.",
            bullets = listOf(
                "Блок «Питание» — аллергены и ограничения.",
                "Цель и уровень активности — для калорий и нагрузки.",
                "Аватар можно сменить нажатием на фото в шапке.",
            ),
            icon = Icons.Filled.Restaurant,
        ),
        FeatureGuidePage(
            title = "Интеграции",
            lead = "Подключите Health Connect, FatSecret и Mi Band — данные будут подтягиваться автоматически.",
            bullets = listOf(
                "Health Connect — сон, шаги, пульс с телефона и часов.",
                "FatSecret — поиск продуктов по базе.",
                "Уведомления — напоминания о воде, еде и таблетках.",
            ),
            icon = Icons.Filled.Sync,
            miniIcons = listOf(Icons.Filled.Sync, Icons.Filled.Notifications),
        ),
        FeatureGuidePage(
            title = "Сообщество и здоровье",
            lead = "Друзья, клубы, достижения и дополнительные разделы здоровья — всё через профиль.",
            bullets = listOf(
                "«Витамины и таблетки» — напоминания о приёме.",
                "«Женское здоровье» — календарь цикла (если указан женский пол).",
                "«Подсказки по разделам» — показать обучение снова.",
            ),
            icon = Icons.Filled.Groups,
        ),
    )
}
