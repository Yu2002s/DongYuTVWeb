package xyz.jdynb.tv.ui.activity

import android.content.Intent
import com.drake.engine.base.EngineToolbarActivity
import xyz.jdynb.tv.DongYuTVApplication
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.ActivityCrashBinding

/**
 * App全局闪退处理
 */
class CrashActivity : EngineToolbarActivity<ActivityCrashBinding>(R.layout.activity_crash) {

  companion object {

    private const val PARAM_LOG = "log"

    @JvmStatic
    fun actionStart(log: String) {
      DongYuTVApplication.Companion.context.startActivity(
        Intent(
          DongYuTVApplication.Companion.context,
          CrashActivity::class.java
        ).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          putExtra(PARAM_LOG, log)
        })
    }
  }

  override fun initView() {
    title = "抱歉，系统崩溃了！"

    binding.restartApp.setOnClickListener {
      finish()
    }
  }

  override fun initData() {
    intent?.getStringExtra(PARAM_LOG)?.let { log ->
      binding.tvCrashContent.text = log
    }
  }
}