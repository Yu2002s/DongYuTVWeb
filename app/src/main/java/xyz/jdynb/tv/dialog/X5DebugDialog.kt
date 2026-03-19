package xyz.jdynb.tv.dialog

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.drake.engine.base.EngineDialog
import com.tencent.smtt.sdk.QbSdk
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogX5DebugBinding
import xyz.jdynb.tv.ui.activity.WebViewUpdateActivity
import xyz.jdynb.tv.utils.getWebViewVersionNumber
import xyz.jdynb.tv.utils.startInstallX5LocationCore

class X5DebugDialog(context: Context) :
  EngineDialog<DialogX5DebugBinding>(context, R.style.Theme_BaseDialog) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_x5_debug)

    binding.tvInfo.text = with(StringBuilder()) {
      append("WebView内核版本: ${getWebViewVersionNumber()}")
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
      startInstallX5LocationCore(context, onSucceed = {
        SPKeyConstants.IS_INSTALL_X5.put(true)
        Toast.makeText(context, "安装成功，未重启App请手动重启", Toast.LENGTH_LONG).show()
      }, onFailed = {
        Toast.makeText(context, "安装失败，直接进入App", Toast.LENGTH_LONG).show()
      })
    }
  }

  override fun initView() {


  }

  override fun initData() {
  }
}