package com.micontrolfinanciero.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.micontrolfinanciero.app.core.database.FinanceDatabase
import com.micontrolfinanciero.app.core.database.dao.AttachmentDao
import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.database.dao.ExpectedIncomeDao
import com.micontrolfinanciero.app.core.database.dao.NotificationLogDao
import com.micontrolfinanciero.app.core.database.dao.ReportDao
import com.micontrolfinanciero.app.core.database.dao.SettingsDao
import com.micontrolfinanciero.app.core.database.dao.BudgetDao
import com.micontrolfinanciero.app.core.database.dao.CategoryDao
import com.micontrolfinanciero.app.core.database.dao.ExpenseDao
import com.micontrolfinanciero.app.core.database.dao.FixedPaymentDao
import com.micontrolfinanciero.app.core.database.dao.IncomeDao
import com.micontrolfinanciero.app.core.database.dao.MovementDao
import com.micontrolfinanciero.app.core.database.dao.UserProfileDao
import com.micontrolfinanciero.app.core.security.DatabasePassphraseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/** Crea la base de datos cifrada una sola vez para toda la app. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: DatabasePassphraseProvider,
    ): FinanceDatabase {
        // Carga el motor nativo de SQLCipher antes de abrir la base.
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(passphraseProvider.getOrCreatePassphrase())
        return Room.databaseBuilder(context, FinanceDatabase::class.java, FinanceDatabase.FILE_NAME)
            .openHelperFactory(factory)
            .build()
    }

    @Provides fun provideUserProfileDao(db: FinanceDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideCategoryDao(db: FinanceDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideBudgetDao(db: FinanceDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideExpenseDao(db: FinanceDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideIncomeDao(db: FinanceDatabase): IncomeDao = db.incomeDao()
    @Provides fun provideMovementDao(db: FinanceDatabase): MovementDao = db.movementDao()
    @Provides fun provideAuditDao(db: FinanceDatabase): AuditDao = db.auditDao()
    @Provides fun provideFixedPaymentDao(db: FinanceDatabase): FixedPaymentDao = db.fixedPaymentDao()
    @Provides fun provideExpectedIncomeDao(db: FinanceDatabase): ExpectedIncomeDao = db.expectedIncomeDao()
    @Provides fun provideAttachmentDao(db: FinanceDatabase): AttachmentDao = db.attachmentDao()
    @Provides fun provideSettingsDao(db: FinanceDatabase): SettingsDao = db.settingsDao()
    @Provides fun provideNotificationLogDao(db: FinanceDatabase): NotificationLogDao = db.notificationLogDao()
    @Provides fun provideReportDao(db: FinanceDatabase): ReportDao = db.reportDao()
}
