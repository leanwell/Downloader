package com.leanwell.downloader

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloaderApp : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        lateinit var instance: DownloaderApp
            private set

        private val _isEngineReady = MutableStateFlow(false)
        val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.leanwell.downloader.settings.AppSettings.init(this)

        applicationScope.launch {
            initEngines()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    private fun initEngines() {
        try {
            Log.d("DownloaderApp", "Initializing YoutubeDL, FFmpeg, and Aria2c...")
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Aria2c.getInstance().init(this)
            _isEngineReady.value = true
            Log.d("DownloaderApp", "Engines initialized successfully!")
        } catch (e: Exception) {
            Log.e("DownloaderApp", "Failed to initialize downloader engines", e)
        }
    }
}
