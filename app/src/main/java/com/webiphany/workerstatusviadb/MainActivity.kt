package com.webiphany.workerstatusviadb

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var videoAssetsViewModel: VideoAssetViewModel? = null
    private var adapter: VideoAssetsAdapter? = null

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
        adapter = VideoAssetsAdapter(videoAssetsViewModel?.videos?.value)
        previewImagesRecyclerView.adapter = adapter

        videoAssetsViewModel?.videos?.observe(this, androidx.lifecycle.Observer { t ->
            if( t != null ){
                if (t.size > 0 ){
                    adapter?.setVideos(t)
                    previewImagesRecyclerView.adapter = adapter
                }
            }
        })
    }

    inner class VideoAssetViewHolder(videoView: View) : RecyclerView.ViewHolder(videoView) {
        var progressText: TextView
        var uuidText: TextView

        init {
            uuidText = videoView.findViewById(R.id.uuid)
            progressText = videoView.findViewById(R.id.progress)
        }
    }

    inner class VideoAssetsAdapter(private var videos: List<VideoAsset>?) :
            RecyclerView.Adapter<VideoAssetViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): VideoAssetViewHolder {
            return VideoAssetViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preview_image, parent, false))
        }

        override fun onBindViewHolder(holder: VideoAssetViewHolder, position: Int) {
            val video = videos?.get(position)

            if (video != null && videoAssetsViewModel != null) {
                val uuid = video.uuid
                if( uuid != null ) {
                    holder.uuidText.text = uuid

                    // Get the livedata to observe and change
                    val living = videoAssetsViewModel?.getByUuid(uuid)

                    living?.observe(this@MainActivity, androidx.lifecycle.Observer { v ->
                        // Got a change, do something with it.
                        if (v != null) {
                            holder.progressText.text = "${v.progress}%"
                        }
                        else {
                            Log.d( TAG, "Video is null, WHY?")
                        }
                    })
                }
            }
        }

        fun setVideos(t: List<VideoAsset>?) {
            videos = t
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            var size = 0
            if (videos != null) {
                size = videos?.size!!
            }
            return size
        }
    }

    companion object {
        var TAG: String = "WSVDB"
    }
}
