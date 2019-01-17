package com.webiphany.workerstatusviadb

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*

class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Get out the UUID
        var uuid = inputData.getString(UUID)

        if (uuid != null) {
            doLongOperation(uuid)
            return Result.success()
        } else {
            return Result.failure()
        }
    }

    private fun doLongOperation(uuid: String) {
        var progress = 0
        var videoDao: VideoAssetDao? = null

        val db = VideoAssetDatabase.getDatabase(applicationContext)
        if (db != null) {
            videoDao = db.videoAssetDao()
        }

        while (progress < 100) {
            progress += (Random().nextFloat() * 10.0).toInt()

            try {
                Thread.sleep(1000)
            } catch (ie: InterruptedException) {

            }
            Log.d( MainActivity.TAG, "Updating progress for ${uuid}: ${progress}")
            videoDao?.updateProgressByUuid(uuid, progress)
        }
    }

    companion object {
        val UUID = "UUID"
    }
}