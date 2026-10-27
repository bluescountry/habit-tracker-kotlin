package com.dayquest.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import com.dayquest.data.db.DbHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

class BackupError(message: String) : Exception(message)

data class BackupSummary(
    val name: String,
    val level: Int,
    val quests: Int,
    val savedAt: String?,
    val file: File
)

class BackupManager(private val context: Context, private val dbHolder: DbHolder) {
    companion object {
        const val CURRENT_VERSION = 1

        fun suggestedBackupFilename(): String {
            val date = java.time.LocalDate.now().toString()
            return "dayquest-backup-$date.db"
        }
    }

    /** Writes a consistent snapshot of the live database to [dest]. */
    suspend fun backupTo(dest: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dbHolder.db.profileDao().touchLastBackup(Instant.now().toString())
            val tmp = File(context.cacheDir, "backup-${System.nanoTime()}.db").also { it.delete() }
            if (Build.VERSION.SDK_INT >= 30) {
                dbHolder.db.openHelper.writableDatabase.execSQL("VACUUM INTO '${tmp.absolutePath}'")
            } else {
                dbHolder.db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
                context.getDatabasePath(DbHolder.DB_NAME).copyTo(tmp, overwrite = true)
            }
            context.contentResolver.openOutputStream(dest, "wt")!!.use { out ->
                tmp.inputStream().use { it.copyTo(out) }
            }
            tmp.delete()
            Unit
        }
    }

    /** Validates [src] without touching the live database. Returns the summary for the confirmation box. */
    suspend fun inspect(src: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(context.cacheDir, "restore.db").also { it.delete() }
            context.contentResolver.openInputStream(src)?.use { i ->
                tmp.outputStream().use { i.copyTo(it) }
            } ?: throw BackupError("The file isn't valid backup data.")

            if (tmp.length() > 50L * 1024 * 1024) throw BackupError("The file is too large to be a Dayquest backup.")
            try {
                SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    if (db.version > CURRENT_VERSION) throw BackupError("This backup was made by a newer version of Dayquest.")
                    val integrityOk = db.rawQuery("PRAGMA integrity_check", null).use {
                        it.moveToFirst() && it.getString(0) == "ok"
                    }
                    if (!integrityOk) throw BackupError("The file isn't valid backup data.")

                    val fkOk = db.rawQuery("PRAGMA foreign_key_check", null).use { !it.moveToFirst() }
                    if (!fkOk) throw BackupError("Quest progress in the backup is damaged.")

                    db.rawQuery("SELECT name, level, last_backup_at FROM profile WHERE id = 1", null).use { c ->
                        if (!c.moveToFirst()) throw BackupError("This isn't a Dayquest backup.")
                        val name = c.getString(0)
                        val level = c.getInt(1)
                        val savedAt = if (c.isNull(2)) null else c.getString(2)
                        val questCount = db.rawQuery("SELECT COUNT(*) FROM quest", null).use { q ->
                            if (q.moveToFirst()) q.getInt(0) else 0
                        }
                        BackupSummary(name, level, questCount, savedAt, tmp)
                    }
                }
            } catch (be: BackupError) {
                throw be
            } catch (t: Throwable) {
                throw BackupError("The file isn't valid backup data.")
            }
        }
    }

    /** Swaps the validated copy in. Keeps pre-restore.db so a failure can be rolled back. */
    suspend fun restore(summary: BackupSummary): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val live = context.getDatabasePath(DbHolder.DB_NAME)
            val safety = File(live.parentFile ?: context.filesDir, "pre-restore.db")
            dbHolder.close()
            live.copyTo(safety, overwrite = true)
            try {
                File(live.path + "-wal").delete()
                File(live.path + "-shm").delete()
                summary.file.copyTo(live, overwrite = true)
                dbHolder.open()
            } catch (t: Throwable) {
                safety.copyTo(live, overwrite = true)
                dbHolder.open()
                throw t
            }
            Unit
        }
    }
}
