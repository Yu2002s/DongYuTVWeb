package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.widget.Toast
import com.bumptech.glide.Glide
import com.drake.engine.base.EngineDialog
import com.drake.engine.dialog.setMaxWidth
import com.drake.engine.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.DialogUpdateBinding
import xyz.jdynb.tv.model.UpdateModel
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 更新弹窗
 */
class UpdateDialog(context: Context, private val updateModel: UpdateModel) :
  EngineDialog<DialogUpdateBinding>(context) {

   companion object {

     private const val TAG = "UpdateDialog"

   }

  private val scope = CoroutineScope(Dispatchers.Default)

  private var timeFlag = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_update)
  }

  override fun initData() {
    binding.m = updateModel

    scope.launch {
      while (timeFlag) {
        delay(1000L)
        if (--updateModel.closeTime <= 0) {
          dismiss()
          break
        }
      }
    }
  }

  private fun startUpdate() {
    timeFlag = false
    Toast.makeText(context, "正在下载更新...请等待", Toast.LENGTH_LONG).show()
    try {
      scope.launch(Dispatchers.Main) {
        val file = File(context.externalCacheDir, "update.apk")
        withContext(Dispatchers.IO) {
          Log.i(TAG, "url: ${updateModel.url}")
          val url = URL(updateModel.url).openConnection() as HttpURLConnection
          url.connectTimeout = 10000
          url.readTimeout = 10000
          url.requestMethod = "GET"
          val fos = FileOutputStream(file)
          url.inputStream.use { inputStream ->
            updateModel.progress = (100 * inputStream.available() / url.contentLength)
            val buffer = ByteArray(1024)
            var len = inputStream.read(buffer)
            fos.use { outputStream ->
              while (len != -1) {
                outputStream.write(buffer, 0, len)
                len = inputStream.read(buffer)
              }
            }
          }
        }
        if (!file.exists()) {
          throw IllegalStateException("更新文件不存在")
        }
        Toast.makeText(context, "下载完成，正在安装", Toast.LENGTH_LONG).show()
        AppUtils.installApp(file)
      }
    } catch (e: Exception) {
      Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
    } finally {
      binding.btnUpdate.isEnabled = true
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    Log.i(TAG, "dispatchKeyEvent: ${event.keyCode}")
    return super.dispatchKeyEvent(event)
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    Log.i(TAG, "onKeyDown: $keyCode")
    when (keyCode) {
      KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
        Log.i(TAG, "update")
        timeFlag = false
        binding.btnUpdate.callOnClick()
        return true
      }
      KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
        dismiss()
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    timeFlag = false
    scope.cancel()
  }

  override fun initView() {
    window!!.attributes.gravity = Gravity.CENTER
    setMaxWidth(percent = 0.9f)

    binding.btnUpdate.setOnClickListener {
      it.isEnabled = false
      startUpdate()
    }

    binding.btnCancel.setOnClickListener {
      dismiss()
    }

    Glide.with(context)
      .load("file:///android_asset/images/qrcode_mp.jpg")
      .into(binding.ivQrcode)
  }

}