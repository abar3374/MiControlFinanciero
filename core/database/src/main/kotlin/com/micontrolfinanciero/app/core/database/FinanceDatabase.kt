package com.micontrolfinanciero.app.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.database.dao.BudgetDao
import com.micontrolfinanciero.app.core.database.dao.CategoryDao
import com.micontrolfinanciero.app.core.database.dao.ExpenseDao
import com.micontrolfinanciero.app.core.database.dao.FixedPaymentDao
import com.micontrolfinanciero.app.core.database.dao.IncomeDao
import com.micontrolfinanciero.app.core.database.dao.MovementDao
import com.micontrolfinanciero.app.core.database.dao.UserProfileDao
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.database.entity.BudgetEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseCategoryEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseEntity
import com.micontrolfinanciero.app.core.database.entity.FixedPaymentEntity
import com.micontrolfinanciero.app.core.database.entity.FixedPaymentRecordEntity
import com.micontrolfinanciero.app.core.database.entity.IncomeEntity
import com.micontrolfinanciero.app.core.database.entity.UserProfileEntity

/**
 * Base de datos de Finance.
 *
 * REGLA: cada cambio en las tablas sube [version] y trae su migración. Nunca se usa la
 * migración destructiva: tus datos deben sobrevivir a cada actualización.
 *
 * Historial:
 *  - v1 (0.2.0): user_profile.
 *  - v2 (0.3.0): expense_category, budget, expense, income, audit_log. Migración automática
 *    (solo agrega tablas nuevas; el perfil queda intacto).
 *  - v3 (0.4.0): fixed_payment, fixed_payment_record (pagos del mes). Migración automática.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        ExpenseCategoryEntity::class,
        BudgetEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class,
        AuditLogEntity::class,
        FixedPaymentEntity::class,
        FixedPaymentRecordEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun movementDao(): MovementDao
    abstract fun auditDao(): AuditDao
    abstract fun fixedPaymentDao(): FixedPaymentDao

    companion object {
        const val FILE_NAME = "finance.db"
    }
}
