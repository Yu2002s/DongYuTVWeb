package xyz.jdynb.tv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.drake.brv.utils.BRV
import com.drake.engine.base.Engine
import timber.log.Timber
import java.io.File

/**
 * X5 内核
 */
class DongYuTVApplication : Application() {

  companion object {

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    @OptIn(UnstableApi::class)
    lateinit var cache: SimpleCache
  }

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    context = this

    Engine.initialize(this)
    BRV.modelId = BR.m

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
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