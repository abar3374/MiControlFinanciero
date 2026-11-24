package com.micontrolfinanciero.app.core.data.di

import com.micontrolfinanciero.app.core.data.finance.RoomBudgetRepository
import com.micontrolfinanciero.app.core.data.finance.RoomCategoryRepository
import com.micontrolfinanciero.app.core.data.finance.RoomMovementRepository
import com.micontrolfinanciero.app.core.data.profile.FileProfilePhotoStorage
import com.micontrolfinanciero.app.core.data.profile.RoomUserProfileRepository
import com.micontrolfinanciero.app.core.domain.finance.BudgetRepository
import com.micontrolfinanciero.app.core.domain.finance.CategoryRepository
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
}
