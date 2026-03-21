package xyz.jdynb.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineActivity
import kotlinx.coroutines.launch
import xyz.jdynb.tv.databinding.ActivityMainBinding
import xyz.jdynb.tv.dialog.ChannelListDialog
import xyz.jdynb.tv.ui.fragment.LivePlayerFragment
import kotlin.system.exitProcess
import androidx.core.view.WindowInsetsControllerCompat
import com.drake.brv.reflect.copyType
import com.drake.engine.utils.NetworkUtils
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.dialog.ChannelSourceDialog
import xyz.jdynb.tv.dialog.SettingDialog
import xyz.jdynb.tv.ui.activity.SearchActivity

class MainActivity : EngineActivity<ActivityMainBinding>(R.layout.activity_main) {

  companion object {

    private const val TAG = "MainActivity"

    private const val SLIDE_DISTANCE = 200
  }

  /**
   * 当前显示的 LivePlayerFragment
   */
  private var livePlayerFragment: LivePlayerFragment? = null

  private lateinit var channelListDialog: ChannelListDialog

  private val mainViewModel by viewModels<MainViewModel>()

  private lateinit var audioManager: AudioManager

  /**
   * 唤醒锁，用于防止设备进入休眠状态导致断网
   */
  private var wakeLock: PowerManager.WakeLock? = null

  /**
   * WiFi 锁，用于保持 WiFi 连接不断
   */
  private var wifiLock: WifiManager.WifiLock? = null

  /**
   * 最后一次按下返回键的时间
   */
  private var lastBackTime = 0L

  /**
   * 网络是否连接
   */
  private var isNetworkConnected = false

  /**
   *网络状态广播接收器
   */
  private val networkReceiver = NetworkBoardReceiver()

  override fun init() {
    super.init()
    // 悬浮窗权限改为手动授权，不对用户展示，以免影响体验
    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
      val activities = packageManager.queryIntentActivities(intent, 0)
      if (activities.isNotEmpty()) {
        intent.data = "package:$packageName".toUri()
        AlertDialog.Builder(this)
          .setTitle("需要悬浮窗权限")
          .setMessage("请授予悬浮窗权限，用于实现开机自启动\n\n按左右方向键选择【确认】或取消【取消】，不开启直接按返回键")
          .setPositiveButton("确定") { _, _ ->
            startActivity(intent)
          }
          .setNegativeButton("取消", null)
          .show()
      }
    }*/

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
    
    // 强制显示鼠标指针，解决电视上插入鼠标后不显示的问题
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      window.decorView.requestPointerCapture()
    }

    audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

    isNetworkConnected = NetworkUtils.isConnected()

    // 注册网络状态广播接收器
    registerNetworkReceiver()

    // 获取唤醒锁，防止设备休眠导致断网
    acquireWakeLock()
    // 获取 WiFi 锁，保持 WiFi 连接
    acquireWifiLock()
  }

  /**
   * 注册网络状态广播接收器
   */
  @Suppress("DEPRECATION")
  private fun registerNetworkReceiver() {
    val filter = IntentFilter().apply {
      addAction(ConnectivityManager.CONNECTIVITY_ACTION)
    }
    registerReceiver(networkReceiver, filter)
  }

  /**
   * 注销网络状态广播接收器
   */
  private fun unregisterNetworkReceiver() {
    try {
      unregisterReceiver(networkReceiver)
    } catch (_: IllegalArgumentException) {
      //接器未注册时会抛出异常，忽略
    }
  }

  override fun initView() {
    binding.m = mainViewModel
    binding.lifecycleOwner = this

    channelListDialog = ChannelListDialog(this, mainViewModel)
    channelListDialog.onRefreshListener = {
      refreshFragment()
    }
    channelListDialog.onSwitchSourceListener = {
      changeSource()
    }
  }

  override fun initData() {
    lifecycleScope.launch {
      mainViewModel.currentChannelPlayer.collect {
        Log.i(TAG, "currentChannelPlayer: $it")
        if (it.isEmpty()) {
          return@collect
        }
        handleChannelPlayerChange(it)
      }
    }

    // 轮询判断网络是否连接，这里不使用，而是使用动态注册的广播接收器
    /*lifecycleScope.launch {
      while (!NetworkUtils.isConnected()) {
        Toast.makeText(this@MainActivity, "检测到未连接到网络，正在尝试刷新...", Toast.LENGTH_LONG)
          .show()
        delay(3000L)
        handleChannelTypeChange()
      }
    }*/
  }

  fun refreshFragment() {
    handleChannelPlayerChange()
  }

  /**
   * 处理频道播放器变化
   *
   * @param player 频道播放器
   */
  private fun handleChannelPlayerChange(player: String? = mainViewModel.currentChannelModel.value?.player) {
    if (player == null) {
      return
    }
    if (mainViewModel.currentChannelModel.value == null) {
      Handler(Looper.getMainLooper()).postDelayed({
        refreshFragment()
      }, 1500L)
      return
    }
    val fragmentClazz = mainViewModel
      .getFragmentClassForChannel(mainViewModel.currentChannelModel.value!!)
      ?: return
    Log.i(TAG, "showFragment: $fragmentClazz")

    showFragment(fragmentClazz)
  }

  /**
   * 换源
   */
  fun changeSource() {
    val currentChannelModel = mainViewModel.currentChannelModel.value
    if (currentChannelModel == null) {
      Toast.makeText(this, "请等待初始化之后操作", Toast.LENGTH_SHORT).show()
      return
    }

    if (currentChannelModel.children.isEmpty()) {
      Toast.makeText(this, "当前频道没有子频道", Toast.LENGTH_SHORT).show()
      return
    }

    ChannelSourceDialog(
      this,
      currentChannelModel
    ).apply {
      onChannelChange = { model ->
        mainViewModel.changeCurrentSource(model)
        refreshFragment()
      }
      show()
    }
  }

  /**
   * 显示指定的 Fragment
   *
   * @param fragmentClazz Fragment 类
   */
  private fun showFragment(fragmentClazz: Class<LivePlayerFragment>) {
    val transaction = supportFragmentManager.beginTransaction()
    val tag = fragmentClazz.name
    val target = fragmentClazz.getDeclaredConstructor().newInstance()
    livePlayerFragment = target
    transaction.replace(R.id.fragment, target, tag)
    transaction.commitAllowingStateLoss()
  }

  /**
   * 处理点击事件
   */
  override fun onClick(v: View) {
    super.onClick(v)
    when (v.id) {
      // 菜单
      R.id.btn_menu -> {
        if (mainViewModel.channelModelList.value.isEmpty()) {
          return
        }
        channelListDialog.show()
      }

      // 左
      R.id.btn_left -> {
        if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
          mainViewModel.down()
        } else {
          mainViewModel.up()
        }
      }

      // 右
      R.id.btn_right -> {
        if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
          mainViewModel.up()
        } else {
          mainViewModel.down()
        }
      }

      // 刷新
      R.id.btn_refresh -> {
        refreshFragment()
      }

      // 搜索
      R.id.btn_search -> {
        startActivity(Intent(this, SearchActivity::class.java))
      }

      // 换源
      R.id.btn_change_source -> {
        changeSource()
      }
    }
  }

  private var downY = 0f
  private var moveY = 0f

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    mainViewModel.showActions()
    if (!SPKeyConstants.SLIDE_SWITCH_CHANNEL.getRequired<Boolean>(false)) {
      return super.dispatchTouchEvent(event)
    }
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        downY = event.y
        moveY = 0f
      }

      MotionEvent.ACTION_MOVE -> {
        moveY = event.y - downY
        Log.i(TAG, "moveY: $moveY")
        binding.fragment.translationY = moveY
      }

      MotionEvent.ACTION_UP -> {
        if (moveY > SLIDE_DISTANCE) {
          binding.btnRight.callOnClick()
        } else if (moveY < -SLIDE_DISTANCE) {
          binding.btnLeft.callOnClick()
        }
        binding.fragment.animate()
          .translationY(0f)
          .start()
      }

      MotionEvent.ACTION_CANCEL -> {
        binding.fragment.translationY = 0f
      }
    }
    return super.dispatchTouchEvent(event)
  }

  override fun onBackPressed() {
    if (handleBackPress()) {
      super.onBackPressed()
    }
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
        if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
          mainViewModel.down()
        } else {
          mainViewModel.up()
        }
      }

      /**
       * 下
       */
      KeyEvent.KEYCODE_DPAD_DOWN -> {
        if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
          mainViewModel.up()
        } else {
          mainViewModel.down()
        }
      }

      // ENTER、OK（确认）
      KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> {
        Log.d(TAG, "onKeyDown: Ok")
        if (SPKeyConstants.OK_CHANNEL.getRequired<Boolean>(true)) {
          binding.btnMenu.callOnClick()
        } else {
          livePlayerFragment?.resumeOrPause()
        }
      }

      // 静音
      KeyEvent.KEYCODE_MUTE -> {
        try {
          audioManager.setStreamVolume(
            AudioManager.STREAM_SYSTEM,
            0,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
          )
        } catch (_: Exception) {
        }
      }

      //  volume down、left
      KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_LEFT -> {
        try {
          audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
          )
        } catch (_: Exception) {
        }
      }

      // volume up、right
      KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_RIGHT -> {
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (volume < audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
          try {
            audioManager.adjustStreamVolume(
              AudioManager.STREAM_MUSIC,
              AudioManager.ADJUST_RAISE,
              AudioManager.FLAG_SHOW_UI
            )
          } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
          }
        }
      }

      // 返回
      /*KeyEvent.KEYCODE_BACK,*/ KeyEvent.KEYCODE_ESCAPE -> {
      handleBackPress()
    }

      // #
      // 重新加载
      KeyEvent.KEYCODE_POUND -> {
        livePlayerFragment?.refresh()
      }

      // 主页
      KeyEvent.KEYCODE_HOME -> {
        exitProcess(0)
      }

      // 菜单
      KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_P -> {
        if (SPKeyConstants.OK_CHANNEL.getRequired<Boolean>(true)) {
          SettingDialog(this).show()
        } else {
          binding.btnMenu.callOnClick()
        }
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

  /**
   * 处理 App 默认的返回
   *
   * @return true 表示已处理返回键 false 表示未处理返回键
   */
  private fun handleBackPress(): Boolean {
    if (mainViewModel.showCurrentChannel.value) {
      // 如果显示了当前频道
      mainViewModel.showCurrentChannel(false)
      mainViewModel.rollbackIndex() // 回滚之前的频道
      return false
    } else {
      if (System.currentTimeMillis() - lastBackTime > 2000) {
        lastBackTime = System.currentTimeMillis()
        Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show()
        return false
      }
    }
    return true
  }

  override fun onDestroy() {
    super.onDestroy()
    // 注销网络状态广播接收器
    unregisterNetworkReceiver()
    // 释放唤醒锁
    releaseWakeLock()
    // 释放 WiFi 锁
    releaseWifiLock()
  }

  private inner class NetworkBoardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      Log.i(TAG, "network change")
      val currentNetworkConnected = NetworkUtils.isConnected()
      if (currentNetworkConnected && !isNetworkConnected) {
        refreshFragment()
        Toast.makeText(context, "已连接到网络", Toast.LENGTH_SHORT).show()
      } else if (!currentNetworkConnected) {
        Toast.makeText(context, "已断开网络，当网络连接后自动刷新页面", Toast.LENGTH_LONG).show()
      }
      isNetworkConnected = currentNetworkConnected
    }

  }

  /**
   * 获取唤醒锁，防止设备进入休眠状态
   */
  private fun acquireWakeLock() {
    try {
      val powerManager = getSystemService(POWER_SERVICE) as PowerManager
      wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "DongYuTV::WakeLock"
      ).apply {
        setReferenceCounted(false)
        acquire(10*60*60*1000L) // 10 小时
      }
      Log.i(TAG, "WakeLock acquired")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to acquire WakeLock", e)
    }
  }

  /**
   * 释放唤醒锁
   */
  private fun releaseWakeLock() {
    try {
      wakeLock?.let {
        if (it.isHeld) {
          it.release()
          Log.i(TAG, "WakeLock released")
        }
      }
      wakeLock = null
    } catch (e: Exception) {
      Log.e(TAG, "Failed to release WakeLock", e)
    }
  }

  /**
   * 获取 WiFi 锁，保持 WiFi 连接不断
   */
  private fun acquireWifiLock() {
    try {
      val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
      wifiLock = wifiManager.createWifiLock(
        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
        "DongYuTV::WifiLock"
      ).apply {
        setReferenceCounted(false)
        acquire()
      }
      Log.i(TAG, "WifiLock acquired")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to acquire WifiLock", e)
    }
  }

  /**
   * 释放 WiFi 锁
   */
  private fun releaseWifiLock() {
    try {
      wifiLock?.let {
        if (it.isHeld) {
          it.release()
          Log.i(TAG, "WifiLock released")
        }
      }
      wifiLock = null
    } catch (e: Exception) {
      Log.e(TAG, "Failed to release WifiLock", e)
    }
  }
}