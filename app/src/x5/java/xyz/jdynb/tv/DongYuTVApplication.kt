package xyz.jdynb.tv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.drake.brv.utils.BRV
import com.drake.engine.base.Engine
import com.hearthappy.x5core.X5CoreManager
import com.hearthappy.x5core.interfaces.X5CoreListener
import com.tencent.smtt.sdk.QbSdk
import kotlin.system.exitProcess

class DongYuTVApplication : Application() {

  companion object {

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    private const val TAG = "DongYuTVApplication"

    var isX5CoreInitFinished = false
  }

  override fun onCreate() {
    super.onCreate()
    context = this

    Engine.initialize(this)
    BRV.modelId = BR.m

    //在自定义的Application中初始化
    X5CoreManager.initX5Core(baseContext,listener = object : X5CoreListener {
      override fun onCoreInitFinished() {
        Log.d(TAG, "onCoreInitFinished: ")
      }

      override fun onViewInitFinished(isX5: Boolean) {
        Log.d(TAG, "onViewInitFinished: $isX5")
        isX5CoreInitFinished = isX5
        if (isX5) {
          Toast.makeText(this@DongYuTVApplication, "X5内核初始化成功", Toast.LENGTH_LONG).show()
        }
      }

      override fun onInstallFinish(stateCode: Int) {
        Log.d(TAG, "onInstallFinish: $stateCode")
        //stateCode返回200则安装成功,需要重启app生效
        if (stateCode == 200) {
          Handler(Looper.getMainLooper()).postDelayed({
            exitProcess(0)
          }, 5000)
          Toast.makeText(this@DongYuTVApplication, "X5内核安装成功，5秒后请重新打开App", Toast.LENGTH_LONG).show()
        }
      }
    })
  }
}