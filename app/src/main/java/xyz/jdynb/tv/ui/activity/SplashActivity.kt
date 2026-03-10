package xyz.jdynb.tv.ui.activity

import android.content.Intent
import com.drake.engine.base.EngineActivity
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.tv.MainActivity
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.ActivitySplashBinding

class SplashActivity : EngineActivity<ActivitySplashBinding>(R.layout.activity_splash) {

  override fun init() {
    super.init()
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