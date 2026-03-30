package xyz.jdynb.tv.dialog

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.drake.engine.base.EngineDialog
import com.drake.engine.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import xyz.jdynb.tv.MainActivity
import xyz.jdynb.tv.MainViewModel
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogSettingBinding
import xyz.jdynb.tv.ui.activity.WifiAdbActivity
import xyz.jdynb.tv.utils.UpdateUtils
import xyz.jdynb.tv.utils.showToast
import kotlin.system.exitProcess

class SettingDialog(context: Context, private val mainViewModel: MainViewModel? = null) :
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


    binding.swHome.initSwitch(SPKeyConstants.HOME_DEFAULT_SEARCH, false) {
      Toast.makeText(context, "下次启动时生效", Toast.LENGTH_SHORT).show()
    }
    binding.swCctv.initSwitch(SPKeyConstants.CCTV_CHANNEL, false) {
      Toast.makeText(context, "需要重启软件才能生效", Toast.LENGTH_LONG).show()
    }
    binding.swSlideSwitchChannel.initSwitch(SPKeyConstants.SLIDE_SWITCH_CHANNEL, false)
    binding.tvIp.text = NetworkUtils.getIPAddress(true)

    binding.btnCheckUpdate.setOnClickListener {
      GlobalScope.launch(Dispatchers.Main) {
        UpdateUtils.checkUpdate(context)
      }
    }

    binding.btnCheckUpdateCustom.setOnClickListener {
      showImageDialog("file:///android_asset/images/qrcode_update.png")
      Toast.makeText(context, "请扫码打开", Toast.LENGTH_SHORT).show()
    }

    binding.btnDonate.setOnClickListener {
      showImageDialog("file:///android_asset/images/qrcode.png")
      Toast.makeText(context, "感谢你的支持！", Toast.LENGTH_SHORT).show()
    }

    binding.btnFeedback.setOnClickListener {
      Toast.makeText(context, "请扫码关注公众号", Toast.LENGTH_SHORT).show()
      showImageDialog("file:///android_asset/images/qrcode_mp.jpg")
    }

    binding.btnWebviewDebug.setOnClickListener {
      X5DebugDialog(context).show()
    }

    binding.btnRemoteInstall.setOnClickListener {
      context.startActivity(Intent(context, WifiAdbActivity::class.java))
    }

    binding.btnExit.isVisible = SPKeyConstants.SHOW_EXIT_BTN.getRequired<Boolean>(false)
    binding.btnExit.setOnClickListener {
      exitProcess(0)
    }

    binding.btnFaq.setOnClickListener {
      FaqDialog(context).show()
    }

    binding.btnHelp.setOnClickListener {
      showImageDialog("file:///android_asset/images/qrcode_help.png")
    }

    binding.btnMoreSetting.setOnClickListener {
      dismiss()
      FullSettingDialog(context).show()
    }
  }

  private fun showImageDialog(path: String) {
    val imageView = ImageView(context).apply {
      layoutParams = ViewGroup.LayoutParams(500, 500)
    }
    // val inputStream = context.assets.open(path)
    // val readBytes = inputStream.readBytes()
    Glide.with(context)
      .load(path)
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

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_P) {
      dismiss()
      val channelName = mainViewModel?.right()
      if (channelName != null) {
        Toast.makeText(context, "已切换到 $channelName", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "当前频道只有一个源", Toast.LENGTH_SHORT).show()
      }
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    try {
      serverThread?.interrupt()
    } catch (_: InterruptedException) {
    }
  }
}