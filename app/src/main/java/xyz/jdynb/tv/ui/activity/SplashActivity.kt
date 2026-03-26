package xyz.jdynb.tv.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.drake.engine.base.EngineActivity
import com.tencent.smtt.sdk.QbSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.MainActivity
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.ActivitySplashBinding
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.utils.X5Utils

class SplashActivity : EngineActivity<ActivitySplashBinding>(R.layout.activity_splash) {

  @SuppressLint("ObsoleteSdkInt")
  override fun init() {
    super.init()

   /* finish()
    startActivity(Intent(this, WebVideoActivity::class.java))

    return*/

    val isInstallX5 = SPKeyConstants.IS_INSTALL_X5.getRequired(false)

    val flavor = BuildConfig.FLAVOR

    val canLoadX5 = QbSdk.canLoadX5(this)

    Timber.i("isInstallX5:$isInstallX5, flavor:$flavor loadX5: $canLoadX5")

    // Android 13 以上不让安装X5内核
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU || flavor == "webview" || isInstallX5 || canLoadX5) {
      if (isInstallX5 || canLoadX5) {
        // 原来每次都要初始化？
        QbSdk.initX5Environment(this, object : QbSdk.PreInitCallback {
          override fun onCoreInitFinished() {
          }

          override fun onViewInitFinished(status: Boolean) {
            Timber.i(" onViewInitFinished is $status, ${QbSdk.isTbsCoreInited()}")
            enterHome()
          }
        })
      } else {
        enterHome()
      }
      return
    }

    lifecycleScope.launch {
      Timber.i("install X5")
      X5Utils.installFromAssets(this@SplashActivity, onProgress = {
        withContext(Dispatchers.Main) {
          if (binding.progressBar.isIndeterminate) {
            binding.progressBar.isIndeterminate = false
          }
          binding.progressBar.progress = it
        }
      }, onSucceed = {
        SPKeyConstants.IS_INSTALL_X5.put(true)
        runOnUiThread {
          restartApp()
          binding.tvTips.text = "初始化完成，未重启App请手动重启App"
          Toast.makeText(this@SplashActivity, "安装成功，未重启App请手动重启", Toast.LENGTH_LONG)
            .show()
          Timber.i("安装成功")
        }
      }, onFailed = {
        Timber.e("安装失败: $it")
        runOnUiThread {
          Toast.makeText(this@SplashActivity, "安装失败，直接进入App", Toast.LENGTH_LONG).show()
          enterHome()
        }
      })
    }
  }

  //重启代码
  private fun restartApp() {
    val intent = packageManager.getLaunchIntentForPackage(this.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    this.startActivity(intent)
  }

  private fun enterHome() {

    finish()

    val homeClass = if (SPKeyConstants.HOME_DEFAULT_SEARCH.getRequired(false))
      SearchActivity::class.java
    else
      MainActivity::class.java

    startActivity(Intent(this, homeClass))
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }

  override fun initData() {

  }

  override fun initView() {
    Glide.with(this)
      .load("file:///android_asset/images/qrcode_mp.jpg")
      .into(binding.ivQrcode)

    Glide.with(this)
      .load("file:///android_asset/images/qrcode_gitee.png")
      .into(binding.ivQrcode2)
  }
}