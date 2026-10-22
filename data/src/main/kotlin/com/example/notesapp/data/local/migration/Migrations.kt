package com.example.notesapp.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5: el amarillo que se guardaba como `SAND` pasa a llamarse `VANILLA`, y `SAND` queda para el beige «Arena».
 * Solo cambian datos: las notas amarillas conservan su color visible.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE notes SET color = 'VANILLA' WHERE color = 'SAND'")
    }
}

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_4_5)
