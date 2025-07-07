package com.synngate.synnframe.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Добавляем новые колонки в таблицу products
            database.execSQL("ALTER TABLE products ADD COLUMN weight REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE products ADD COLUMN maxQtyPerPallet REAL NOT NULL DEFAULT 0.0")
        }
    }
    
    // Список всех миграций
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_6_7
    )
}