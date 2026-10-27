package com.dayquest.data.db

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DbHolder(private val context: Context) {
    companion object {
        const val DB_NAME = "dayquest.db"
    }

    private var _db: DayquestDb? = null
    private val _dbVersion = MutableStateFlow(0L)
    val dbVersion: StateFlow<Long> = _dbVersion

    val db: DayquestDb
        @Synchronized get() {
            if (_db == null) {
                _db = Room.databaseBuilder(context.applicationContext, DayquestDb::class.java, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return _db!!
        }

    @Synchronized
    fun close() {
        _db?.close()
        _db = null
    }

    @Synchronized
    fun open(): DayquestDb {
        val database = db
        _dbVersion.value = System.currentTimeMillis()
        return database
    }
}
