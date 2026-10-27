package com.rosterx.di

import com.rosterx.data.repository.RosterRepositoryImpl
import com.rosterx.domain.repository.RosterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRosterRepository(
        rosterRepositoryImpl: RosterRepositoryImpl
    ): RosterRepository
}
