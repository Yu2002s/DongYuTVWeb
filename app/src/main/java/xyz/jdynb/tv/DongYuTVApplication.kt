package xyz.jdynb.tv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.drake.brv.utils.BRV
import com.drake.engine.base.Engine
import com.norman.webviewup.lib.UpgradeCallback
import com.norman.webviewup.lib.WebViewUpgrade
import com.norman.webviewup.lib.source.UpgradeAssetSource
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.ProgressListener
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsFramework
import com.tencent.smtt.sdk.core.dynamicinstall.DynamicInstallManager
import java.io.File
import kotlin.system.exitProcess


class DongYuTVApplication : Application() {

  companion object {

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    private const val TAG = "DongYuTVApplication"

  }

  override fun onCreate() {
    super.onCreate()
    context = this

    Engine.initialize(this)
    BRV.modelId = BR.m
    val map: MutableMap<String?, Any?> = HashMap()
    map.put(TbsCoreSettings.MULTI_PROCESS_ENABLE, 1)
    QbSdk.initTbsSettings(map)

    TbsFramework.setUp(this)
    QbSdk.enableX5WithoutRestart()

    val localPreInitCallback = object : QbSdk.PreInitCallback {
      override fun onCoreInitFinished() {
        Log.i(TAG, "onCoreInitFinished")
      }

      override fun onViewInitFinished(p0: Boolean) {
        Log.i(TAG, "onViewInitFinished: $p0")
      }
    }

    val manager = DynamicInstallManager(this)
    manager.registerListener(object : ProgressListener {
      override fun onProgress(i: Int) {
        // 下载安装过程回调，0-100
        Log.i(TAG, "progress: $i")
      }

      override fun onFinished() {
        // 下载安装授权完成，可以使用X5了
        Log.i(TAG, "onFinished")
        // 预初始化X5内核
        QbSdk.preInit(this@DongYuTVApplication, true, localPreInitCallback)
      }

      override fun onFailed(code: Int, msg: String?) {
        // 过程失败
        Log.i(TAG, "onError: $code; msg: $msg")
      }
    })

    val appNeedUpdateX5 = QbSdk.getTbsVersion(context) != 46719

    Log.i(TAG, "TabsVersion: ${QbSdk.getTbsVersion(context)}")

    if (manager.needUpdateLicense() || appNeedUpdateX5) {
      Log.i(TAG, "startInstall")
      manager.startInstall()
    } else {
      Log.i(TAG, "预初始化X5内核")
      QbSdk.preInit(this@DongYuTVApplication, true, localPreInitCallback)
    }
  }
}