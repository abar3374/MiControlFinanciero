package com.micontrolfinanciero.app.core.data.di

import com.micontrolfinanciero.app.core.data.profile.FileProfilePhotoStorage
import com.micontrolfinanciero.app.core.data.profile.RoomUserProfileRepository
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
}
