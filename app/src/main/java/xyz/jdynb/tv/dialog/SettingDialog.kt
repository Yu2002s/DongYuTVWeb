package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.drake.engine.base.EngineDialog
import com.drake.engine.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.SpUtils.put
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

    binding.swReverseDirection.isChecked = SPKeyConstants.REVERSE_DIRECTION.getRequired(false)
    binding.swBoot.isChecked = SPKeyConstants.BOOT_AUTO_START.getRequired(true)
    binding.swHome.isChecked = SPKeyConstants.HOME_DEFAULT_SEARCH.getRequired(false)

    binding.swBoot.setOnCheckedChangeListener { buttonView, isChecked ->
      SPKeyConstants.BOOT_AUTO_START.put(isChecked)
    }

    binding.swReverseDirection.setOnCheckedChangeListener { buttonView, isChecked ->
      SPKeyConstants.REVERSE_DIRECTION.put(isChecked)
    }

    binding.swHome.setOnCheckedChangeListener { buttonView, isChecked ->
      SPKeyConstants.HOME_DEFAULT_SEARCH.put(isChecked)
      Toast.makeText(context, "下次启动时生效", Toast.LENGTH_SHORT).show()
    }

    binding.tvIp.text = NetworkUtils.getIPAddress(true)

    binding.btnCheckUpdate.setOnClickListener {
      GlobalScope.launch(Dispatchers.Main) {
        UpdateUtils.checkUpdate(context)
      }
    }

    binding.btnDonate.setOnClickListener {
      val imageView = ImageView(context).apply {
        layoutParams = ViewGroup.LayoutParams(400, 400)
      }
      val inputStream = context.assets.open("images/qrcode.png")
      val readBytes = inputStream.readBytes()
      Glide.with(context)
        .load(readBytes)
        .into(imageView)
      AlertDialog.Builder(context)
        .setView(imageView)
        .show()
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