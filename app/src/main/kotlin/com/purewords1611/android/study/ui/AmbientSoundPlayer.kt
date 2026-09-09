package com.purewords1611.android.study.ui

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import com.purewords1611.android.study.data.AmbientSoundscape
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AmbientSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSoundscape: AmbientSoundscape = AmbientSoundscape.SCRIPTORIUM

    fun toggleScriptoriumMode(enabled: Boolean, soundscape: AmbientSoundscape = AmbientSoundscape.SCRIPTORIUM) {
        currentSoundscape = soundscape
        if (enabled) {
            start(soundscape)
        } else {
            stop()
        }
    }

    fun setSoundscape(soundscape: AmbientSoundscape) {
        val wasPlaying = mediaPlayer?.isPlaying == true
        if (wasPlaying && currentSoundscape != soundscape) {
            stop()
            currentSoundscape = soundscape
            start(soundscape)
        } else {
            currentSoundscape = soundscape
        }
    }

    private fun start(soundscape: AmbientSoundscape) {
        if (mediaPlayer?.isPlaying == true) return
        
        try {
            val resId = context.resources.getIdentifier(soundscape.resName, "raw", context.packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context, resId).apply {
                    isLooping = true
                    start()
                }
                android.util.Log.i("AmbientSoundPlayer", "Starting Ambient Mode: ${soundscape.label}")
            } else {
                android.util.Log.w("AmbientSoundPlayer", "Resource not found (R.raw.${soundscape.resName}). Placeholder mode active.")
                // Fallback to scriptorium if it exists and we're not already trying it
                if (soundscape != AmbientSoundscape.SCRIPTORIUM) {
                    start(AmbientSoundscape.SCRIPTORIUM)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AmbientSoundPlayer", "Failed to start ambient mode", e)
        }
    }

    private fun stop() {
        android.util.Log.i("AmbientSoundPlayer", "Stopping Scriptorium Ambient Mode")
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: Exception) {
                // Ignore if already stopped
            } finally {
                it.release()
            }
        }
        mediaPlayer = null
    }
}
