package com.synngate.synnframe.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.synngate.synnframe.data.local.dao.LogDao
import com.synngate.synnframe.data.local.dao.ProductDao
import com.synngate.synnframe.data.local.dao.ServerDao
import com.synngate.synnframe.data.local.dao.TaskDao
import com.synngate.synnframe.data.local.dao.UserDao
import com.synngate.synnframe.data.local.entity.BarcodeEntity
import com.synngate.synnframe.data.local.entity.LogEntity
import com.synngate.synnframe.data.local.entity.ProductEntity
import com.synngate.synnframe.data.local.entity.ProductUnitEntity
import com.synngate.synnframe.data.local.entity.ServerEntity
import com.synngate.synnframe.data.local.entity.TaskEntity
import com.synngate.synnframe.data.local.entity.TaskFactLineEntity
import com.synngate.synnframe.data.local.entity.TaskPlanLineEntity
import com.synngate.synnframe.data.local.entity.UserEntity

/**
 * Основная база данных приложения
 */
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
        TaskFactLineEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DAO для работы с серверами
     */
    abstract fun serverDao(): ServerDao

    /**
     * DAO для работы с пользователями
     */
    abstract fun userDao(): UserDao

    /**
     * DAO для работы с логами
     */
    abstract fun logDao(): LogDao

    /**
     * DAO для работы с товарами
     */
    abstract fun productDao(): ProductDao

    /**
     * DAO для работы с заданиями
     */
    abstract fun taskDao(): TaskDao

    companion object {
        private const val DATABASE_NAME = "synnframe_database"

        /**
         * Синглтон для получения экземпляра базы данных
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Получение экземпляра базы данных
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}