package xyz.jdynb.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.drake.engine.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.jdynb.tv.base.BaseKeyEventActivity
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.ActivityMainBinding
import xyz.jdynb.tv.ui.activity.SearchActivity
import xyz.jdynb.tv.ui.dialog.ChannelListDialog
import xyz.jdynb.tv.ui.dialog.ChannelSourceDialog
import xyz.jdynb.tv.ui.dialog.SettingDialog
import xyz.jdynb.tv.ui.fragment.LivePlayerFragment
import xyz.jdynb.tv.utils.SlideTouchHelper
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.UpdateUtils
import xyz.jdynb.tv.utils.showToast

/**
 * 主页面
 *
 * @author Yu2002s
 */
class MainActivity : BaseKeyEventActivity<ActivityMainBinding>(R.layout.activity_main), SlideTouchHelper.OnSlideListener {

  /**
   * 当前显示的 LivePlayerFragment
   */
  private var livePlayerFragment: LivePlayerFragment? = null

  /**
   * 频道列表对话框
   */
  private lateinit var channelListDialog: ChannelListDialog

  /**
   * 主ViewModel
   */
  private val mainViewModel by viewModels<MainViewModel>()

  /**
   * 滑动切换频道的帮助类
   */
  private val slideTouchHelper by lazy {
    SlideTouchHelper(binding.fragment)
  }

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

  /**
   * 处理菜单键点击的 Handler
   */
  private val handler = Handler(Looper.getMainLooper())

  /**
   * 最后一次点击菜单键的时间
   */
  private var lastMenuClickTime = 0L

  /**
   * 点击菜单键的 Runnable
   */
  private val menuClickRunnable = Runnable {
    if (SPKeyConstants.OK_CHANNEL.getRequired<Boolean>(true)) {
      SettingDialog(this, mainViewModel).show()
    } else {
      binding.btnMenu.callOnClick()
    }
  }

  /**
   * 初始化
   */
  override fun init() {
    super.init()
    // 初始化窗口
    initWindow()

    // 检查网络连接状态
    checkNetworkConnection()
    // 注册网络状态广播接收器
    registerNetworkReceiver()
    // 获取唤醒锁，防止设备休眠导致断网
    acquireWakeLock()
    // 获取 WiFi 锁，保持 WiFi 连接
    acquireWifiLock()
    // 设置滑动切换频道的监听器
    slideTouchHelper.onSlideListener = this
  }

  override fun onSlideChange(x: Float, y: Float) {
    // 根据滑动距离计算透明度
  }

  override fun onSlided(direction: SlideTouchHelper.SlideDirection) {
    when (direction) {
      SlideTouchHelper.SlideDirection.Left -> {
        val channelName = mainViewModel.right()
        if (channelName != null) {
          Toast.makeText(this@MainActivity, "已切换到：$channelName", Toast.LENGTH_SHORT)
            .show()
        } else {
          Toast.makeText(this@MainActivity, "只有一个源", Toast.LENGTH_SHORT).show()
        }
      }

      SlideTouchHelper.SlideDirection.Up -> {
        mainViewModel.enableDebounce = false
        binding.btnLeft.callOnClick()
      }

      SlideTouchHelper.SlideDirection.Right -> {
        val channelName = mainViewModel.left()
        if (channelName != null) {
          Toast.makeText(this@MainActivity, "已切换到：$channelName", Toast.LENGTH_SHORT)
            .show()
        } else {
          Toast.makeText(this@MainActivity, "只有一个源", Toast.LENGTH_SHORT).show()
        }
      }

      SlideTouchHelper.SlideDirection.Down -> {
        mainViewModel.enableDebounce = false
        binding.btnRight.callOnClick()
      }
    }
  }

  override fun onDPadLeft(): Boolean {
    if (SPKeyConstants.VOLUME_CONTROL_DIRECTION.getRequired<Boolean>(false)) {
      volumeDown()
    } else {
      val source = mainViewModel.left()
      if (source == null) {
        Toast.makeText(this, "当前频道只有一个源", Toast.LENGTH_SHORT).show()
        return true
      }
      Toast.makeText(this, "已切换到: $source", Toast.LENGTH_SHORT).show()
      return true
    }
    return super.onDPadLeft()
  }

  override fun onDPadUp(): Boolean {
    if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
      mainViewModel.down()
    } else {
      mainViewModel.up()
    }
    return super.onDPadUp()
  }

  override fun onDPadDown(): Boolean {
    if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
      mainViewModel.up()
    } else {
      mainViewModel.down()
    }
    return super.onDPadDown()
  }

  override fun onDPadRight(): Boolean {
    if (SPKeyConstants.VOLUME_CONTROL_DIRECTION.getRequired<Boolean>(false)) {
      volumeUp()
    } else {
      val source = mainViewModel.right()
      if (source == null) {
        Toast.makeText(this, "当前频道只有一个源", Toast.LENGTH_SHORT).show()
        return true
      }
      Toast.makeText(this, "已切换到: $source", Toast.LENGTH_SHORT).show()
      return true
    }
    return super.onDPadRight()
  }

  override fun onOk(): Boolean {
    if (isLongPress) {
      mainViewModel.favoriteOrUnFavorite()
      return true
    }
    if (SPKeyConstants.OK_CHANNEL.getRequired<Boolean>(true)) {
      binding.btnMenu.callOnClick()
    } else {
      livePlayerFragment?.resumeOrPause()
    }
    return super.onOk()
  }

  override fun onMenu(): Boolean {
    val now = System.currentTimeMillis()

    if (now - lastMenuClickTime < 500) {
      handler.removeCallbacks(menuClickRunnable)
      val channelName = mainViewModel.right()
      if (channelName != null) {
        Toast.makeText(this, "已切换到: $channelName", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(this, "当前频道只有一个源", Toast.LENGTH_SHORT).show()
      }
    } else {
      handler.postDelayed(menuClickRunnable, 500)
      lastMenuClickTime = now
    }
    return super.onMenu()
  }

  override fun onNumber(number: String): Boolean {
    mainViewModel.appendNumber(number)
    return super.onNumber(number)
  }

  override fun initView() {
    binding.m = mainViewModel
    binding.lifecycleOwner = this

    binding.btnExit.isVisible = SPKeyConstants.SHOW_EXIT_BTN.getRequired<Boolean>(false)

    channelListDialog = ChannelListDialog(this, mainViewModel)
    channelListDialog.onRefreshListener = {
      refreshFragment()
    }
    channelListDialog.onSwitchSourceListener = {
      chooseLiveSource()
    }

    binding.btnLock.setOnLongClickListener {
      mainViewModel.unLock()
      "已解除锁定".showToast()
      true
    }
  }

  override fun initData() {
    // 监听当前频道播放器变化
    lifecycleScope.launch {
      mainViewModel.currentChannelPlayer.collect {
        if (it.isEmpty()) {
          return@collect
        }
        handleChannelPlayerChange(it)
      }
    }

    // 检查更新
    if (SPKeyConstants.CHECK_UPDATE.getRequired<Boolean>(true)) {
      lifecycleScope.launch {
        UpdateUtils.checkUpdate(this@MainActivity, showToast = false, showDialog = false)
      }
    }
  }

  /**
   * 刷新 LiveFragment
   */
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

    showFragment(fragmentClazz)
  }

  /**
   * 选择直播源
   */
  private fun chooseLiveSource() {
    val currentChannelModel = mainViewModel.currentChannelModel.value
    if (currentChannelModel == null) {
      Toast.makeText(this, "请等待初始化之后操作", Toast.LENGTH_SHORT).show()
      return
    }

    if (currentChannelModel.children.isEmpty()) {
      Toast.makeText(this, "当前频道没有子频道", Toast.LENGTH_SHORT).show()
    }

    ChannelSourceDialog(
      this,
      currentChannelModel
    ).apply {
      onChannelChange = { model ->
        mainViewModel.changeCurrentSource(model)
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

    if (binding.loading.isVisible) {
      binding.loading.isVisible = false
    }
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
      R.id.btn_left -> mainViewModel.downOrUp()

      // 右
      R.id.btn_right -> mainViewModel.upOrDown()

      // 刷新
      R.id.btn_refresh -> refreshFragment()

      // 搜索
      R.id.btn_search -> startActivity(Intent(this, SearchActivity::class.java))

      // 换源
      R.id.btn_change_source -> chooseLiveSource()

      // 退出
      R.id.btn_exit -> handleBackPress()

      // 锁定
      R.id.btn_lock -> {
        if (mainViewModel.lockMode.value) {
          "请长按解除锁定".showToast()
          return
        }
        mainViewModel.lock()
        "已锁定".showToast()
      }
    }
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    // 触摸时显示 action 内容
    mainViewModel.showActions()
    if (!SPKeyConstants.SLIDE_SWITCH_CHANNEL.getRequired<Boolean>(false)) {
      return super.dispatchTouchEvent(event)
    }
    slideTouchHelper.onTouchEvent(event)
    return super.dispatchTouchEvent(event)
  }

  /**
   * 处理 App 默认的返回
   *
   * @return true 表示已处理返回键 false 表示未处理返回键
   */
  override fun handleBackPress(): Boolean {
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
      } else {
        finish()
      }
    }
    return true
  }

  override fun onBackPressed() {
    // 关闭默认的返回关闭
  }

  override fun onDestroy() {
    super.onDestroy()
    // 注销网络状态广播接收器
    unregisterNetworkReceiver()
    // 释放唤醒锁
    releaseWakeLock()
    // 释放 WiFi 锁
    releaseWifiLock()

    handler.removeCallbacksAndMessages(null)
  }

  /**
   * 网络状态广播接收器
   */
  private inner class NetworkBoardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
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
   * 检查网络连接状态
   */
  private fun checkNetworkConnection() {
    // 获取当前网络连接状态
    isNetworkConnected = NetworkUtils.isConnected()
    lifecycleScope.launch {
      var count = 1
      while (true) {
        delay(count * 1000L)
        if (count < 10) {
          count++
        }
        // 10秒轮询检测网络连接
        val connected = NetworkUtils.isAvailableByPing()
        if (connected && !isNetworkConnected) {
          refreshFragment()
        }
        isNetworkConnected = connected
      }
    }
  }

  /**
   * 初始化窗口
   */
  private fun initWindow() {
    // 保持屏幕常亮
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    // 隐藏系统栏
    insetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())

    // 强制显示鼠标指针，解决电视上插入鼠标后不显示的问题
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      window.decorView.requestPointerCapture()
    }
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
        acquire(10 * 60 * 60 * 1000L) // 10 小时
      }
    } catch (e: Exception) {
      Timber.e(e)
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
        }
      }
      wakeLock = null
    } catch (e: Exception) {
      Timber.e(e)
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
    } catch (e: Exception) {
      Timber.e(e)
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
        }
      }
      wifiLock = null
    } catch (e: Exception) {
      Timber.e(e)
    }
  }
}