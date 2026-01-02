package xyz.jdynb.tv

import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineActivity
import com.drake.engine.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import xyz.jdynb.tv.databinding.ActivityMainBinding
import xyz.jdynb.tv.dialog.ChannelListDialog
import xyz.jdynb.tv.dialog.UpdateDialog
import xyz.jdynb.tv.fragment.LivePlayerFragment
import xyz.jdynb.tv.fragment.YspLivePlayerFragment
import xyz.jdynb.tv.model.UpdateModel
import xyz.jdynb.tv.utils.WebViewUpgrade
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class MainActivity : EngineActivity<ActivityMainBinding>(R.layout.activity_main) {

  companion object {

    private const val TAG = "MainActivity"

    private const val CHECK_UPDATE_URL =
      "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/version.json"

  }

  private val livePlayerFragment: LivePlayerFragment = YspLivePlayerFragment()

  private lateinit var channelListDialog: ChannelListDialog

  private val mainViewModel by viewModels<MainViewModel>()

  private lateinit var audioManager: AudioManager

  private var lastBackTime = 0L

  private var isUpgrade = false

  override fun init() {
    super.init()
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.hide(WindowInsetsCompat.Type.systemBars())

    // 判断cpu型号决定需不需要升级
    isUpgrade = !Build.SUPPORTED_ABIS.any {
      it.contains("arm64")
    }

    Log.i(TAG, "abi: ${Build.SUPPORTED_ABIS.joinToString(",")}")

    audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
  }

  override fun initView() {
    binding.m = mainViewModel
    binding.lifecycleOwner = this

    channelListDialog = ChannelListDialog(this, mainViewModel)
  }

  override fun initData() {
    lifecycleScope.launch {
      mainViewModel.currentChannelModel.collect {
        Log.i(TAG, "currentChannelModel: $it")
      }
    }

    lifecycleScope.launch {
      checkUpdate()
    }

    lifecycleScope.launch {
      mainViewModel.currentChannelType.collect {
        if (it.isEmpty()) return@collect
        val fragmentClazz = mainViewModel
          .getFragmentClassForChannel(mainViewModel.currentChannelModel.value!!)
        Log.i(TAG, "showFragment: $fragmentClazz")

        if (isUpgrade || BuildConfig.DEBUG) {
          showFragment(fragmentClazz)
        } else {
          isUpgrade = true
          WebViewUpgrade.initWebView(this@MainActivity) {
            showFragment(fragmentClazz)
          }
        }
      }
      /*mainViewModel.currentFragment.collect { clazz ->
        clazz ?: return@collect
        Log.i(TAG, "showFragment: $clazz")

        if (isUpgrade || BuildConfig.DEBUG) {
          showFragment(clazz)
        } else {
          isUpgrade = true
          WebViewUpgrade.initWebView(this@MainActivity) {
            showFragment(clazz)
          }
        }
      }*/
    }
  }

  private fun showFragment(fragmentClazz: Class<LivePlayerFragment>) {
    val transaction = supportFragmentManager.beginTransaction()
    val tag = fragmentClazz.name
    val target = fragmentClazz.getDeclaredConstructor().newInstance()
    /*val livePlayerFragment = supportFragmentManager
      .findFragmentByTag(tag) as? LivePlayerFragment
    val target = livePlayerFragment ?: (supportFragmentManager.fragmentFactory.instantiate(
      fragmentClazz.classLoader!!,
      tag
    ))
    supportFragmentManager.fragments.forEach {
      if (it != null && it != target && it.isAdded && it.id == R.id.fragment) {
        transaction.hide(it)
      }
    }*/
    transaction.replace(R.id.fragment, target, tag)
    transaction.commitNow()
  }

  private suspend fun checkUpdate() {
    try {
      val updateModel = withContext(Dispatchers.IO) {
        val connection: HttpURLConnection =
          URL(CHECK_UPDATE_URL).openConnection() as HttpURLConnection
        connection.inputStream.use { inputStream ->
          val content = inputStream.readBytes().toString(StandardCharsets.UTF_8)
          Log.i(TAG, "content: $content")
          val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
          }
          json.decodeFromString<UpdateModel>(content)
        }
      }
      Log.i(TAG, "updateModel: $updateModel")
      if (AppUtils.getAppVersionCode() < updateModel.versionCode) {
        // 发现新版本
        UpdateDialog(this, updateModel).run {
          setCancelable(false)
          setCanceledOnTouchOutside(false)
          show()
        }
      }
    } catch (_: Exception) {
    }
  }

  override fun onClick(v: View) {
    super.onClick(v)
    when (v.id) {
      R.id.btn_menu -> {
        if (mainViewModel.channelModelList.value.isEmpty()) {
          return
        }
        channelListDialog.show()
      }

      R.id.btn_left -> {
        mainViewModel.down()
      }

      R.id.btn_right -> {
        mainViewModel.up()
      }
    }
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    mainViewModel.showActions()
    return super.dispatchTouchEvent(event)
  }

  /**
   * 事件分发时就拦截，避免事件被 webview 拦截
   */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    Log.i(TAG, "dispatchKeyEvent: ${event.keyCode}")
    val keyCode = event.keyCode
    val action = event.action
    if (action != KeyEvent.ACTION_DOWN) {
      return super.dispatchKeyEvent(event)
    }
    when (keyCode) {
      /**
       * 上
       */
      KeyEvent.KEYCODE_DPAD_UP -> {
        if (channelListDialog.isShowing) {
          return true
        }
        mainViewModel.up()
      }

      /**
       * 下
       */
      KeyEvent.KEYCODE_DPAD_DOWN -> {
        if (channelListDialog.isShowing) {
          return true
        }
        mainViewModel.down()
      }

      // ENTER、OK（确认）
      KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> {
        if (channelListDialog.isShowing) {
          return true
        }
        Log.d(TAG, "onKeyDown: Ok")
        livePlayerFragment.resumeOrPause()
      }

      // 静音
      KeyEvent.KEYCODE_MUTE -> {
        try {
          audioManager.setStreamVolume(
            AudioManager.STREAM_SYSTEM,
            0,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
          )
        } catch (_: SecurityException) {
        }
      }

      KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_LEFT -> {
        if (channelListDialog.isShowing) {
          return true
        }
        try {
          audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
          )
        } catch (_: SecurityException) {
        }
      }

      KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_RIGHT -> {
        if (channelListDialog.isShowing) {
          return true
        }
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (volume < audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
          try {
            audioManager.adjustStreamVolume(
              AudioManager.STREAM_MUSIC,
              AudioManager.ADJUST_RAISE,
              AudioManager.FLAG_SHOW_UI
            )
          } catch (e: SecurityException) {
            Log.e(TAG, e.message.toString())
          }
        }
      }

      // 返回
      KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
        if (channelListDialog.isShowing) {
          return true
        }
        if (mainViewModel.showCurrentChannel.value) {
          // 如果显示了当前频道
          mainViewModel.showCurrentChannel(false)
          mainViewModel.rollbackIndex() // 回滚之前的频道
        } else {
          if (System.currentTimeMillis() - lastBackTime > 2000) {
            lastBackTime = System.currentTimeMillis()
            Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show()
          } else {
            finish()
          }
        }
      }

      // 主页
      KeyEvent.KEYCODE_HOME -> {

      }

      // 菜单
      KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_P -> {
        if (channelListDialog.isShowing) {
          return true
        }
        binding.btnMenu.callOnClick()
      }

      // 0
      // 数字
      KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
      KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
      KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
      KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2,
      KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5,
      KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8,
      KeyEvent.KEYCODE_NUMPAD_9 -> {
        val num = getNumForKeyCode(keyCode)

        Log.i(TAG, "input number: $num")

        mainViewModel.appendNumber(num)
      }
    }
    return super.dispatchKeyEvent(event)
  }

  private fun getNumForKeyCode(keyCode: Int): String {
    return when (keyCode) {
      KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> "0"
      KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> "1"
      KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> "2"
      KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> "3"
      KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> "4"
      KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> "5"
      KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> "6"
      KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> "7"
      KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> "8"
      KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> "9"
      else -> ""
    }
  }
}