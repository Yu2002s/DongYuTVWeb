package xyz.jdynb.tv.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.bumptech.glide.Glide
import com.drake.engine.base.EngineDialog
import com.drake.engine.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogSettingBinding
import xyz.jdynb.tv.utils.UpdateUtils

class SettingDialog(context: Context) :
  EngineDialog<DialogSettingBinding>(context, R.style.Theme_BaseDialog) {

  private var serverThread: Thread? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_setting)
  }

  override fun initView() {
    binding.btnBack.setOnClickListener {
      dismiss()
    }

    binding.btnBack.requestFocus()

    binding.swBoot.initSwitch(SPKeyConstants.BOOT_AUTO_START, true)
    binding.swReverseDirection.initSwitch(SPKeyConstants.REVERSE_DIRECTION, false)
    binding.swHome.initSwitch(SPKeyConstants.HOME_DEFAULT_SEARCH, false) {
      Toast.makeText(context, "下次启动时生效", Toast.LENGTH_SHORT).show()
    }
    binding.swCctv.initSwitch(SPKeyConstants.CCTV_CHANNEL, false) {
      Toast.makeText(context, "需要重启软件才能生效", Toast.LENGTH_LONG).show()
    }
    binding.swUpdate.initSwitch(SPKeyConstants.CHECK_UPDATE, true)
    binding.swOkChannel.initSwitch(SPKeyConstants.OK_CHANNEL, false)

    binding.swSlideSwitchChannel.initSwitch(SPKeyConstants.SLIDE_SWITCH_CHANNEL, false)

    binding.tvIp.text = NetworkUtils.getIPAddress(true)

    binding.btnCheckUpdate.setOnClickListener {
      GlobalScope.launch(Dispatchers.Main) {
        UpdateUtils.checkUpdate(context)
      }
    }

    binding.btnDonate.setOnClickListener {
      showImageDialog("images/qrcode.png")
    }

    binding.btnFeedback.setOnClickListener {
      Toast.makeText(context, "请扫码关注公众号", Toast.LENGTH_SHORT).show()
      showImageDialog("images/qrcode_mp.jpg")
    }

    binding.btnWebviewDebug.setOnClickListener {
      X5DebugDialog(context).show()
    }
  }

  private fun showImageDialog(path: String) {
    val imageView = ImageView(context).apply {
      layoutParams = ViewGroup.LayoutParams(500, 500)
    }
    val inputStream = context.assets.open(path)
    val readBytes = inputStream.readBytes()
    Glide.with(context)
      .load(readBytes)
      .into(imageView)
    Dialog(context).apply {
      setContentView(imageView)
      show()
    }
  }

  private fun SwitchCompat.initSwitch(key: String, default: Boolean, listener: ((Boolean) -> Unit)? = null) {
    isChecked = key.getRequired<Boolean>(default)
    setOnCheckedChangeListener { _, isChecked ->
      key.put(isChecked)
      listener?.invoke(isChecked)
    }
  }

  override fun initData() {
    /*try {
      serverThread = thread {
        val serverSocket = ServerSocket(8888)
        serverSocket.reuseAddress = true
        val socket = serverSocket.accept()
        Log.i("jdy", "已连接设备: ${socket.inetAddress.hostAddress}")
        val br = BufferedReader(socket.inputStream.reader())
        val bw = socket.outputStream.writer()

        var line = br.readLine()
        while (line != null) {
          when (line) {
            "liveModel" -> {
              bw.write(json.encodeToString(mainViewModel.liveModel))
            }
          }
          line = br.readLine()
        }
        socket.shutdownInput()
        socket.close()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }*/
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    try {
      serverThread?.interrupt()
    } catch (_: InterruptedException) {
    }
  }
}