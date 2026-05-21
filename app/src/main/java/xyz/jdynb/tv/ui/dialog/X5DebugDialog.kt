package xyz.jdynb.tv.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineDialog
import com.drake.engine.utils.AppUtils
import com.tencent.smtt.sdk.QbSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.R
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogX5DebugBinding
import xyz.jdynb.tv.model.WebViewCoreModel
import xyz.jdynb.tv.ui.activity.X5WebViewDebugActivity
import xyz.jdynb.tv.utils.NetworkUtils
import xyz.jdynb.tv.utils.X5Utils
import java.io.File
import androidx.core.graphics.drawable.toDrawable

class X5DebugDialog(context: Context) :
  EngineDialog<DialogX5DebugBinding>(context, R.style.Theme_BaseDialog) {

  companion object {

    private const val TAG = "X5DebugDialog"

  }

  private val handler = Handler(Looper.getMainLooper())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_x5_debug)
  }

  private suspend fun installFromAsset() {
    var progress = 0
    X5Utils.installFromAssets(context, onProgress = {
      if (it - progress > 30) {
        progress = it
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "安装进度: $it%", Toast.LENGTH_SHORT).show()
        }
      }
    }, onSucceed = {
      SPKeyConstants.IS_INSTALL_X5.put(true)
      binding.root.post {
        AlertDialog.Builder(context)
          .setTitle("提示")
          .setMessage("安装完成，请手动断电重启设备。如果还是无效，请点击设置中的问题反馈给我反馈")
          .show()
        Toast.makeText(context, "安装完成，请手动断电重启设备。", Toast.LENGTH_LONG).show()
      }
    }, onFailed = {
      binding.root.post {
        AlertDialog.Builder(context)
          .setTitle("提示")
          .setMessage("安装失败，失败原因: $it，请点击设置中的问题反馈给我反馈")
          .show()
      }
    })
  }

  private suspend fun installFromNetwork(url: String) {
    val progressDialog = ProgressDialog(context).apply {
      setMessage("正在下载，请稍候...(安装后断电重启设备)")
      setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
      show()
    }
    X5Utils.installFromNetwork(context, url, onProgress = {
      withContext(Dispatchers.Main) {
        progressDialog.progress = it
      }
    }, onSucceed = {
      Log.i(TAG, "X5安装成功")
      SPKeyConstants.IS_INSTALL_X5.put(true)
      binding.root.post {
        progressDialog.progress = 100
        progressDialog.setMessage("安装成功，请手动断电重启设备，才能生效。如果还是无效，请点击设置中的问题反馈给我反馈")
        Toast.makeText(context, "安装成功，请手动重启App（断电）", Toast.LENGTH_LONG).show()
      }
    }, onFailed = {
      Log.e(TAG, "X5安装失败: $it")
      binding.root.post {
        Toast.makeText(context, "安装失败：$it", Toast.LENGTH_LONG).show()
        progressDialog.setMessage("安装失败，失败原因：$it，请点击设置中的问题反馈给我反馈")
      }
    })
  }

  private suspend fun installWebView(url: String) {
    val progressDialog = ProgressDialog(context).apply {
      setMessage("正在下载，请稍候...(安装完成后断电重启)")
      setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
      show()
    }
    try {
      // 安装更新WebView
      val installPath = X5Utils.downloadCoreFile("webview.apk", url, onProgress = {
        withContext(Dispatchers.Main) {
          Log.i(TAG, "install progress: $it")
          progressDialog.progress = it
        }
      })
      val webviewApk = File(installPath)
      if (!webviewApk.exists()) {
        return
      }
      Log.i(TAG, "开始安装WebView: ${webviewApk.path}")
      Toast.makeText(context, "开始安装WebView，可能设备不支持安装，安装后断电重启设备", Toast.LENGTH_LONG).show()
      AppUtils.installApp(webviewApk)
    } catch (e: Exception) {
      Log.e(TAG, "安装WebView失败: ${e.message}")
      Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
      progressDialog.dismiss()
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
    window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    window?.setBackgroundDrawable(Color.argb(125, 0, 0, 0).toDrawable())
    binding.tvInfo.text = with(StringBuilder()) {
      append("WebView内核版本: ${X5Utils.getWebViewVersionNumber()}")
      appendLine()
      append("X5内核版本: ${QbSdk.getTbsVersion(context)}")
      appendLine()
      append("Android Sdk版本: ${Build.VERSION.SDK_INT}")
      appendLine()
      append("构建分支: ${BuildConfig.FLAVOR}")
      appendLine()
      append("CPU架构: ${Build.CPU_ABI},${Build.CPU_ABI2}")
    }

    binding.rvCore.dividerSpace(20).setup {
      addType<WebViewCoreModel>(R.layout.item_list_webview)

      R.id.item_webview.onClick {
        val model = getModel<WebViewCoreModel>()
        Log.i(TAG, "model: $model")
        CoroutineScope(Dispatchers.Main).launch {
          if (model.type == 1) {
            installFromNetwork(model.url)
          } else {
            installWebView(model.url)
          }
        }
      }
    }

    binding.btnX5.setOnClickListener {
      context.startActivity(Intent(context, X5WebViewDebugActivity::class.java))
    }

    binding.btnX5.requestFocus()

    binding.btnX5Install.setOnClickListener {
      if (BuildConfig.FLAVOR == "webview") {
        Toast.makeText(context, "当前是Webview版本，请使用右侧在线安装", Toast.LENGTH_LONG).show()
        return@setOnClickListener
      }
      Toast.makeText(context, "开始安装X5内核，请稍候...", Toast.LENGTH_LONG).show()
      CoroutineScope(Dispatchers.Default).launch {
        installFromAsset()
      }
    }

    binding.btnWebviewDownload.setOnClickListener {
      showImageDialog("file:///android_asset/images/qrcode_webview.png")
    }
  }

  override fun initData() {
    CoroutineScope(Dispatchers.Main).launch {
      // val abi = if (X5Utils.is32BitAbi(context)) "armeabi-v7a" else "arm64-v8a"
      NetworkUtils.requestSuspendResult<List<WebViewCoreModel>>(Api.WEBVIEW_LIST /*mapOf("abi" to abi)*/)
        .onSuccess {
          binding.rvCore.models = it
        }.onFailure {
          Toast.makeText(context, "获取数据失败: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    handler.removeCallbacksAndMessages(null)
  }
}