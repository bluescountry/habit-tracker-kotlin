package com.dayquest.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        QuestEntity::class,
        QuestStepEntity::class,
        DailyProgressEntity::class,
        XpLedgerEntity::class,
        RewardEarnedEntity::class,
        MilestoneRewardEntity::class,
        DayLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DayquestDb : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun questDao(): QuestDao
    abstract fun progressDao(): ProgressDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun rewardEarnedDao(): RewardEarnedDao
    abstract fun milestoneRewardDao(): MilestoneRewardDao
    abstract fun dayLogDao(): DayLogDao
}
