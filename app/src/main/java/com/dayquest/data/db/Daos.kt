package com.dayquest.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("UPDATE profile SET last_backup_at = :at WHERE id = 1")
    suspend fun touchLastBackup(at: String)

    @Query("UPDATE profile SET rank_style = :style WHERE id = 1")
    suspend fun updateRankStyle(style: String)
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quest ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE archived_at IS NULL ORDER BY sort_order ASC")
    fun observeActive(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest")
    suspend fun getAll(): List<QuestEntity>

    @Query("SELECT * FROM quest_step ORDER BY quest_id, position ASC")
    fun observeSteps(): Flow<List<QuestStepEntity>>

    @Query("SELECT * FROM quest_step ORDER BY quest_id, position ASC")
    suspend fun getAllSteps(): List<QuestStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<QuestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<QuestStepEntity>)

    @Query("UPDATE quest SET archived_at = :at WHERE id = :id")
    suspend fun archiveQuest(id: String, at: String)

    @Query("UPDATE quest SET archived_at = NULL WHERE id = :id")
    suspend fun unarchiveQuest(id: String)

    @Query("UPDATE quest SET reward_text = :text WHERE id = :id")
    suspend fun updateRewardText(id: String, text: String?)

    @Query("DELETE FROM quest WHERE is_custom = 1 AND archived_at < :cutoffDate")
    suspend fun cleanupOldArchived(cutoffDate: String)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM daily_progress WHERE day = :day")
    fun observeForDay(day: String): Flow<List<DailyProgressEntity>>

    @Query("SELECT * FROM daily_progress WHERE day = :day")
    suspend fun getForDay(day: String): List<DailyProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: DailyProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<DailyProgressEntity>)

    @Query("UPDATE daily_progress SET steps_done = :stepsDone WHERE day = :day AND quest_id = :questId")
    suspend fun updateStepsDone(day: String, questId: String, stepsDone: Int)

    @Query("UPDATE daily_progress SET claimed_at = :at WHERE day = :day AND quest_id = :questId")
    suspend fun markClaimed(day: String, questId: String, at: String)
}

@Dao
interface LedgerDao {
    @Insert
    suspend fun insert(entry: XpLedgerEntity)

    @Query("SELECT * FROM xp_ledger WHERE day IN (:days) ORDER BY created_at ASC")
    fun observeForDays(days: List<String>): Flow<List<XpLedgerEntity>>

    @Query("SELECT * FROM xp_ledger ORDER BY created_at ASC")
    fun observeAll(): Flow<List<XpLedgerEntity>>

    @Query("SELECT * FROM xp_ledger ORDER BY created_at ASC")
    suspend fun getAll(): List<XpLedgerEntity>
}

@Dao
interface RewardEarnedDao {
    @Query("SELECT * FROM reward_earned ORDER BY earned_at DESC")
    fun observeAll(): Flow<List<RewardEarnedEntity>>

    @Query("SELECT * FROM reward_earned")
    suspend fun getAll(): List<RewardEarnedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RewardEarnedEntity)

    @Query("UPDATE reward_earned SET enjoyed_at = :at WHERE source = :source")
    suspend fun markEnjoyed(source: String, at: String)
}

@Dao
interface MilestoneRewardDao {
    @Query("SELECT * FROM milestone_reward")
    fun observeAll(): Flow<List<MilestoneRewardEntity>>

    @Query("SELECT * FROM milestone_reward")
    suspend fun getAll(): List<MilestoneRewardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MilestoneRewardEntity)

    @Query("DELETE FROM milestone_reward WHERE kind = :kind AND key = :key")
    suspend fun delete(kind: String, key: String)
}

@Dao
interface DayLogDao {
    @Query("SELECT * FROM day_log ORDER BY day DESC")
    fun observeAll(): Flow<List<DayLogEntity>>

    @Query("SELECT * FROM day_log ORDER BY day DESC")
    suspend fun getAll(): List<DayLogEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DayLogEntity)

    @Query("SELECT COUNT(*) > 0 FROM day_log WHERE day = :day")
    suspend fun hasDay(day: String): Boolean
}
