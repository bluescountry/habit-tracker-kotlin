package com.dayquest.data

import com.dayquest.domain.Category
import com.dayquest.domain.LibraryItem
import com.dayquest.domain.Quest

object SeedData {
    val QUESTS: List<Quest> = listOf(
        Quest(
            id = "body",
            category = Category.Body,
            title = "Body — strength",
            shortTitle = "20-min strength",
            xp = 90,
            story = "Twenty minutes, no equipment. Two moves, three rounds, and a stronger you before lunch.",
            steps = listOf(
                "Warm up: 2 min of arm circles and squats",
                "Pushups: 3 rounds of 12",
                "Plank: 3 holds of 45 seconds",
                "Log how it felt"
            )
        ),
        Quest(
            id = "read",
            category = Category.Read,
            title = "Read — Four Thousand Weeks",
            shortTitle = "Four Thousand Weeks",
            xp = 70,
            story = "Fifteen pages of Oliver Burkeman on spending a finite life well. Pick up where your bookmark is.",
            steps = listOf(
                "Open the book at your bookmark",
                "Read 15 pages without switching apps",
                "Underline one idea worth keeping"
            )
        ),
        Quest(
            id = "listen",
            category = Category.Listen,
            title = "Listen — Huberman Lab",
            shortTitle = "Sleep Architecture",
            xp = 50,
            story = "One Huberman Lab episode on how sleep is built, ideally on a walk. Eyes up, screen off.",
            steps = listOf(
                "Queue the Sleep Architecture episode",
                "Listen for 25 minutes",
                "Note one takeaway for tonight"
            )
        ),
        Quest(
            id = "nourish",
            category = Category.Nourish,
            title = "Nourish — high-protein lunch",
            shortTitle = "High-protein lunch",
            xp = 70,
            story = "Cook something real instead of grabbing something fast. Aim for at least 30 g of protein.",
            steps = listOf(
                "Pick a recipe with 30 g+ protein",
                "Cook it",
                "Eat at the table, phone away"
            )
        ),
        Quest(
            id = "mind",
            category = Category.Mind,
            title = "Mind — focus",
            shortTitle = "10-min focus",
            xp = 60,
            story = "Ten quiet minutes to train attention. Count breaths, notice the drift, return.",
            steps = listOf(
                "Set a 10-minute timer",
                "Breathe and count to ten, repeat",
                "Note where your attention drifted"
            )
        ),
        Quest(
            id = "watch",
            category = Category.Watch,
            title = "Watch — The Bear",
            shortTitle = "The Bear · S3 E4",
            xp = 90,
            story = "Earn the couch. One episode after your other quests are done, no second-screening.",
            steps = listOf(
                "Finish at least two other quests",
                "Watch S3 E4 start to finish",
                "Rate it and jot one line"
            )
        )
    )

    val INITIAL_STEPS_DONE: Map<String, List<Boolean>> = mapOf(
        "body" to listOf(true, false, false, false),
        "read" to listOf(false, false, false),
        "listen" to listOf(false, false, false),
        "nourish" to listOf(false, false, false),
        "mind" to listOf(false, false, false),
        "watch" to listOf(false, false, false)
    )

    val LIBRARY: List<LibraryItem> = listOf(
        LibraryItem("l1", Category.Body, "30-minute walk", 50, "30 min", listOf("Put your shoes on", "Walk for 30 minutes", "Note one thing you noticed")),
        LibraryItem("l2", Category.Body, "10-minute stretch", 40, "10 min", listOf("Clear a patch of floor", "Stretch hips, back and shoulders", "Hold each position for 30 seconds")),
        LibraryItem("l3", Category.Read, "20 pages of fiction", 60, "20 pages", listOf("Pick the book up", "Read 20 pages", "Write down one line you liked")),
        LibraryItem("l4", Category.Read, "Read and summarise an article", 50, "15 min", listOf("Choose one long article", "Read it without skimming", "Summarise it in two sentences")),
        LibraryItem("l5", Category.Listen, "Language podcast episode", 40, "20 min", listOf("Queue the episode", "Listen and repeat out loud", "Note three new words")),
        LibraryItem("l6", Category.Listen, "One album, no skipping", 50, "40 min", listOf("Pick an album you have never finished", "Listen start to finish", "Name your favourite track")),
        LibraryItem("l7", Category.Nourish, "Drink 2 litres of water", 30, "All day", listOf("Fill your bottle in the morning", "Refill it at lunch", "Finish it before dinner")),
        LibraryItem("l8", Category.Nourish, "Cook dinner from scratch", 80, "45 min", listOf("Choose a recipe", "Cook it", "Eat it at the table")),
        LibraryItem("l9", Category.Mind, "Journal for five minutes", 40, "5 min", listOf("Open your notebook", "Write for five minutes without stopping", "Circle the one thing that matters")),
        LibraryItem("l10", Category.Mind, "Screens off after 21:00", 60, "Evening", listOf("Set a 20:45 reminder", "Put the phone in another room", "Do something offline")),
        LibraryItem("l11", Category.Watch, "One documentary episode", 70, "45 min", listOf("Pick an episode", "Watch it without a second screen", "Jot down one thing you learned")),
        LibraryItem("l12", Category.Watch, "Learn a skill on video", 60, "20 min", listOf("Choose a tutorial", "Follow along, don’t just watch", "Try the technique once on your own"))
    )

    val WEEK_XP: List<Int> = listOf(310, 280, 420, 350, 190, 400) // Mon-Sat (sum 1,950)
    val WEEK_CAT: Map<Category, Int> = mapOf(
        Category.Body to 520,
        Category.Read to 380,
        Category.Listen to 250,
        Category.Nourish to 290,
        Category.Mind to 330,
        Category.Watch to 180
    )
    val MISSED_INDICES: Set<Int> = setOf(2, 6, 10, 12, 21) // out of 35 days (Mon 21 Sep to Sun 25 Oct)
    const val WEEK_QUESTS_BASE: Int = 29
}
