package com.dayquest.domain

// Pure functions: state in, state out. No Android, no database, no clock (callers pass `nowIso`).
// Mirrors claim(), removeQuest(), checkGoals() etc. in dayquest-mobile-app.html.

val STREAK_GOALS = listOf(7, 14, 30)

/** Dota 2 has no Epic tier, so Archon (the tier just below Legend) takes the same place. */
val RANK_REWARD: Map<RankStyle, List<String>> = mapOf(
    RankStyle.Dota to listOf("Archon", "Legend"),
    RankStyle.Mlbb to listOf("Epic", "Legend"),
)

val RankStyle.key: String get() = name.lowercase()          // "dota", "mlbb", "lol", "valorant", "level"
fun tierLow(style: RankStyle, tier: String): Int = style.tiers.first { it.name == tier }.low

data class DisplayRank(val label: String, val tier: String, val numeral: String)

fun displayRank(s: AppState): DisplayRank =
    rankInfo(s)?.let { DisplayRank(it.label, it.tier, it.numeral) }
        ?: DisplayRank("Level ${s.level}", "Level", s.level.toString())

sealed interface Event {
    data class RankUp(val before: DisplayRank, val after: DisplayRank, val gain: Int) : Event
    /** [numeral] is the streak day, the level, or the tier name when [isWord] is true. */
    data class RewardEarned(val key: String, val reward: EarnedReward, val numeral: String, val isWord: Boolean) : Event
}

data class ClaimResult(val state: AppState, val message: String, val events: List<Event>)

/** Claims a quest whose steps are all done. Returns null when it cannot be claimed. */
fun claim(s: AppState, questId: String, nowIso: String): ClaimResult? {
    val quest = s.quests.firstOrNull { it.id == questId && it.id !in s.archived } ?: return null
    if (questId in s.claimed) return null
    if (s.stepsDone[questId]?.all { it } != true) return null

    val beforeRank = displayRank(s)
    val rating0 = rating(s)
    var st = s.copy(claimed = s.claimed + questId, xp = s.xp + quest.xp)
    var msg = "+${quest.xp} XP — ${quest.category} quest complete."

    // small reward (written on the quest)
    s.rewards[questId]?.let { text ->
        st = st.copy(earned = st.earned + (questId to EarnedReward(text, RewardKind.Small, quest.shortTitle, nowIso)))
        msg += " Reward ready: $text."
    }

    // streak
    var newDay = 0
    if (!st.securedToday) {
        newDay = st.streakBase + 1
        st = st.copy(securedToday = true)
        msg += " Streak extended to $newDay days."
    }

    // level-up loop (cap grows 12% per level, rounded to the nearest 10)
    val gained = mutableListOf<Int>()
    var xp = st.xp; var level = st.level; var cap = st.cap
    while (xp >= cap) {
        xp -= cap; level += 1; gained += level
        cap = (Math.round(cap * 1.12 / 10.0) * 10).toInt()
        msg = "Level $level reached! $msg"
    }
    st = st.copy(xp = xp, level = level, cap = cap)

    // rank or level label changed?
    val events = mutableListOf<Event>()
    val after = displayRank(st)
    if (after.label != beforeRank.label) {
        msg = "${if (st.rankStyle == RankStyle.Level) "Level up" else "Rank up"}: ${after.label}. $msg"
        events += Event.RankUp(beforeRank, after, rating(st) - rating0)
    }

    // medium and big rewards
    val (st2, earnedEvents) = checkGoals(st, newDay, gained, nowIso)
    if (earnedEvents.isNotEmpty()) msg += " Reward earned: ${earnedEvents.joinToString(", ") { it.reward.text }}."
    return ClaimResult(st2, msg, events + earnedEvents)
}

/** Order matters: streak, then level-up (first level gained), then rank tiers of the selected game. */
fun checkGoals(s: AppState, newDay: Int, gained: List<Int>, nowIso: String): Pair<AppState, List<Event.RewardEarned>> {
    var earned = s.earned
    var goals = s.goals
    val out = mutableListOf<Event.RewardEarned>()

    fun earn(key: String, kind: RewardKind, text: String, from: String, numeral: String, isWord: Boolean): Boolean {
        if (key in earned) return false
        val reward = EarnedReward(text, kind, from, nowIso)
        earned = earned + (key to reward)
        out += Event.RewardEarned(key, reward, numeral, isWord)
        return true
    }

    if (newDay > 0) goals.streak[newDay]?.let { earn("streak:$newDay", RewardKind.Medium, it, "$newDay-day streak", "$newDay", false) }

    if (gained.isNotEmpty()) goals.level?.let { text ->
        if (earn("level:${gained[0]}", RewardKind.Big, text, "Reached Level ${gained[0]}", "${gained[0]}", false)) goals = goals.copy(level = null)
    }

    val style = s.rankStyle                        // rank rewards belong to the game in use
    RANK_REWARD[style]?.forEach { tier ->
        val key = "${style.key}:$tier"
        val text = goals.rank[key] ?: return@forEach
        if (rating(s) >= tierLow(style, tier)) earn("rank:$key", RewardKind.Big, text, "Reached ${style.label} $tier", tier, true)
    }
    return s.copy(earned = earned, goals = goals) to out
}

// ---- steps -----------------------------------------------------------------------------------

fun toggleStep(s: AppState, questId: String, index: Int): AppState {
    if (questId in s.claimed) return s
    val steps = s.stepsDone[questId] ?: return s
    if (index !in steps.indices) return s
    return s.copy(stepsDone = s.stepsDone + (questId to steps.mapIndexed { i, v -> if (i == index) !v else v }))
}

/** "Start quest" / "Mark next step done": ticks the first unticked step. */
fun advanceQuest(s: AppState, questId: String): AppState {
    val i = s.stepsDone[questId]?.indexOf(false) ?: return s
    return if (i < 0) s else toggleStep(s, questId, i)
}

// ---- quests ----------------------------------------------------------------------------------

fun validReward(text: String?): String? = text?.trim()?.takeIf { it.isNotEmpty() }?.take(40)

fun addQuest(s: AppState, quest: Quest, reward: String? = null): AppState = s.copy(
    quests = s.quests + quest,
    stepsDone = s.stepsDone + (quest.id to quest.steps.map { false }),
    rewards = validReward(reward)?.let { s.rewards + (quest.id to it) } ?: s.rewards,
    selectedQuestId = quest.id,
)

/** Soft delete. Only unclaimed quests can be removed; the caller keeps the id for Undo. */
fun removeQuest(s: AppState, questId: String): AppState? {
    if (s.quests.none { it.id == questId } || questId in s.claimed || questId in s.archived) return null
    val archived = s.archived + questId
    val next = s.quests.firstOrNull { it.id !in archived && it.id !in s.claimed }
        ?: s.quests.firstOrNull { it.id !in archived }
    return s.copy(archived = archived, selectedQuestId = next?.id)
}

fun undoRemove(s: AppState, questId: String): AppState =
    s.copy(archived = s.archived - questId, selectedQuestId = questId)

// ---- rewards ---------------------------------------------------------------------------------

/** Small reward on a quest. Ignored once the quest is claimed. Blank text removes it. */
fun setQuestReward(s: AppState, questId: String, text: String?): AppState {
    if (questId in s.claimed || s.quests.none { it.id == questId }) return s
    val t = validReward(text)
    return s.copy(rewards = if (t == null) s.rewards - questId else s.rewards + (questId to t))
}

fun enjoy(s: AppState, key: String, nowIso: String): AppState {
    val e = s.earned[key] ?: return s
    return if (e.enjoyed) s else s.copy(earned = s.earned + (key to e.copy(enjoyedAt = nowIso)))
}

sealed interface GoalSlot {
    data class Streak(val days: Int) : GoalSlot
    object Level : GoalSlot
    data class Rank(val key: String) : GoalSlot       // "mlbb:Epic"
}

/** Medium and big rewards. A milestone already reached cannot be given a reward. Blank text removes it. */
fun setGoal(s: AppState, slot: GoalSlot, text: String?): AppState {
    val t = validReward(text)
    val g = s.goals
    return when (slot) {
        is GoalSlot.Streak -> {
            if (slot.days !in STREAK_GOALS || slot.days <= s.streak) s
            else s.copy(goals = g.copy(streak = if (t == null) g.streak - slot.days else g.streak + (slot.days to t)))
        }
        GoalSlot.Level -> s.copy(goals = g.copy(level = t))
        is GoalSlot.Rank -> {
            val parts = slot.key.split(":")
            if (parts.size != 2) return s
            val (game, tier) = parts
            val style = RANK_REWARD.keys.firstOrNull { it.key == game }
            if (style == null || tier !in RANK_REWARD.getValue(style) || rating(s) >= tierLow(style, tier)) s
            else s.copy(goals = g.copy(rank = if (t == null) g.rank - slot.key else g.rank + (slot.key to t)))
        }
    }
}

fun completeOnboarding(s: AppState, rawName: String): AppState? {
    val name = rawName.trim()
    return if (name.isEmpty() || name.length > 24) null else s.copy(name = name)
}
