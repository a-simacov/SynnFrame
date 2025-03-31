package com.synngate.synnframe.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.synngate.synnframe.data.local.dao.LogDao
import com.synngate.synnframe.data.local.dao.ProductDao
import com.synngate.synnframe.data.local.dao.ServerDao
import com.synngate.synnframe.data.local.dao.SyncHistoryDao
import com.synngate.synnframe.data.local.dao.SyncOperationDao
import com.synngate.synnframe.data.local.dao.TaskDao
import com.synngate.synnframe.data.local.dao.UserDao
import com.synngate.synnframe.data.local.entity.BarcodeEntity
import com.synngate.synnframe.data.local.entity.LogEntity
import com.synngate.synnframe.data.local.entity.ProductEntity
import com.synngate.synnframe.data.local.entity.ProductUnitEntity
import com.synngate.synnframe.data.local.entity.ServerEntity
import com.synngate.synnframe.data.local.entity.SyncOperation
import com.synngate.synnframe.data.local.entity.TaskEntity
import com.synngate.synnframe.data.local.entity.TaskFactLineEntity
import com.synngate.synnframe.data.local.entity.TaskPlanLineEntity
import com.synngate.synnframe.data.local.entity.UserEntity
import com.synngate.synnframe.data.sync.SyncHistoryRecord

@Database(
    entities = [
        ServerEntity::class,
        UserEntity::class,
        LogEntity::class,
        ProductEntity::class,
        ProductUnitEntity::class,
        BarcodeEntity::class,
        TaskEntity::class,
        TaskPlanLineEntity::class,
        TaskFactLineEntity::class,
        SyncOperation::class,
        SyncHistoryRecord::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao

    abstract fun userDao(): UserDao

    abstract fun logDao(): LogDao

    abstract fun productDao(): ProductDao

    abstract fun taskDao(): TaskDao

    abstract fun syncOperationDao(): SyncOperationDao

    abstract fun syncHistoryDao(): SyncHistoryDao

    companion object {
        private const val DATABASE_NAME = "synnframe_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_3_4) // Добавляем миграцию
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Миграция для добавления колонки binCode
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем колонку binCode в таблицу task_plan_lines
                database.execSQL("ALTER TABLE task_plan_lines ADD COLUMN binCode TEXT")

                // Добавляем колонку binCode в таблицу task_fact_lines
                database.execSQL("ALTER TABLE task_fact_lines ADD COLUMN binCode TEXT")
            }
        }
    }
}