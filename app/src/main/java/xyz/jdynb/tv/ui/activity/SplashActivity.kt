package xyz.jdynb.tv.ui.activity

import android.content.Intent
import android.widget.Toast
import com.drake.engine.base.EngineActivity
import com.tencent.smtt.sdk.QbSdk
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.MainActivity
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.ActivitySplashBinding
import xyz.jdynb.tv.utils.getWebViewVersionNumber
import xyz.jdynb.tv.utils.startInstallX5LocationCore

class SplashActivity : EngineActivity<ActivitySplashBinding>(R.layout.activity_splash) {

  override fun init() {
    super.init()

    val isInstallX5 = SPKeyConstants.IS_INSTALL_X5.getRequired(false)

    if (isInstallX5 || getWebViewVersionNumber() >= 85 || QbSdk.canLoadX5(this)) {
      enterHome()
      return
    }

    Toast.makeText(this, "正在安装X5内核，请稍候...", Toast.LENGTH_LONG).show()
    startInstallX5LocationCore(this, onSucceed = {
      SPKeyConstants.IS_INSTALL_X5.put(true)
      restartApp()
      Toast.makeText(this, "安装成功，未重启App请手动重启", Toast.LENGTH_LONG).show()
    }, onFailed = {
      Toast.makeText(this, "安装失败，直接进入App", Toast.LENGTH_LONG).show()
      enterHome()
    })
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
  }
}