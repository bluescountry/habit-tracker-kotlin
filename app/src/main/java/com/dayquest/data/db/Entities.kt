package com.dayquest.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val level: Int,
    val xp: Int,
    val cap: Int,
    @ColumnInfo(name = "streak_base") val streakBase: Int,
    @ColumnInfo(name = "rank_style") val rankStyle: String = "level",
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "last_backup_at") val lastBackupAt: String? = null,
)

@Entity(
    tableName = "quest",
    indices = [Index(value = ["archived_at"])]
)
data class QuestEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val xp: Int,
    val story: String,
    @ColumnInfo(name = "is_custom") val isCustom: Int = 0,
    @ColumnInfo(name = "library_id") val libraryId: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "archived_at") val archivedAt: String? = null,
    @ColumnInfo(name = "reward_text") val rewardText: String? = null,
)

@Entity(
    tableName = "quest_step",
    primaryKeys = ["quest_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["quest_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuestStepEntity(
    @ColumnInfo(name = "quest_id") val questId: String,
    val position: Int,
    val text: String,
)

@Entity(
    tableName = "daily_progress",
    primaryKeys = ["day", "quest_id"],
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["quest_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["quest_id"])]
)
data class DailyProgressEntity(
    val day: String,
    @ColumnInfo(name = "quest_id") val questId: String,
    @ColumnInfo(name = "steps_done") val stepsDone: Int = 0,
    @ColumnInfo(name = "claimed_at") val claimedAt: String? = null,
)

@Entity(
    tableName = "xp_ledger",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["quest_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["day"]), Index(value = ["quest_id"])]
)
data class XpLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,
    @ColumnInfo(name = "quest_id") val questId: String? = null,
    val category: String,
    val xp: Int,
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(
    tableName = "reward_earned",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["quest_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["source"], unique = true), Index(value = ["enjoyed_at"]), Index(value = ["quest_id"])]
)
data class RewardEarnedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String = "small", // "small", "medium", "big"
    val source: String,
    @ColumnInfo(name = "quest_id") val questId: String? = null,
    @ColumnInfo(name = "from_label") val fromLabel: String,
    @ColumnInfo(name = "reward_text") val rewardText: String,
    @ColumnInfo(name = "earned_at") val earnedAt: String,
    @ColumnInfo(name = "enjoyed_at") val enjoyedAt: String? = null,
)

@Entity(
    tableName = "milestone_reward",
    primaryKeys = ["kind", "key"]
)
data class MilestoneRewardEntity(
    val kind: String, // 'streak', 'level', 'rank'
    val key: String,  // '14', 'next', 'mlbb:Epic'
    @ColumnInfo(name = "reward_text") val rewardText: String,
)

@Entity(tableName = "day_log")
data class DayLogEntity(
    @PrimaryKey val day: String,
)
