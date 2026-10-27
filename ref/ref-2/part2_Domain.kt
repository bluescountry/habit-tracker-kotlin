package com.dayquest.domain

// Domain models for everything the rank engine does not cover.
// RankStyle, Tier, RankInfo, rating() and rankInfo() already exist in RankEngine.kt (guide section 6).

enum class Category { Body, Read, Listen, Nourish, Mind, Watch }

enum class RewardKind { Small, Medium, Big }

data class Quest(
    val id: String,
    val category: Category,
    val title: String,
    val shortTitle: String,
    val xp: Int,
    val story: String,
    val steps: List<String>,
    val isCustom: Boolean = false,
    val libraryId: String? = null,
)

/** A reward the user wrote and has now earned. [text] is a copy, so later edits never change it. */
data class EarnedReward(
    val text: String,
    val kind: RewardKind,
    val from: String,            // "20-min strength", "14-day streak", "Reached Level 15", "Reached Mobile Legends Epic"
    val earnedAt: String,        // ISO-8601
    val enjoyedAt: String? = null,
) {
    val enjoyed: Boolean get() = enjoyedAt != null
}

/** Medium and big rewards waiting to be earned. */
data class Goals(
    val streak: Map<Int, String> = emptyMap(),     // 7, 14, 30
    val level: String? = null,                     // next level-up only, cleared when it pays out
    val rank: Map<String, String> = emptyMap(),    // "dota:Archon", "dota:Legend", "mlbb:Epic", "mlbb:Legend"
)

/**
 * Every parameter has a default so tests can build a state with just the fields they care about
 * (RankEngineTest passes level, xp, cap, claimed and rankStyle only).
 * Defaults match the prototype's seed numbers.
 */
data class AppState(
    val level: Int = 14,
    val xp: Int = 1648,
    val cap: Int = 2480,
    val streakBase: Int = 13,                       // days before today
    val securedToday: Boolean = false,
    val claimed: Set<String> = emptySet(),          // quest ids claimed today
    val rankStyle: RankStyle = RankStyle.Level,
    val name: String = "",
    val quests: List<Quest> = emptyList(),
    val stepsDone: Map<String, List<Boolean>> = emptyMap(),
    val archived: Set<String> = emptySet(),         // removed from today, restorable by Undo
    val rewards: Map<String, String> = emptyMap(),  // questId -> the user's own reward text
    val earned: Map<String, EarnedReward> = emptyMap(),   // key: questId | "streak:14" | "level:15" | "rank:mlbb:Epic"
    val goals: Goals = Goals(),
    val selectedQuestId: String? = null,
    val lastBackupAt: String? = null,
) {
    val streak: Int get() = streakBase + if (securedToday) 1 else 0
    val visibleQuests: List<Quest> get() = quests.filter { it.id !in archived }

    companion object {
        /** The prototype's starting state: Level style, Body quest first step ticked, sample rewards. */
        fun seed(name: String = "Sam"): AppState {
            val quests = SeedData.quests
            return AppState(
                name = name,
                quests = quests,
                stepsDone = quests.associate { q -> q.id to q.steps.mapIndexed { i, _ -> q.id == "body" && i == 0 } },
                rewards = mapOf("body" to "An iced latte", "watch" to "Order your favourite takeaway"),
                goals = Goals(
                    streak = mapOf(14 to "A nice dinner out", 30 to "A weekend day trip"),
                    level = "New running shoes",
                    rank = mapOf(
                        "mlbb:Epic" to "Pizza night with friends", "mlbb:Legend" to "A new gadget",
                        "dota:Archon" to "A takeaway night", "dota:Legend" to "A new gadget",
                    ),
                ),
                selectedQuestId = "body",
            )
        }
    }
}

object SeedData {
    val quests: List<Quest> = listOf(
        Quest("body", Category.Body, "Strength", "20-min strength", 90,
            "Twenty minutes, no equipment. Two moves, three rounds, and a stronger you before lunch.",
            listOf("Warm up: 2 min of arm circles and squats", "Pushups: 3 rounds of 12", "Plank: 3 holds of 45 seconds", "Log how it felt")),
        Quest("read", Category.Read, "Four Thousand Weeks", "Four Thousand Weeks", 70,
            "Fifteen pages of Oliver Burkeman on spending a finite life well. Pick up where your bookmark is.",
            listOf("Open the book at your bookmark", "Read 15 pages without switching apps", "Underline one idea worth keeping")),
        Quest("listen", Category.Listen, "Huberman Lab", "Sleep Architecture", 50,
            "One Huberman Lab episode on how sleep is built, ideally on a walk. Eyes up, screen off.",
            listOf("Queue the Sleep Architecture episode", "Listen for 25 minutes", "Note one takeaway for tonight")),
        Quest("nourish", Category.Nourish, "High-protein lunch", "High-protein lunch", 70,
            "Cook something real instead of grabbing something fast. Aim for at least 30 g of protein.",
            listOf("Pick a recipe with 30 g+ protein", "Cook it", "Eat at the table, phone away")),
        Quest("mind", Category.Mind, "Focus", "10-min focus", 60,
            "Ten quiet minutes to train attention. Count breaths, notice the drift, return.",
            listOf("Set a 10-minute timer", "Breathe and count to ten, repeat", "Note where your attention drifted")),
        Quest("watch", Category.Watch, "The Bear", "The Bear · S3 E4", 90,
            "Earn the couch. One episode after your other quests are done, no second-screening.",
            listOf("Finish at least two other quests", "Watch S3 E4 start to finish", "Rate it and jot one line")),
    )
}
