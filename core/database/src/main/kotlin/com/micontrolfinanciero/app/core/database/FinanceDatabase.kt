package com.micontrolfinanciero.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.micontrolfinanciero.app.core.database.dao.UserProfileDao
import com.micontrolfinanciero.app.core.database.entity.UserProfileEntity

/**
 * Base de datos de Finance.
 *
 * REGLA: cada cambio en las tablas sube [version] y trae su Migration con prueba.
 * Nunca se usa la migración destructiva: tus datos deben sobrevivir a cada actualización.
 *
 * Versión 1 (0.2.0): perfil del usuario. Las tablas de gastos, ingresos, presupuestos y
 * créditos llegan en las siguientes versiones.
 */
@Database(
    entities = [UserProfileEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        const val FILE_NAME = "finance.db"
    }
}
