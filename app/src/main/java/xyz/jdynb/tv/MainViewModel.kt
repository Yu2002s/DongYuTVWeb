package xyz.jdynb.tv

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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import xyz.jdynb.music.utils.SpUtils.get
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.SpUtils.put
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.enums.LivePlayer
import xyz.jdynb.tv.fragment.LivePlayerFragment
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.LiveChannelTypeModel
import xyz.jdynb.tv.model.LiveModel
import xyz.jdynb.tv.utils.NetworkUtils

class MainViewModel : ViewModel() {

  companion object {

    private const val TAG = "MainViewModel"

    /**
     * 直播配置地址
     */
    private const val LIVE_CONFIG_URL = "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/live.jsonc"

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
    ignoreUnknownKeys = true
    encodeDefaults = true
    allowComments = true
  }

  /**
   * 当前的频道信息
   */
  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  val currentChannelModel = _currentIndex
    .debounce(300)
    .onEach {
      if (!isTypingNumber()) {
        // 记录切台之前的频道索引位置
        beforeIndex = it
      }
      _showCurrentChannel.value = true
    }
    .flatMapLatest {
      if (_channelModelList.value.isEmpty()) {
        // 如果当前频道列表为空，则初始化频道列表，并初始化 JS 脚本
        init()
      }
      SPKeyConstants.CURRENT_INDEX.put(it)
      flowOf(channelModelList.value.getOrNull(it) ?: LiveChannelModel())
    }
    .onEach {
      _currentChannelType.value = it.channelType
      // 保存当前的频道信息
      SPKeyConstants.CURRENT_CHANNEL.put(json.encodeToString(it))
    }
    .catch {
      emit(LiveChannelModel())
    }
    .stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      getInitialChannelModel()
    )

  /**
   * 初始化数据
   */
  private suspend fun init() = withContext(Dispatchers.IO) {
    val liveContent = NetworkUtils.getResponseBodyCache(LIVE_CONFIG_URL, "live.jsonc")
    _liveModel = json.decodeFromString<LiveModel>(liveContent)
    _channelTypeModelList.value = liveModel.channel
    _channelModelList.value = _channelTypeModelList.value.flatMap { liveChannelTypeModel ->
      liveChannelTypeModel.channelList.onEach {
        if (it.player.isEmpty()) {
          it.player = liveChannelTypeModel.player
        }
        it.channelType = liveChannelTypeModel.channelType
      }
    }.distinctBy { it.number }.sortedBy { it.number } // 去重，并且升序排序
  }

  /**
   * 获取初始化默认的频道
   */
  private fun getInitialChannelModel(): LiveChannelModel {
    return SPKeyConstants.CURRENT_CHANNEL.get<String?>()?.let {
      runCatching {
        json.decodeFromString<LiveChannelModel>(it)
      }.getOrDefault(LiveChannelModel())
    } ?: LiveChannelModel()
  }

  /**
   * 通过渠道获取到对应的 Fragment
   */
  @Suppress("UNCHECKED_CAST")
  private fun getFragmentClassForChannel(channelModel: LiveChannelModel): Class<LivePlayerFragment> {
    return LivePlayer.getLivePlayerForPlayer(channelModel.player).clazz as Class<LivePlayerFragment>
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

  @OptIn(ExperimentalCoroutinesApi::class)
  val currentFragment = currentChannelModel
    .flatMapConcat {
      flowOf(getFragmentClassForChannel(it))
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

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
      val seekToIndex = seekToNumber - 1
      if (seekToIndex == currentIndex.value) {
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