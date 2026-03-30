package xyz.jdynb.tv.ui.activity

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineToolbarActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.jdynb.tv.DongYuTVApplication
import xyz.jdynb.tv.R
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.databinding.ActivityCrashBinding
import xyz.jdynb.tv.model.AppCrashLogModel
import xyz.jdynb.tv.utils.NetworkUtils
import xyz.jdynb.tv.utils.UpdateUtils

/**
 * App全局闪退处理
 */
class CrashActivity : EngineToolbarActivity<ActivityCrashBinding>(R.layout.activity_crash) {

  companion object {

    private const val PARAM_LOG = "log"

    @JvmStatic
    fun actionStart(log: String) {
      DongYuTVApplication.context.startActivity(
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

    binding.restartApp.requestFocus()

    binding.checkUpdate.setOnClickListener {
      lifecycleScope.launch {
        UpdateUtils.checkUpdate(this@CrashActivity, true)
      }
    }

    /*Toast.makeText(this, "30秒后自动重启", Toast.LENGTH_SHORT).show()

    Handler(Looper.getMainLooper()).postDelayed({
      binding.restartApp.callOnClick()
    }, 30000L)*/
  }

  override fun initData() {
    intent?.getStringExtra(PARAM_LOG)?.let { log ->
      binding.tvCrashContent.text = log

      lifecycleScope.launch(Dispatchers.IO) {
        try {
          val appCrashLog = AppCrashLogModel(content = log)
          val requestBody = NetworkUtils.json.encodeToString(appCrashLog)
            .toRequestBody(contentType = "application/json".toMediaTypeOrNull())
          NetworkUtils.requestSyncResult<Unit?>(Api.APP_CRASH, requestBody)
        } catch (_: Exception) {
        }
      }
    }
  }
}