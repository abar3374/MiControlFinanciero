package com.micontrolfinanciero.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.micontrolfinanciero.app.core.database.FinanceDatabase
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

    @Provides
    fun provideUserProfileDao(database: FinanceDatabase): UserProfileDao = database.userProfileDao()
}
