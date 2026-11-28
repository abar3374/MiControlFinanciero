package com.micontrolfinanciero.app.core.data.di

import com.micontrolfinanciero.app.core.data.finance.RoomAttachmentRepository
import com.micontrolfinanciero.app.core.data.finance.RoomBudgetRepository
import com.micontrolfinanciero.app.core.data.finance.RoomReportRepository
import com.micontrolfinanciero.app.core.data.settings.RoomSettingsRepository
import com.micontrolfinanciero.app.core.data.finance.RoomCategoryRepository
import com.micontrolfinanciero.app.core.data.finance.RoomFixedPaymentRepository
import com.micontrolfinanciero.app.core.data.finance.RoomMovementRepository
import com.micontrolfinanciero.app.core.data.profile.FileProfilePhotoStorage
import com.micontrolfinanciero.app.core.data.profile.RoomUserProfileRepository
import com.micontrolfinanciero.app.core.domain.finance.AttachmentRepository
import com.micontrolfinanciero.app.core.domain.finance.BudgetRepository
import com.micontrolfinanciero.app.core.domain.finance.ReportRepository
import com.micontrolfinanciero.app.core.domain.finance.SettingsRepository
import com.micontrolfinanciero.app.core.domain.finance.CategoryRepository
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentRepository
import com.micontrolfinanciero.app.core.domain.finance.MovementRepository
import com.micontrolfinanciero.app.core.domain.profile.ProfilePhotoStorage
import com.micontrolfinanciero.app.core.domain.profile.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Le dice a Hilt qué implementación usar para cada interfaz del dominio. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindUserProfileRepository(impl: RoomUserProfileRepository): UserProfileRepository

    @Binds
    abstract fun bindProfilePhotoStorage(impl: FileProfilePhotoStorage): ProfilePhotoStorage

    @Binds
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindBudgetRepository(impl: RoomBudgetRepository): BudgetRepository

    @Binds
    abstract fun bindMovementRepository(impl: RoomMovementRepository): MovementRepository

    @Binds
    abstract fun bindFixedPaymentRepository(impl: RoomFixedPaymentRepository): FixedPaymentRepository

    @Binds
    abstract fun bindAttachmentRepository(impl: RoomAttachmentRepository): AttachmentRepository

    @Binds
    abstract fun bindReportRepository(impl: RoomReportRepository): ReportRepository

    @Binds
    abstract fun bindSettingsRepository(impl: RoomSettingsRepository): SettingsRepository
}
