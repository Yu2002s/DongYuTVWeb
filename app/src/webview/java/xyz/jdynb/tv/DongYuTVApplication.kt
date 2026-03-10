package xyz.jdynb.tv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.drake.brv.utils.BRV
import com.drake.engine.base.Engine
import com.drake.statelayout.StateConfig
import xyz.jdynb.tv.utils.CrashHandler
import java.io.File

class DongYuTVApplication : Application() {

  companion object {

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    private const val TAG = "DongYuTVApplication"

    @OptIn(UnstableApi::class)
    lateinit var cache: SimpleCache
  }

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    context = this
    CrashHandler.getInstance().init()

    Engine.initialize(this)
    BRV.modelId = BR.m

    StateConfig.apply {
      loadingLayout = R.layout.layout_loading
      errorLayout = R.layout.layout_error
      emptyLayout = R.layout.layout_empty

      // 设置重试id
      setRetryIds(R.id.error_msg)
      onError { error ->
        findViewById<TextView>(R.id.error_msg).text = if (error is Exception) error.message else "加载失败，请重试"
      }
      onEmpty { tag ->
        if (tag is String) {
          findViewById<TextView>(R.id.empty_tips).text = tag
        }
      }
      onContent {  }
      onLoading {  }
    }

    // 1. 创建缓存目录
    val cacheDir = File(cacheDir, "media_cache").apply { mkdirs() }

    // 2. 定义缓存驱逐策略，这里使用LRU策略，最大缓存3GB
    val cacheEvictor =
      LeastRecentlyUsedCacheEvictor(3000 * 1024 * 1024L) // 注意数值后面加 L 避免int溢出 [citation:1]

    // 3. 创建数据库提供者（SimpleCache需要），使用Media3提供的简单实现
    val databaseProvider = StandaloneDatabaseProvider(this)

    // 4. 构建 SimpleCache 实例
    cache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)
  }
}