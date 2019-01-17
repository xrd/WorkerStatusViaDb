package com.webiphany.workerstatusviadb

import android.app.Application
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "video_table")
class VideoAsset {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "progress")
    var progress: Int = 0

    @ColumnInfo(name = "uuid")
    @NonNull
    var uuid: String? = null

}

class VideoAssetRepository(application: Application) {

    private var videoDao: VideoAssetDao? = null

    init {
        val db = VideoAssetDatabase.getDatabase(application)
        if (db != null) {
            videoDao = db.videoAssetDao()
        }
    }

    fun findAllVideos(): LiveData<List<VideoAsset>>? {
        if (videoDao != null) {
            return videoDao?.findAll()
        } else {
            Log.v(MainActivity.TAG, "DAO is null, fatal error")
            return null
        }
    }

    fun insert(video: VideoAsset) {
        insertAsyncTask(videoDao).execute(video)
    }

    fun get(id: String): LiveData<VideoAsset>? = videoDao?.findVideoAssetById(id)

    private class insertAsyncTask internal
    constructor(private val asyncTaskDao: VideoAssetDao?) :
            AsyncTask<VideoAsset, Void, Void>() {

        override fun doInBackground(vararg params: VideoAsset): Void? {
            asyncTaskDao?.insert(params[0])
            return null
        }
    }

    companion object {
        var instance: VideoAssetRepository? = null

        fun getInstance(application: Application): VideoAssetRepository? {
            synchronized(VideoAssetRepository::class) {
                if (instance == null) {
                    instance = VideoAssetRepository(application)
                }
            }
            return instance
        }
    }
}

@Database(entities = arrayOf(VideoAsset::class), version = 3)
abstract class VideoAssetDatabase : RoomDatabase() {

    abstract fun videoAssetDao(): VideoAssetDao

    companion object {

        @Volatile
        private var INSTANCE: VideoAssetDatabase? = null


        fun getDatabase(context: Context): VideoAssetDatabase? {
            if (INSTANCE == null) {
                synchronized(VideoAssetDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.applicationContext,
                                VideoAssetDatabase::class.java, "video_asset_database")
                                .build()
                    }
                }
            }
            return INSTANCE
        }
    }
}

@Dao
interface VideoAssetDao {

    @Insert
    fun insert(asset: VideoAsset)

    @Query("SELECT * from video_table")
    fun findAll(): LiveData<List<VideoAsset>>

    @Query("select * from video_table where id = :s limit 1")
    fun findVideoAssetById(s: String): LiveData<VideoAsset>

    @Query("select * from video_table where uuid = :uuid limit 1")
    fun findVideoAssetByUuid(uuid: String): LiveData<VideoAsset>

    @Query( "update video_table set progress = :p where uuid = :uuid")
    fun updateProgressByUuid(uuid: String, p: Int )
}