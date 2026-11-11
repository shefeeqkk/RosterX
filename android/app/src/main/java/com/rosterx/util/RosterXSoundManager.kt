package com.rosterx.util

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.util.Log
import com.rosterx.R

/**
 * High-performance sound manager for Project RosterX.
 * Provides instant, zero-latency playback of the custom Warm Crystal Chime
 * across foreground push arrivals and in-app alert notifications.
 */
object RosterXSoundManager {
    private const val TAG = "RosterXSoundManager"

    @Volatile
    private var soundPool: SoundPool? = null
    @Volatile
    private var chimeSoundId: Int = 0
    @Volatile
    private var isLoaded: Boolean = false
    private val lock = Any()

    /**
     * Resolves the canonical android.resource URI for NotificationChannel and NotificationCompat.
     */
    fun getNotificationSoundUri(context: Context): Uri {
        return Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.rosterx_chime}"
        )
    }

    /**
     * Preloads the custom chime into low-latency SoundPool.
     */
    fun preload(context: Context) {
        initSoundPool(context)
    }

    private fun initSoundPool(context: Context) {
        if (soundPool == null) {
            synchronized(lock) {
                if (soundPool == null) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

                    val pool = SoundPool.Builder()
                        .setMaxStreams(3)
                        .setAudioAttributes(audioAttributes)
                        .build()

                    pool.setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0 && sampleId == chimeSoundId) {
                            isLoaded = true
                            Log.d(TAG, "RosterX Crystal Chime preloaded successfully in SoundPool")
                        }
                    }

                    try {
                        chimeSoundId = pool.load(context.applicationContext, R.raw.rosterx_chime, 1)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load chime into SoundPool", e)
                    }

                    soundPool = pool
                }
            }
        }
    }

    /**
     * Plays the soothing tiny Warm Crystal Chime.
     * Uses SoundPool for zero-latency, with seamless MediaPlayer fallback.
     */
    fun playNotificationChime(context: Context) {
        try {
            initSoundPool(context)
            val pool = soundPool
            if (pool != null && isLoaded && chimeSoundId != 0) {
                pool.play(chimeSoundId, 0.90f, 0.90f, 1, 0, 1.0f)
            } else {
                MediaPlayer.create(context.applicationContext, R.raw.rosterx_chime)?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setOnCompletionListener { mp -> mp.release() }
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing notification chime", e)
        }
    }
}
