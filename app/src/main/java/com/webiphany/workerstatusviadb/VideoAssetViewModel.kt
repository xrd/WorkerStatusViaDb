package com.webiphany.workerstatusviadb

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import java.util.concurrent.Executors

class VideoAssetViewModel(application: Application) : AndroidViewModel(application) {

    private val videoAssetRepository: VideoAssetRepository?
    var videos: LiveData<List<VideoAsset>>? = null

    private val executorService = Executors.newSingleThreadExecutor()

    init {
        videoAssetRepository = VideoAssetRepository.getInstance(application)
        videos = videoAssetRepository?.findAllVideos()
    }

    fun getByUuid(id: String) = videoAssetRepository?.get(id)

    fun insert(video: VideoAsset) {
        executorService.execute {
            videoAssetRepository?.insert(video)
        }
    }

}
