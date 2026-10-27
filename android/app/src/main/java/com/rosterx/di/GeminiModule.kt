package com.rosterx.di

import com.rosterx.domain.service.GeminiRosterService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    private const val GEMINI_MODEL = "gemini-1.5-flash"

    @Provides
    @Singleton
    fun provideGeminiRosterService(): GeminiRosterService {
        val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
        return GeminiRosterService(
            apiKey = apiKey,
            modelName = GEMINI_MODEL
        )
    }
}
