package com.mistavinya.smac.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AudioPlayer(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: MediaPlayer? = null
        
        fun getMediaPlayer(): MediaPlayer {
            return INSTANCE ?: synchronized(this) {
                val instance = MediaPlayer()
                INSTANCE = instance
                instance
            }
        }
    }

    private var mediaPlayer: MediaPlayer = getMediaPlayer()

    fun play(filePath: String, onCompletion: () -> Unit) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayer", "File does not exist: $filePath")
            return
        }

        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, Uri.fromFile(file))
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                onCompletion()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play audio", e)
        }
    }

    fun pause() {
        if (mediaPlayer.isPlaying) mediaPlayer.pause()
    }

    fun resume() {
        if (!mediaPlayer.isPlaying) mediaPlayer.start()
    }

    fun stop() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        mediaPlayer.reset()
    }

    fun isPlaying(): Boolean = try { mediaPlayer.isPlaying } catch (e: Exception) { false }

    fun getCurrentPosition(): Int = try { mediaPlayer.currentPosition } catch (e: Exception) { 0 }

    fun getDuration(): Int = try { mediaPlayer.duration } catch (e: Exception) { 0 }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }
}
