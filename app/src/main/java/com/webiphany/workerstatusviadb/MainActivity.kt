package com.webiphany.workerstatusviadb

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.*

class MainActivity : AppCompatActivity() {

    private var videoAssetsViewModel: VideoAssetViewModel? = null
    private var adapter: VideoAssetsAdapter? = null
    val networkScope = CoroutineScope(newSingleThreadContext("NetworkThread"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { _ ->
            uploadNewVideo()
        }
        videoAssetsViewModel = ViewModelProviders.of(this).get(VideoAssetViewModel::class.java)
        setupPreviewImages()
    }

    private suspend fun addNewVideo() {
        val videoAsset = VideoAsset()
        videoAsset.uuid = Integer.toHexString(Random().nextInt() * 10000)
        videoAssetsViewModel?.insert(videoAsset)

        var uuid = videoAsset.uuid
        var progress = 0
        var videoDao: VideoAssetDao? = null

        val db = VideoAssetDatabase.getDatabase(applicationContext)
        if (db != null) {
            videoDao = db.videoAssetDao()
        }

        while (progress < 100) {
            progress += (Random().nextFloat() * 10.0).toInt()

            delay(1000)

            Log.d( MainActivity.TAG, "Updating progress for ${uuid}: ${progress}")
            if (uuid != null) {
                videoDao?.updateProgressByUuid(uuid, progress)
            }
        }
    }

    private fun uploadNewVideo() {
        val videoAsset = VideoAsset()
        videoAsset.uuid = Integer.toHexString(Random().nextInt() * 10000)
        videoAssetsViewModel?.insert(videoAsset)

        // Create a new worker, add to the items
        val uploadRequestBuilder = OneTimeWorkRequest.Builder(UploadWorker::class.java)
        val data = Data.Builder()

        data.putString(UploadWorker.UUID, videoAsset.uuid)
        uploadRequestBuilder.setInputData(data.build())
        val uploadRequest = uploadRequestBuilder.build()
        WorkManager.getInstance().enqueue(uploadRequest)
    }

    private fun setupPreviewImages() {
        val mLayoutManager = GridLayoutManager(this, 4)
        previewImagesRecyclerView.layoutManager = mLayoutManager
        adapter = VideoAssetsAdapter(this,videoAssetsViewModel?.videos)
        previewImagesRecyclerView.adapter = adapter
    }

    inner class VideoAssetViewHolder(videoView: View) : RecyclerView.ViewHolder(videoView) {
        var progressText: TextView
        var uuidText: TextView

        init {
            uuidText = videoView.findViewById(R.id.uuid)
            progressText = videoView.findViewById(R.id.progress)
        }
    }

    inner class VideoAssetsAdapter(
            lifecycle: LifecycleOwner,
            val videos: LiveData<List<VideoAsset>>?) :
            RecyclerView.Adapter<VideoAssetViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): VideoAssetViewHolder {
            return VideoAssetViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preview_image, parent, false))
        }

        init {
            videos?.observe(lifecycle, Observer {
                /**
                 * rebuild list when data changes. a better implementation would use DiffUtils here :-)
                 */
                notifyDataSetChanged()
            })
        }

        override fun onBindViewHolder(holder: VideoAssetViewHolder, position: Int) {
            val video = videos?.value?.get(position)
            if (video != null && videoAssetsViewModel != null) {
                val uuid = video.uuid
                if( uuid != null ) {
                    holder.uuidText.text = uuid
                    holder.progressText.text = "${video.progress}%"
                }
            }
        }

        override fun getItemCount() = videos?.value?.size ?: 0
    }

    companion object {
        var TAG: String = "WSVDB"
    }
}
