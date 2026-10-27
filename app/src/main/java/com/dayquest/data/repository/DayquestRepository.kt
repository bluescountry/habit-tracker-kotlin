package com.dayquest.data.repository

import androidx.room.withTransaction
import com.dayquest.data.db.DailyProgressEntity
import com.dayquest.data.db.DayLogEntity
import com.dayquest.data.db.DayquestDb
import com.dayquest.data.db.DbHolder
import com.dayquest.data.db.MilestoneRewardEntity
import com.dayquest.data.db.ProfileEntity
import com.dayquest.data.db.QuestEntity
import com.dayquest.data.db.QuestStepEntity
import com.dayquest.data.db.RewardEarnedEntity
import com.dayquest.data.db.XpLedgerEntity
import com.dayquest.domain.AppState
import com.dayquest.domain.Category
import com.dayquest.domain.ClaimResult
import com.dayquest.domain.EarnedReward
import com.dayquest.domain.GoalSlot
import com.dayquest.domain.Goals
import com.dayquest.domain.Quest
import com.dayquest.domain.RankStyle
import com.dayquest.domain.RewardKind
import com.dayquest.domain.SeedData
import com.dayquest.domain.advanceQuest
import com.dayquest.domain.claim
import com.dayquest.domain.key
import com.dayquest.domain.removeQuest
import com.dayquest.domain.toggleStep
import com.dayquest.domain.validReward
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DayquestRepository(
    private val dbHolder: DbHolder,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    val db: DayquestDb get() = dbHolder.db

    fun observe(): Flow<AppState?> {
        return dbHolder.dbVersion.flatMapLatest {
            val database = dbHolder.db
            val today = LocalDate.now(clock).toString()

            combine(
                database.profileDao().observe(),
                database.questDao().observeAll(),
                database.questDao().observeSteps(),
                database.progressDao().observeForDay(today),
                database.rewardEarnedDao().observeAll(),
                database.milestoneRewardDao().observeAll(),
                database.dayLogDao().observeAll()
            ) { array: Array<Any?> ->
                val profile = array[0] as? ProfileEntity
                if (profile == null) return@combine null
                @Suppress("UNCHECKED_CAST")
                val quests = array[1] as List<QuestEntity>
                @Suppress("UNCHECKED_CAST")
                val steps = array[2] as List<QuestStepEntity>
                @Suppress("UNCHECKED_CAST")
                val progress = array[3] as List<DailyProgressEntity>
                @Suppress("UNCHECKED_CAST")
                val rewardsEarned = array[4] as List<RewardEarnedEntity>
                @Suppress("UNCHECKED_CAST")
                val milestones = array[5] as List<MilestoneRewardEntity>
                @Suppress("UNCHECKED_CAST")
                val dayLogs = array[6] as List<DayLogEntity>
                mapToAppState(profile, quests, steps, progress, rewardsEarned, milestones, dayLogs, today)
            }
        }
    }

    suspend fun loadState(): AppState? {
        val database = db
        val today = LocalDate.now(clock).toString()
        val profile = database.profileDao().get() ?: return null
        val quests = database.questDao().getAll()
        val steps = database.questDao().getAllSteps()
        val progress = database.progressDao().getForDay(today)
        val rewardsEarned = database.rewardEarnedDao().getAll()
        val milestones = database.milestoneRewardDao().getAll()
        val dayLogs = database.dayLogDao().getAll()

        return mapToAppState(profile, quests, steps, progress, rewardsEarned, milestones, dayLogs, today)
    }

    private fun mapToAppState(
        profile: ProfileEntity,
        quests: List<QuestEntity>,
        steps: List<QuestStepEntity>,
        progress: List<DailyProgressEntity>,
        rewardsEarned: List<RewardEarnedEntity>,
        milestones: List<MilestoneRewardEntity>,
        dayLogs: List<DayLogEntity>,
        today: String
    ): AppState {
        val stepMap = steps.groupBy({ it.questId }, { it.text })
        val domainQuests = quests.map { q ->
            val stList = stepMap[q.id] ?: emptyList()
            Quest(
                id = q.id,
                category = runCatching { Category.valueOf(q.category) }.getOrDefault(Category.Body),
                title = q.title,
                shortTitle = q.title,
                xp = q.xp,
                story = q.story,
                steps = stList,
                isCustom = q.isCustom == 1,
                libraryId = q.libraryId
            )
        }

        val progressByQuest = progress.associateBy { it.questId }
        val stepsDoneMap = domainQuests.associate { q ->
            val p = progressByQuest[q.id]
            val mask = p?.stepsDone ?: 0
            q.id to q.steps.mapIndexed { idx, _ -> ((mask shr idx) and 1) == 1 }
        }

        val claimedSet = progress.filter { it.claimedAt != null }.map { it.questId }.toSet()
        val archivedSet = quests.filter { it.archivedAt != null }.map { it.id }.toSet()
        val questRewards = quests.mapNotNull { q -> q.rewardText?.let { q.id to it } }.toMap()

        val earnedMap = rewardsEarned.associate { r ->
            val key = if (r.source.startsWith("quest:")) {
                r.questId ?: r.source.removePrefix("quest:").substringBefore(":")
            } else {
                r.source
            }
            val kind = when (r.kind) {
                "medium" -> RewardKind.Medium
                "big" -> RewardKind.Big
                else -> RewardKind.Small
            }
            key to EarnedReward(
                text = r.rewardText,
                kind = kind,
                from = r.fromLabel,
                earnedAt = r.earnedAt,
                enjoyedAt = r.enjoyedAt
            )
        }

        var streakGoals = emptyMap<Int, String>()
        var levelGoal: String? = null
        var rankGoals = emptyMap<String, String>()

        milestones.forEach { m ->
            when (m.kind) {
                "streak" -> m.key.toIntOrNull()?.let { d -> streakGoals = streakGoals + (d to m.rewardText) }
                "level" -> if (m.key == "next") levelGoal = m.rewardText
                "rank" -> rankGoals = rankGoals + (m.key to m.rewardText)
            }
        }

        val dayLogSet = dayLogs.map { it.day }.toSet()
        val securedToday = dayLogSet.contains(today)

        val todayDate = LocalDate.parse(today)
        var consecutiveDays = 0
        var checkDate = todayDate.minusDays(1)
        while (dayLogSet.contains(checkDate.toString())) {
            consecutiveDays++
            checkDate = checkDate.minusDays(1)
        }
        val streakBase = profile.streakBase + consecutiveDays

        val rankStyle = RankStyle.entries.firstOrNull { it.key == profile.rankStyle } ?: RankStyle.Level

        val visible = domainQuests.filter { it.id !in archivedSet }
        val selected = visible.firstOrNull { it.id !in claimedSet }?.id ?: visible.firstOrNull()?.id

        return AppState(
            level = profile.level,
            xp = profile.xp,
            cap = profile.cap,
            streakBase = streakBase,
            securedToday = securedToday,
            claimed = claimedSet,
            rankStyle = rankStyle,
            name = profile.name,
            quests = domainQuests,
            stepsDone = stepsDoneMap,
            archived = archivedSet,
            rewards = questRewards,
            earned = earnedMap,
            goals = Goals(streak = streakGoals, level = levelGoal, rank = rankGoals),
            selectedQuestId = selected,
            lastBackupAt = profile.lastBackupAt
        )
    }

    suspend fun claim(questId: String): ClaimResult? = db.withTransaction {
        val before = loadState() ?: return@withTransaction null
        val now = Instant.now(clock).toString()
        val today = LocalDate.now(clock).toString()
        val result = claim(before, questId, now) ?: return@withTransaction null
        persistClaim(before, result.state, today, now)
        result
    }

    private suspend fun persistClaim(before: AppState, after: AppState, today: String, now: String) {
        val database = db
        // 1. daily_progress for newly claimed
        val newClaimed = after.claimed - before.claimed
        for (qId in newClaimed) {
            val steps = after.stepsDone[qId] ?: emptyList()
            val mask = steps.foldIndexed(0) { i, acc, done -> if (done) acc or (1 shl i) else acc }
            database.progressDao().upsert(
                DailyProgressEntity(day = today, questId = qId, stepsDone = mask, claimedAt = now)
            )
        }

        // 2. xp_ledger rows
        for (qId in newClaimed) {
            val q = after.quests.firstOrNull { it.id == qId } ?: continue
            database.ledgerDao().insert(
                XpLedgerEntity(day = today, questId = qId, category = q.category.name, xp = q.xp, createdAt = now)
            )
        }

        // 3. profile update
        val curProfile = database.profileDao().get()
        if (curProfile != null) {
            database.profileDao().upsert(
                curProfile.copy(level = after.level, xp = after.xp, cap = after.cap)
            )
        }

        // 4. day_log insert if secured today
        if (after.securedToday && !before.securedToday) {
            database.dayLogDao().insert(DayLogEntity(day = today))
        }

        // 5. reward_earned
        val newEarnedKeys = after.earned.keys - before.earned.keys
        for (key in newEarnedKeys) {
            val reward = after.earned.getValue(key)
            val source = when {
                key.startsWith("streak:") || key.startsWith("level:") || key.startsWith("rank:") -> key
                else -> "quest:$key:$today"
            }
            val questId = if (reward.kind == RewardKind.Small) key else null
            database.rewardEarnedDao().insert(
                RewardEarnedEntity(
                    kind = reward.kind.name.lowercase(),
                    source = source,
                    questId = questId,
                    fromLabel = reward.from,
                    rewardText = reward.text,
                    earnedAt = reward.earnedAt
                )
            )
        }

        // 6. delete level goal if paid out
        if (before.goals.level != null && after.goals.level == null) {
            database.milestoneRewardDao().delete("level", "next")
        }
    }

    suspend fun toggleStep(questId: String, index: Int): AppState? = db.withTransaction {
        val before = loadState() ?: return@withTransaction null
        val today = LocalDate.now(clock).toString()
        val after = toggleStep(before, questId, index)
        val steps = after.stepsDone[questId] ?: emptyList()
        val mask = steps.foldIndexed(0) { i, acc, done -> if (done) acc or (1 shl i) else acc }
        val curProgress = db.progressDao().getForDay(today).firstOrNull { it.questId == questId }
        db.progressDao().upsert(
            DailyProgressEntity(
                day = today,
                questId = questId,
                stepsDone = mask,
                claimedAt = curProgress?.claimedAt
            )
        )
        after
    }

    suspend fun advanceQuest(questId: String): AppState? = db.withTransaction {
        val before = loadState() ?: return@withTransaction null
        val today = LocalDate.now(clock).toString()
        val after = advanceQuest(before, questId)
        val steps = after.stepsDone[questId] ?: emptyList()
        val mask = steps.foldIndexed(0) { i, acc, done -> if (done) acc or (1 shl i) else acc }
        val curProgress = db.progressDao().getForDay(today).firstOrNull { it.questId == questId }
        db.progressDao().upsert(
            DailyProgressEntity(
                day = today,
                questId = questId,
                stepsDone = mask,
                claimedAt = curProgress?.claimedAt
            )
        )
        after
    }

    suspend fun addQuest(quest: Quest, reward: String?) = db.withTransaction {
        val sortOrder = (db.questDao().getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val entity = QuestEntity(
            id = quest.id,
            category = quest.category.name,
            title = quest.title,
            xp = quest.xp,
            story = quest.story,
            isCustom = if (quest.isCustom) 1 else 0,
            libraryId = quest.libraryId,
            sortOrder = sortOrder,
            archivedAt = null,
            rewardText = validReward(reward)
        )
        db.questDao().insertQuests(listOf(entity))
        val stepEntities = quest.steps.mapIndexed { idx, text ->
            QuestStepEntity(questId = quest.id, position = idx, text = text)
        }
        db.questDao().insertSteps(stepEntities)
    }

    suspend fun removeQuest(questId: String): Boolean = db.withTransaction {
        val before = loadState() ?: return@withTransaction false
        if (questId in before.claimed || before.quests.none { it.id == questId } || questId in before.archived) {
            return@withTransaction false
        }
        val now = Instant.now(clock).toString()
        db.questDao().archiveQuest(questId, now)
        true
    }

    suspend fun undoRemove(questId: String) = db.withTransaction {
        db.questDao().unarchiveQuest(questId)
    }

    suspend fun setQuestReward(questId: String, text: String?) = db.withTransaction {
        val before = loadState() ?: return@withTransaction
        if (questId in before.claimed) return@withTransaction
        db.questDao().updateRewardText(questId, validReward(text))
    }

    suspend fun setGoal(slot: GoalSlot, text: String?) = db.withTransaction {
        val t = validReward(text)
        when (slot) {
            is GoalSlot.Streak -> {
                if (t == null) db.milestoneRewardDao().delete("streak", slot.days.toString())
                else db.milestoneRewardDao().upsert(MilestoneRewardEntity("streak", slot.days.toString(), t))
            }
            GoalSlot.Level -> {
                if (t == null) db.milestoneRewardDao().delete("level", "next")
                else db.milestoneRewardDao().upsert(MilestoneRewardEntity("level", "next", t))
            }
            is GoalSlot.Rank -> {
                if (t == null) db.milestoneRewardDao().delete("rank", slot.key)
                else db.milestoneRewardDao().upsert(MilestoneRewardEntity("rank", slot.key, t))
            }
        }
    }

    suspend fun enjoy(key: String) = db.withTransaction {
        val now = Instant.now(clock).toString()
        val allRewards = db.rewardEarnedDao().getAll()
        val target = if (key.contains(":")) {
            allRewards.firstOrNull { it.source == key }
        } else {
            allRewards.firstOrNull { it.questId == key || it.source.startsWith("quest:$key:") }
        }
        if (target != null && target.enjoyedAt == null) {
            db.rewardEarnedDao().markEnjoyed(target.source, now)
        }
    }

    suspend fun setRankStyle(style: RankStyle) = db.withTransaction {
        db.profileDao().updateRankStyle(style.key)
    }

    suspend fun completeOnboarding(rawName: String, isDemo: Boolean = false): Boolean = db.withTransaction {
        val name = rawName.trim()
        if (name.isEmpty() || name.length > 24) return@withTransaction false
        val now = Instant.now(clock).toString()
        val today = LocalDate.now(clock).toString()

        val (level, xp, cap, streakBase) = if (isDemo) {
            listOf(14, 1648, 2480, 13)
        } else {
            listOf(1, 0, 500, 0)
        }

        db.profileDao().upsert(
            ProfileEntity(
                id = 1,
                name = name,
                level = level,
                xp = xp,
                cap = cap,
                streakBase = streakBase,
                rankStyle = "level",
                createdAt = now
            )
        )

        val seedQuests = SeedData.quests
        val questEntities = seedQuests.mapIndexed { idx, q ->
            val reward = when (q.id) {
                "body" -> "An iced latte"
                "watch" -> "Order your favourite takeaway"
                else -> null
            }
            QuestEntity(
                id = q.id,
                category = q.category.name,
                title = q.title,
                xp = q.xp,
                story = q.story,
                isCustom = 0,
                libraryId = null,
                sortOrder = idx,
                archivedAt = null,
                rewardText = reward
            )
        }
        db.questDao().insertQuests(questEntities)

        val stepEntities = seedQuests.flatMap { q ->
            q.steps.mapIndexed { pIdx, text ->
                QuestStepEntity(questId = q.id, position = pIdx, text = text)
            }
        }
        db.questDao().insertSteps(stepEntities)

        if (isDemo) {
            db.progressDao().upsert(
                DailyProgressEntity(
                    day = today,
                    questId = "body",
                    stepsDone = 1, // bit 0 = first step ticked
                    claimedAt = null
                )
            )
        }

        val milestones = if (isDemo) {
            listOf(
                MilestoneRewardEntity("streak", "14", "A nice dinner out"),
                MilestoneRewardEntity("streak", "30", "A weekend day trip"),
                MilestoneRewardEntity("level", "next", "New running shoes"),
                MilestoneRewardEntity("rank", "mlbb:Epic", "Pizza night with friends"),
                MilestoneRewardEntity("rank", "mlbb:Legend", "A new gadget"),
                MilestoneRewardEntity("rank", "dota:Archon", "A takeaway night"),
                MilestoneRewardEntity("rank", "dota:Legend", "A new gadget"),
            )
        } else {
            listOf(
                MilestoneRewardEntity("streak", "7", "A nice dinner out"),
                MilestoneRewardEntity("streak", "14", "A weekend day trip"),
                MilestoneRewardEntity("streak", "30", "A big celebration"),
                MilestoneRewardEntity("level", "next", "A treat for Level 2"),
                MilestoneRewardEntity("rank", "mlbb:Epic", "Pizza night with friends"),
                MilestoneRewardEntity("rank", "mlbb:Legend", "A new gadget"),
                MilestoneRewardEntity("rank", "dota:Archon", "A takeaway night"),
                MilestoneRewardEntity("rank", "dota:Legend", "A new gadget"),
            )
        }
        milestones.forEach { db.milestoneRewardDao().upsert(it) }

        true
    }

    suspend fun reset() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        dbHolder.open()
    }
}
