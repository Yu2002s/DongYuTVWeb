package xyz.jdynb.tv.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.drake.engine.base.EngineDialog
import com.tencent.smtt.sdk.QbSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogX5DebugBinding
import xyz.jdynb.tv.ui.activity.WebViewUpdateActivity
import xyz.jdynb.tv.utils.X5Utils

class X5DebugDialog(context: Context) :
  EngineDialog<DialogX5DebugBinding>(context, R.style.Theme_BaseDialog) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_x5_debug)

    binding.tvInfo.text = with(StringBuilder()) {
      append("WebView内核版本: ${X5Utils.getWebViewVersionNumber()}")
      appendLine()
      append("X5内核版本: ${QbSdk.getTbsVersion(context)}")
      appendLine()
      append("Android Sdk版本: ${Build.VERSION.SDK_INT}" )
    }

    binding.btnX5.setOnClickListener {
      context.startActivity(Intent(context, WebViewUpdateActivity::class.java))
    }

    binding.btnX5.requestFocus()

    binding.btnX5Install.setOnClickListener {
      Toast.makeText(context, "开始安装X5内核，请稍候...", Toast.LENGTH_LONG).show()
      CoroutineScope(Dispatchers.Default).launch {
        var progress = 0
        X5Utils.startInstallX5LocationCore(context, onProgress = {
          if (it - progress > 30) {
            progress = it
            withContext(Dispatchers.Main) {
              Toast.makeText(context, "安装进度: $it%", Toast.LENGTH_SHORT).show()
            }
          }
        }, onSucceed = {
          SPKeyConstants.IS_INSTALL_X5.put(true)
          Looper.prepare()
          Toast.makeText(context, "安装成功，未重启App请手动重启", Toast.LENGTH_LONG).show()
          Looper.loop()
        }, onFailed = {
          Toast.makeText(context, "安装失败，直接进入App", Toast.LENGTH_LONG).show()
        })
      }
    }

    binding.btnWebviewDownload.setOnClickListener {
      showImageDialog("file:///android_asset/images/qrcode_webview.png")
    }
  }

  private fun showImageDialog(path: String) {
    val imageView = ImageView(context).apply {
      layoutParams = ViewGroup.LayoutParams(500, 500)
    }
    Glide.with(context)
      .load(path)
      .into(imageView)
    Dialog(context).apply {
      setContentView(imageView)
      show()
    }
  }

  override fun initView() {


  }

  override fun initData() {
  }
}