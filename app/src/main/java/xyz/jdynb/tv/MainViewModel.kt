package xyz.jdynb.tv

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.enums.LivePlayer
import xyz.jdynb.tv.ui.fragment.LivePlayerFragment
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.LiveChannelTypeModel
import xyz.jdynb.tv.model.LiveModel
import xyz.jdynb.tv.utils.NetworkUtils
import xyz.jdynb.tv.utils.SpUtils.get

class MainViewModel : ViewModel() {

  companion object {

    private const val TAG = "MainViewModel"

    /**
     * 直播配置地址
     */
    private const val LIVE_CONFIG_URL =
      "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/lives/live-2026-03-19.jsonc"

  }

  private val _currentIndex = MutableStateFlow(SPKeyConstants.CURRENT_INDEX.getRequired(0))

  /**
   *  当前直播的索引位置，默认为 0
   */
  val currentIndex = _currentIndex.asStateFlow()

  /**
   * 切台之前的频道索引位置
   */
  private var beforeIndex = currentIndex.value

  private var _liveModel: LiveModel? = null

  /**
   * /assets/live-3.jsonc 的反序列配置对象
   */
  val liveModel get() = _liveModel!!

  private val _channelModelList = MutableStateFlow<List<LiveChannelModel>>(emptyList())

  /**
   * 频道列表
   */
  val channelModelList = _channelModelList.asStateFlow()

  private val _channelTypeModelList = MutableStateFlow<List<LiveChannelTypeModel>>(emptyList())

  /**
   * 频道分组列表
   */
  val channelTypeModelList = _channelTypeModelList.asStateFlow()

  private val _showActions = MutableStateFlow(true)

  /**
   * 是否显示操作按钮
   */
  val showActions = _showActions.asStateFlow()

  /**
   * 切台输入的数字
   */
  val numberStringBuilder = StringBuilder()

  @OptIn(ExperimentalSerializationApi::class)
  private val json = Json {
    ignoreUnknownKeys = true // 忽略未知的键
    encodeDefaults = true // 序列化默认值
    allowComments = true // 允许注释
  }

  /**
   * 当前的频道信息
   */
  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  val currentChannelModel = _currentIndex
    // 300ms 防抖
    .debounce(300)
    // 每次改变时都执行这里
    .onEach {
      // 非输入状态时
      if (!isTypingNumber()) {
        // 记录切台之前的频道索引位置
        beforeIndex = it
      }
      // 显示当前的频道信息
      _showCurrentChannel.value = true
    }
    // 转换为一个新的流
    .flatMapLatest {
      if (_channelModelList.value.isEmpty()) {
        // 如果当前频道列表为空，则初始化频道列表，并初始化 JS 脚本
        init()
      }
      // 保存当前的频道索引
      SPKeyConstants.CURRENT_INDEX.put(it)
      val currentChannelModel = channelModelList.value.getOrNull(it) ?: getDefaultChannelModel()
      if (currentChannelModel.children.isNotEmpty()) {
        // 如果有子频道，则读取子频道的配置并设置
        // 读取保存的索引
        val index = "channel_config_${currentChannelModel.channelType}".get<Int>(currentChannelModel.channelName, 0) ?: 0
        Log.i(TAG, "index: $index, ${currentChannelModel.channelName}")
        val savedChannelModel = currentChannelModel.children.getOrNull(index) ?: currentChannelModel.children.first()
        if (savedChannelModel.player.isNotEmpty()) {
          currentChannelModel.player = savedChannelModel.player
        }
        if (savedChannelModel.args.isNotEmpty()) {
          currentChannelModel.args = savedChannelModel.args
        }
      }
      flowOf(currentChannelModel)
    }
    // 改变时都会执行这里
    .onEach {
      // 设置当前的频道分类
      Log.i(TAG, "beforeChannelPlayer: ${_currentChannelPlayer.value} afterChannelPlayer: ${it.player}")
      _currentChannelType.value = it.channelType
      _currentChannelPlayer.value = it.player
      // 保存当前的频道信息
      SPKeyConstants.CURRENT_CHANNEL.put(json.encodeToString(it))
    }
    // 发生错误时
    .catch {
      Log.e(TAG, "获取频道信息失败: ", it)
      // 播放默认的频道
      emit(getDefaultChannelModel())
    }
    .stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      null // 默认没有状态
    )

  /**
   * 初始化数据
   */
  private suspend fun init() = withContext(Dispatchers.IO) {
    // 读取网络上的配置文件
    val liveContent = NetworkUtils.getResponseBodyCache(LIVE_CONFIG_URL, "lives/live-2026-03-19.jsonc")
    // Log.i(TAG, "liveContent: $liveContent")
    // 反序列化赋值给 liveModel 对象
    _liveModel = json.decodeFromString<LiveModel>(liveContent)
    // 频道类型列表
    _channelTypeModelList.value =
      liveModel.channel.filter {
        // 过滤掉一些配置出错的频道
        val currentPlayer = liveModel.player.find { player -> player.id == it.player }
        if (currentPlayer == null) {
          return@filter false
        }
        // 过滤隐藏掉的和没有找到播放配置的频道
        !it.hidden && LivePlayer.getLivePlayerForPlayer(currentPlayer.name) != null
      }
    // 将所有频道进行打平存储起来
    _channelModelList.value = _channelTypeModelList.value.flatMap { liveChannelTypeModel ->
      // 一些关键信息需要进行遍历配置
      liveChannelTypeModel.channelList.onEach {
        // 设置 hidden
        if (liveChannelTypeModel.hidden != it.hidden) {
          it.hidden = liveChannelTypeModel.hidden
        }
        // player 名称
        if (it.player.isEmpty()) {
          it.player = liveChannelTypeModel.player
        }
        if (it.source.isEmpty()) {
          it.source = liveChannelTypeModel.source
        }
        // 频道类型
        it.channelType = liveChannelTypeModel.channelType
      }
    }.filter { !it.hidden }.onEachIndexed { index, model ->
      // 统一设置频道号码
      model.number = index + 1 // 设置频道序号
    }.onEach {
      it.children.forEach { child ->
        child.number = it.number
      }
    }

    //distinctBy { it.number }.sortedBy { it.number } // 去重，并且升序排序
  }

  /**
   * 获取默认的频道
   */
  private fun getDefaultChannelModel() = LiveChannelModel(
    channelName = "CCTV1",
    tvLogo = "https://resources.yangshipin.cn/assets/oms/image/202306/d57905b93540bd15f0c48230dbbbff7ee0d645ff539e38866e2d15c8b9f7dfcd.png?imageMogr2/format/webp",
    channelType = "央视",
    number = 1,
    player = "ysp",
    args = mapOf("pid" to "600001859", "streamId" to "2024078201")
  )

  /**
   * 通过频道获取到对应的 LivePlayerFragment
   *
   * @see LiveChannelModel
   * @see LiveModel
   * @see LivePlayerFragment
   */
  @Suppress("UNCHECKED_CAST")
  fun getFragmentClassForChannel(channelModel: LiveChannelModel): Class<LivePlayerFragment>? {
    val name = liveModel.player.find { it.id == channelModel.player }?.name ?: return null
    return LivePlayer.getLivePlayerForPlayer(name)?.clazz as Class<LivePlayerFragment>?
  }

  private val _showCurrentChannel = MutableStateFlow(true)

  /**
   * 是否显示当前频道信息
   */
  val showCurrentChannel = _showCurrentChannel.asStateFlow()

  private val _currentChannelType = MutableStateFlow("")

  /**
   * 当前频道渠道类型分组
   */
  val currentChannelType = _currentChannelType.asStateFlow()

  private var _currentChannelPlayer = MutableStateFlow("")

  @OptIn(ExperimentalCoroutinesApi::class)
  val currentChannelPlayer = _currentChannelPlayer.asStateFlow()

  init {
    viewModelScope.launch {
      currentIndex.collectLatest {
        delay(4000L)
        // 4秒后隐藏当前频道信息
        _showCurrentChannel.value = false
      }
    }

    viewModelScope.launch {
      showActions.collectLatest {
        if (it) {
          // 5秒未操作自动隐藏操作栏
          delay(5000)
          showActions(false)
        }
      }
    }
  }

  /**
   * 是否正在输入数字 (切台)
   */
  fun isTypingNumber() = numberStringBuilder.isNotEmpty()

  /**
   * 清除输入的数字
   */
  fun clearInputNumber() {
    numberStringBuilder.clear()
  }

  /**
   * 改变当前的频道索引位置
   */
  fun changeCurrentIndex(currentIndex: Int) {
    _currentIndex.value = currentIndex
  }

  /**
   * 通过 model 修改当前索引的位置
   */
  fun changeCurrentIndex(model: LiveChannelModel) {
    changeCurrentIndex(channelModelList.value.indexOf(model))
  }

  /**
   * 通过 model 修改当前频道的播放源
   */
  fun changeCurrentSource(model: LiveChannelModel) {
    currentChannelModel.value?.let {
      it.player = model.player
      it.args = model.args
      _currentChannelPlayer.value = model.player
    }
  }

  /**
   * 是否显示当前的频道信息
   */
  fun showCurrentChannel(show: Boolean) {
    _showCurrentChannel.value = show
  }

  /**
   * 回滚切台之前的频道索引
   */
  fun rollbackIndex() {
    _currentIndex.value = beforeIndex
  }

  /**
   * 上一个频道
   */
  fun up() {
    if (currentIndex.value >= channelModelList.value.size - 1) {
      _currentIndex.value = 0
    } else {
      _currentIndex.value++
    }
  }

  /**
   * 下一个频道
   */
  fun down() {
    if (currentIndex.value <= 0) {
      _currentIndex.value = channelModelList.value.size - 1
    } else {
      _currentIndex.value--
    }
  }

  /**
   * 切台追加输入数字
   */
  fun appendNumber(number: String) {
    numberStringBuilder.append(number)

    val seekToNumber = numberStringBuilder.toString().toIntOrNull() ?: return
    if (seekToNumber in 1..channelModelList.value.size) {
      val seekToIndex = channelModelList.value.indexOfFirst { it.number == seekToNumber }
      if (seekToIndex == currentIndex.value) {
        numberStringBuilder.clear()
        return
      } else if (seekToIndex == -1) {
        Toast.makeText(DongYuTVApplication.context, "没有找到该频道", Toast.LENGTH_SHORT).show()
        numberStringBuilder.clear()
        return
      }
      _currentIndex.value = seekToIndex
    }
  }

  /**
   * 显示操作栏
   */
  fun showActions(show: Boolean = true) {
    _showActions.value = show
  }
}