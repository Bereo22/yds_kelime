package com.example.yds_kelime

import android.content.Context
import android.media.MediaPlayer

class SoundManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val prefs = context.getSharedPreferences("YDS_PREFS", Context.MODE_PRIVATE)
    
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) {
            prefs.edit().putBoolean("sound_enabled", value).apply()
        }
    
    fun playCorrectSound() {
        if (!soundEnabled) return
        playSound(R.raw.sound_correct)
    }
    
    fun playWrongSound() {
        if (!soundEnabled) return
        playSound(R.raw.sound_wrong)
    }
    
    fun playFlipSound() {
        if (!soundEnabled) return
        playSound(R.raw.sound_flip)
    }
    
    private fun playSound(resourceId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, resourceId)
            mediaPlayer?.setOnCompletionListener { 
                it.release()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
