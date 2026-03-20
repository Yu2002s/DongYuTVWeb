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
import java.io.File

/**
 * X5 内核
 */
class DongYuTVApplication : Application() {

  companion object {

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    private const val TAG = "DongYuTVApplication"

    private const val TARGET_X5_VERSION = 46719

    var isX5Init = false

    @OptIn(UnstableApi::class)
    lateinit var cache: SimpleCache
  }

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    context = this

    Engine.initialize(this)
    BRV.modelId = BR.m

    // 1. 创建缓存目录
    val cacheDir = File(cacheDir, "media_cache").apply { mkdirs() }

    // 2. 定义缓存驱逐策略，这里使用LRU策略，最大缓存3GB
    val cacheEvictor =
      LeastRecentlyUsedCacheEvictor(3000 * 1024 * 1024L) // 注意数值后面加 L 避免int溢出 [citation:1]

    // 3. 创建数据库提供者（SimpleCache需要），使用Media3提供的简单实现
    val databaseProvider = StandaloneDatabaseProvider(this)

    // 4. 构建 SimpleCache 实例
    cache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

    /*val map: MutableMap<String?, Any?> = HashMap()
    map.put(TbsCoreSettings.MULTI_PROCESS_ENABLE, 1)
    QbSdk.initTbsSettings(map)

    TbsFramework.setUp(this)

    val localPreInitCallback = object : QbSdk.PreInitCallback {
      override fun onCoreInitFinished() {
        Log.i(TAG, "onCoreInitFinished")
      }

      override fun onViewInitFinished(b: Boolean) {
        Log.i(TAG, "onViewInitFinished: $b")
        if (b) {
          isX5Init = true
          Toast.makeText(this@DongYuTVApplication, "X5内核初始化成功", Toast.LENGTH_SHORT).show()
        }
      }
    }

    QbSdk.enableX5WithoutRestart()
    val manager = DynamicInstallManager(this)
    manager.registerListener(object : ProgressListener {
      override fun onProgress(i: Int) {
        Log.i(TAG, "downloading: $i")
      }

      override fun onFinished() {
        Log.i(TAG, "onFinished")
        QbSdk.preInit(this@DongYuTVApplication, true, localPreInitCallback)
      }

      override fun onFailed(code: Int, msg: String?) {
        Log.i(TAG, "onError: $code; msg: $msg")
        Toast.makeText(this@DongYuTVApplication, "X5内核初始化失败: $msg", Toast.LENGTH_LONG).show()
      }
    })*/

    // yourAppNeedUpdateX5 是App自己来定义的条件，通常我们可以这样判断：
    // boolean yourAppNeedUpdateX5 = QbSdk.getTbsVersion(context) != TargetX5Version;
    // targetX5Version: config.tbs对应的内核版本号，试用的见 zip 里文件夹的数字后缀
    /*if (manager.needUpdateLicense() ||  QbSdk.getTbsVersion(this) != TARGET_X5_VERSION) {
      manager.startInstall()
      repeat(3) {
        Toast.makeText(this, "开始安装X5内核，请耐心等待...", Toast.LENGTH_LONG).show()
      }
    } else {
      QbSdk.preInit(this, true, localPreInitCallback)
    }*/
  }
}