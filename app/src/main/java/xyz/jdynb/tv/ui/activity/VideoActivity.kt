package xyz.jdynb.tv.ui.activity

import android.content.Intent
import android.net.http.UrlRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.NonNull
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineActivity
import com.drake.engine.utils.EncryptUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import xyz.jdynb.tv.DongYuTVApplication
import xyz.jdynb.tv.R
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.databinding.ActivityVideoBinding
import xyz.jdynb.tv.databinding.ItemSelectionBinding
import xyz.jdynb.tv.model.MovieModel
import xyz.jdynb.tv.model.VideoProgressModel
import xyz.jdynb.tv.model.response.XMResolveResponse
import xyz.jdynb.tv.utils.AESUtils
import xyz.jdynb.tv.utils.EncryptUtils.md5
import xyz.jdynb.tv.utils.NetworkUtils
import xyz.jdynb.tv.utils.getSerializableForKey
import xyz.jdynb.tv.utils.putSerializable
import java.io.File
import java.io.IOException
import java.net.URLEncoder


class VideoActivity : EngineActivity<ActivityVideoBinding>(R.layout.activity_video),
  Player.Listener {

  private lateinit var player: Player

  private lateinit var movieModel: MovieModel

  private lateinit var selectionRv: RecyclerView

  private lateinit var title: TextView

  private lateinit var currentVideoProgressFile: File

  private var videoProgressModel: VideoProgressModel = VideoProgressModel()

  private var isRunningProgress = false

  private var isShowControl = false

  private val handler = Handler(Looper.getMainLooper())

  companion object {
    private const val TAG = "VideoActivity"

    private const val PROGRESS_UPDATE_INTERVAL = 5000L

    private const val MAX_SAVE_PROGRESS_COUNT = 2

    @JvmStatic
    fun play(movieModel: MovieModel) {
      Log.d(TAG, "play: ${movieModel.url}")
      val intent = Intent(DongYuTVApplication.context, VideoActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putSerializable("movieModel", movieModel)
      }
      DongYuTVApplication.context.startActivity(intent)
    }
  }

  override fun initData() {
    movieModel = intent.extras?.getSerializableForKey<MovieModel>("movieModel") as MovieModel

    lifecycleScope.launch {
      withContext(Dispatchers.IO) {
        try {
          val videoProgressFile = File(cacheDir, "video_progress")
          if (!videoProgressFile.exists()) {
            videoProgressFile.mkdirs()
          }
          val files = videoProgressFile.listFiles() ?: arrayOf()
          Log.d(TAG, "fileCount: ${files.size}, maxCount: $MAX_SAVE_PROGRESS_COUNT")
          if (files.size > MAX_SAVE_PROGRESS_COUNT) {
            // 按修改时间排序（从旧到新），删除最旧的文件
            files.sortedBy { it.lastModified() }
              .take(files.size - MAX_SAVE_PROGRESS_COUNT)  // 取最旧的 N 个文件（需要删除的数量）
              .forEach { 
                Log.d(TAG, "删除旧的进度文件：${it.name}")
                it.delete() 
              }
          }
          currentVideoProgressFile = File(
            videoProgressFile, EncryptUtil.encryptMD5ToString(
              movieModel.url?.ifEmpty { movieModel.id } ?: movieModel.id))
          if (!currentVideoProgressFile.exists()) {
            currentVideoProgressFile.createNewFile()
          } else {
            val videoProgressContent = currentVideoProgressFile.readText()
            if (videoProgressContent.isNotEmpty()) {
              videoProgressModel =
                NetworkUtils.json.decodeFromString<VideoProgressModel>(videoProgressContent)
              Log.i(TAG, "videoProgressModel: $videoProgressModel")
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "initData: ", e)
        }
      }

      selectionRv.models =
        movieModel.items ?: listOf(MovieModel.Item(name = "1", url = movieModel.url!!))
      Log.i(TAG, "movieModel: $movieModel")

      if (movieModel.items.isNullOrEmpty()) {
        movieModel.url?.let { originalUrl ->
          // 如果有 URL，创建媒体源（ResolvingDataSource 会自动解析）
          val mediaItem = MediaItem.fromUri(originalUrl)
          player.setMediaItem(mediaItem)
          if (videoProgressModel.currentProgress > 0) {
            player.seekTo(videoProgressModel.currentProgress)
          }
          player.prepare()
        }
        title.text = movieModel.title
      } else {
        // 有多个播放源时，为每个 item 创建媒体源
        val mediaItems = movieModel.items?.map { item ->
          item.url.let { MediaItem.fromUri(it) }
        } ?: emptyList()
        player.setMediaItems(
          mediaItems,
          videoProgressModel.currentIndex,
          videoProgressModel.currentProgress
        )
        player.prepare()
      }
    }
  }

  @UnstableApi
  override fun initView() {

    binding.playerView.setShowSubtitleButton(true)
    // binding.playerView.setShowRewindButton(true)
    binding.playerView.setShowPlayButtonIfPlaybackIsSuppressed(true)
    binding.playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
      isShowControl = visibility == PlayerControlView.VISIBLE
    })
    createVideoPlayer()
    player.addListener(this)

    title = findViewById(R.id.exo_title)
    selectionRv = findViewById(R.id.rv_selection)
    selectionRv.divider {
      setDivider(8, true)
      orientation = DividerOrientation.GRID
      includeVisible = true
    }.setup {
      singleMode = true
      addType<MovieModel.Item>(R.layout.item_selection)

      onChecked { position, checked, allChecked ->
        val model = getModel<MovieModel.Item>(position)
        model.checked = checked
        if (!checked) {
          return@onChecked
        }
        videoProgressModel.currentIndex = position
        title.text = movieModel.title + " - " + model.name
        stopProgress()
        saveProgress()
      }

      R.id.selection.onClick {
        setChecked(modelPosition, true)
        player.seekTo(modelPosition, 0)
      }
    }

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (isShowControl) {
          binding.playerView.hideController()
        } else {
          finish()
        }
      }
    })
  }

  @UnstableApi
  private fun createVideoPlayer() {
    // 1. 创建 HTTP 数据源工厂，配置连接参数
    val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
      setConnectTimeoutMs(5000) // 连接超时 5 秒
      setReadTimeoutMs(8000)    // 读取超时 8 秒
      setKeepPostFor302Redirects(true)  // 保持 POST 请求重定向
      setAllowCrossProtocolRedirects(true) // 允许跨协议重定向
    }

    // 2. 创建缓存数据源工厂（注意：缓存应该在最外层）
    val cacheDataSourceFactory = CacheDataSource.Factory()
      .setCache(DongYuTVApplication.cache)
      .setUpstreamDataSourceFactory(httpDataSourceFactory)
      .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    // 3. 创建 URL 解析数据源工厂，用于 API 签名和解密（包装在缓存之上）
    val resolvingDataSourceFactory = ResolvingDataSource.Factory(
      cacheDataSourceFactory,
      ApiUriResolve()
    )

    // 4. 创建 HLS 媒体源工厂，使用解析后的数据源
    val hlsMediaSourceFactory = HlsMediaSource.Factory(resolvingDataSourceFactory)

    // 5. 构建优化的 ExoPlayer，配置播放参数
    player = ExoPlayer.Builder(this, hlsMediaSourceFactory)
      .setSeekBackIncrementMs(10 * 1000L)  // 快退 10 秒
      .setSeekForwardIncrementMs(10 * 1000L) // 快进 10 秒
      .setHandleAudioBecomingNoisy(true) // 处理音频中断（耳机拔出等）
      .setWakeMode(C.WAKE_MODE_LOCAL) // 保持 CPU 唤醒，防止播放时休眠
      .build()
    binding.playerView.player = player
    player.playWhenReady = true
    binding.playerView.hideController()
  }

  @UnstableApi
  private class ApiUriResolve : ResolvingDataSource.Resolver {

    @Throws(Exception::class)
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
      // 只处理 HTTP/HTTPS 请求
      if (dataSpec.uri.scheme != "http" && dataSpec.uri.scheme != "https") {
        return dataSpec
      }

      // 如果是已经解析过的真实地址（包含特定域名或路径），直接返回
      val uriString = dataSpec.uri.toString()
      // Log.i(TAG, "原始请求 URI: $uriString")

      // 检查是否是 m3u8 或 ts 文件请求
      val isM3u8 = uriString.contains(".m3u8", ignoreCase = true)
      val isTS = uriString.contains(".ts", ignoreCase = true)

      if (isTS || isM3u8 || uriString.contains("cdn.hls.one")) {
        return dataSpec
      }

      // 只对初始的 m3u8 请求进行 API 解析，后续的 ts 分片不解析
      /*if (!isM3u8 || uriString.startsWith("http://")) { // 假设真实地址是 http 开头
        Log.d(TAG, "非 m3u8 请求或已是真实地址，跳过解析")
        return dataSpec
      }*/

      try {
        val urlEncoded = URLEncoder.encode(uriString, "UTF-8")
        val now = System.currentTimeMillis()
        val key = "$now$urlEncoded".md5()

        // Log.i(TAG, "key: $key")
        val sign = AESUtils.encrypt(key, key.md5(), "fUU9eRmkYzsgbkEK") ?: ""
        val formBody = FormBody.Builder()
          .add("tm", now.toString())
          .add("url", urlEncoded)
          .add("key", key)
          .add("sign", sign)
          .build()
        val request = Request.Builder()
          .url("https://202.189.8.170/Api")
          .post(formBody)
          .build()
        val result = NetworkUtils.requestBlock<XMResolveResponse>(request).getOrThrow()

        val decrypt = AESUtils.decrypt(result.data, result.key, result.iv)
          ?: throw NullPointerException("decrypt is null")
        // Log.i(TAG, "decrypt: $decrypt")
        val json = decrypt.replace("tg:@xmflv", "")
          .replace("\u0003\u0003\u0003", "")
        // Log.i(TAG, "json: $json")
        val jsonObject = JSONObject(json)
        val url = jsonObject.getString("url")
        Log.i(TAG, "解析后的真实 URL: $url")

        // 使用真实 URL 重建 DataSpec
        return dataSpec.buildUpon()
          .setUri(url)
          .build()
      } catch (e: Exception) {
        Log.e(TAG, "API 解析失败", e)
        throw e
      }
    }
  }

  private val progressRunnable = object : Runnable {
    override fun run() {
      val currentProgress = player.currentPosition
      setProgress(currentProgress)
      Log.i(TAG, "currentProgress: $currentProgress")
      if (isRunningProgress) {
        handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
      } else {
        handler.removeCallbacks(this)
      }
    }
  }

  private fun runProgress() {
    if (isRunningProgress) {
      Log.i(TAG, "isRunningProgress")
      return
    }
    isRunningProgress = true
    Log.i(TAG, "runProgress")
    handler.postDelayed(progressRunnable, PROGRESS_UPDATE_INTERVAL)
  }

  private fun setProgress(progress: Long) {
    videoProgressModel.currentProgress = progress
    saveProgress()
  }

  private fun saveProgress() {
    try {
      currentVideoProgressFile.writeText(NetworkUtils.json.encodeToString(videoProgressModel))
    } catch (e: IOException) {
      Log.e(TAG, "saveProgress Error", e)
    }
  }

  private fun stopProgress() {
    isRunningProgress = false
    handler.removeCallbacks(progressRunnable)
  }

  override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    Log.i(
      TAG,
      "onMediaItemTransition: ${mediaItem?.localConfiguration?.uri}, currentMediaItemIndex: ${player.currentMediaItemIndex}"
    )
    selectionRv.bindingAdapter.setChecked(player.currentMediaItemIndex, true)
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    super.onIsPlayingChanged(isPlaying)
    if (isPlaying) {
      runProgress()
    } else {
      stopProgress()
    }
  }

  override fun onResume() {
    super.onResume()
    if (::player.isInitialized && !player.isPlaying) {
      player.play()
    }
  }

  override fun onStop() {
    super.onStop()
    if (player.isPlaying) {
      player.pause()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.removeCallbacksAndMessages(null)
    player.release()
  }
}