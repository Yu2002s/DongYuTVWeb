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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.deleteAll
import org.litepal.extension.findAll
import org.litepal.extension.findFirst
import timber.log.Timber
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
import xyz.jdynb.tv.utils.showToast

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

  private val _favoriteChannelModelList = MutableStateFlow<List<LiveChannelModel>?>(null)

  val favoriteChannelModelList = _favoriteChannelModelList.asStateFlow()

  private val _showActions = MutableStateFlow(true)

  /**
   * 是否显示操作按钮
   */
  val showActions = _showActions.asStateFlow()

  private val _lockMode = MutableStateFlow(SPKeyConstants.LOCK_MODE.getRequired<Boolean>(false))

  /**
   * 是否开启锁定模式
   */
  val lockMode = _lockMode.asStateFlow()

  /**
   * 切台输入的数字
   */
  val numberStringBuilder = StringBuilder()

  /**
   * 是否启用防抖
   */
  var enableDebounce = true

  /**
   * 是否是收藏模式
   */
  var favoriteMode = SPKeyConstants.FAVORITE_MODE.getRequired<Boolean>(false)

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
    .debounce(if (enableDebounce) 300 else 0)
    // 每次改变时都执行这里
    .onEach {
      enableDebounce = true
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
        val index = "channel_config_${currentChannelModel.channelType}".get<Int>(
          currentChannelModel.channelName,
          0
        ) ?: 0
        Timber.i("index: $index, ${currentChannelModel.channelName}")
        val savedChannelModel =
          currentChannelModel.children.getOrNull(index) ?: currentChannelModel.children.first()
        if (savedChannelModel.player.isNotEmpty()) {
          currentChannelModel.player = savedChannelModel.player
        }
        if (savedChannelModel.args.isNotEmpty()) {
          currentChannelModel.args = savedChannelModel.args
        }
      }

      if (favoriteMode) {
        if (!hasFavoriteChannelList) {
          getFavoriteChannelListDB()
        }
        currentChannelModel.isFavorite = LitePal.where(
          "channelName = ? and channelType = ?",
          currentChannelModel.channelName,
          currentChannelModel.channelType
        ).count<LiveChannelModel>() > 0
      }

      flowOf(currentChannelModel)
    }
    // 改变时都会执行这里
    .onEach {
      // 设置当前的频道分类
      Timber.i(
        "beforeChannelPlayer: ${_currentChannelPlayer.value} afterChannelPlayer: ${it.player}"
      )
      _currentChannelType.value = it.channelType
      _currentChannelPlayer.value = it.player
      // 保存当前的频道信息
      SPKeyConstants.CURRENT_CHANNEL.put(json.encodeToString(it))
    }
    // 发生错误时
    .catch {
      Timber.e(it, "获取频道信息失败")
      // 播放默认的频道
      emit(getDefaultChannelModel())
    }
    .stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      null // 默认没有状态
    )

  /**
   * 初始化频道列表
   */
  /*private suspend fun initChannelList() = withContext(Dispatchers.IO) {
    val channelList = NetworkUtils.requestSuspendCache<List<ChannelModel>>(Api.CHANNEL_LIST)
      .getOrThrow() // 这里可以进一步进行处理
    _channelModelList.value = channelList
  }*/

  /**
   * 初始化数据
   */
  private suspend fun init() = withContext(Dispatchers.IO) {
    val liveConfigUrl = SPKeyConstants.CHANNEL_CONFIG_URL.getRequired<String>(LIVE_CONFIG_URL)
      .ifBlank { LIVE_CONFIG_URL }
    // 读取网络上的配置文件
    val liveContent =
      NetworkUtils.getResponseBodyCache(liveConfigUrl, "lives/live-2026-03-19.jsonc")
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
      _currentChannelPlayer.value = "" // 清空当前播放器
      _currentChannelPlayer.value = model.player // 设置当前播放器
      Timber.i("currentChannelModel: $it")
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
   * 下一个频道
   */
  fun up() {
    val channelModel = currentChannelModel.value
    if (favoriteMode && hasFavoriteChannelList && channelModel?.isFavorite == true) {
      // 如果是收藏的
      var favoriteIndex =
        _favoriteChannelModelList.value?.indexOfFirst { it.number == channelModel.number } ?: return
      if (favoriteIndex == _favoriteChannelModelList.value!!.size - 1) {
        favoriteIndex = 0
      } else {
        favoriteIndex++
      }
      changeCurrentIndex(_favoriteChannelModelList.value!![favoriteIndex])
      return
    }
    if (currentIndex.value >= channelModelList.value.size - 1) {
      _currentIndex.value = 0
    } else {
      _currentIndex.value++
    }
  }

  /**
   * 上一个频道
   */
  fun down() {
    val channelModel = currentChannelModel.value
    if (favoriteMode && hasFavoriteChannelList && channelModel?.isFavorite == true) {
      // 如果是收藏的
      var favoriteIndex =
        _favoriteChannelModelList.value?.indexOfFirst { it.number == channelModel.number } ?: return
      if (favoriteIndex == 0) {
        favoriteIndex = _favoriteChannelModelList.value!!.size - 1
      } else {
        favoriteIndex--
      }
      Timber.i("favoriteIndex: $favoriteIndex")
      changeCurrentIndex(_favoriteChannelModelList.value!![favoriteIndex])
      return
    }
    Timber.i("currentIndex: ${currentIndex.value}")
    if (currentIndex.value <= 0) {
      _currentIndex.value = channelModelList.value.size - 1
    } else {
      _currentIndex.value--
    }
  }

  /**
   * 下一个或上一个频道
   */
  fun downOrUp() {
    if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
      down()
    } else {
      up()
    }
  }

  /**
   * 上一个或下一个频道
   */
  fun upOrDown() {
    if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
      up()
    } else {
      down()
    }
  }

  /**
   * 切换到上一个源
   */
  fun left(): String? {
    currentChannelModel.value?.let {
      if (it.children.size > 1) {
        var index = "channel_config_${it.channelType}".get<Int>(it.channelName, 0) ?: 0
        index = if (index > 0) index - 1 else it.children.size - 1
        changeCurrentSource(it.children[index])
        index.put("channel_config_${it.channelType}", it.channelName)
        return it.children[index].channelName
      }
    }
    return null
  }

  /**
   * 切换到下一个源
   */
  fun right(): String? {
    currentChannelModel.value?.let {
      if (it.children.size > 1) {
        var index = "channel_config_${it.channelType}".get<Int>(it.channelName, 0) ?: 0
        index = if (index < it.children.size - 1) index + 1 else 0
        changeCurrentSource(it.children[index])
        index.put("channel_config_${it.channelType}", it.channelName)
        return it.children[index].channelName
      }
    }
    return null
  }

  /**
   * 切换到下一个源
   *
   * @param showNullToast 是否显示没有源的提示
   * @return true 切换成功，false 切换失败
   */
  fun nextSource(showNullToast: Boolean = true): Boolean {
    val name = right()
    Timber.i("nextSource: $name")
    if (name != null) {
      "已切换到源【$name】".showToast(Toast.LENGTH_LONG)
      return true
    } else if (showNullToast) {
      "当前频道只有一个源".showToast(Toast.LENGTH_LONG)
    }
    return false
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

  /**
   * 收藏或取消收藏频道
   */
  fun favoriteOrUnFavorite(
    channelModel: LiveChannelModel? = currentChannelModel.value,
    callback: (() -> Unit)? = null
  ) {
    channelModel ?: return
    viewModelScope.launch {
      val isFavorite = withContext(Dispatchers.IO) {
        val foundChannelModel = LitePal.where(
          "channelName = ? and channelType = ?",
          channelModel.channelName,
          channelModel.channelType
        )
          .findFirst<LiveChannelModel>()
        Timber.i("foundChannelModel: $foundChannelModel")
        if (foundChannelModel == null) {
          Timber.i("收藏: $channelModel")
          channelModel.copy().save()
        } else {
          Timber.i("取消收藏: $channelModel")
          handleUnFavorite(foundChannelModel)
          false
        }
      }
      callback?.invoke()
      Timber.i("收藏结果: ${channelModel.channelName} ${if (isFavorite) "已收藏" else "已取消收藏"}")
      (channelModel.channelName + if (isFavorite) "已收藏" else "已取消收藏").showToast()
    }
  }

  /**
   * 取消收藏指定频道
   */
  suspend fun handleUnFavorite(channelModel: LiveChannelModel) {
    withContext(Dispatchers.IO) {
      LitePal.deleteAll<LiveChannelModel>(
        "channelName = ? and channelType = ?",
        channelModel.channelName,
        channelModel.channelType
      )
    }
  }

  /**
   * 取消收藏指定频道
   */
  fun unFavorite(channelModel: LiveChannelModel? = currentChannelModel.value) {
    channelModel ?: return
    viewModelScope.launch {
      handleUnFavorite(channelModel)
      "${channelModel.channelName}已取消收藏".showToast()
    }
  }

  /**
   * 清空收藏频道列表
   */
  fun clearFavoriteChannelList() {
    _favoriteChannelModelList.value = null
  }

  private suspend fun getFavoriteChannelListDB() {
    _favoriteChannelModelList.value = withContext(Dispatchers.IO) {
      LitePal.findAll<LiveChannelModel>().sortedBy { it.number }.onEach {
        it.isFavorite = true
      }
    }
  }

  /**
   * 获取收藏频道列表
   */
  fun getFavoriteChannelList() {
    viewModelScope.launch {
      getFavoriteChannelListDB()
    }
  }

  /**
   * 锁定
   */
  fun lock() {
    _lockMode.value = true
    SPKeyConstants.LOCK_MODE.put(true)
  }

  /**
   * 解锁
   */
  fun unLock() {
    _lockMode.value = false
    SPKeyConstants.LOCK_MODE.put(false)
  }

  /**
   * 是否有收藏频道
   */
  val hasFavoriteChannelList get() = !_favoriteChannelModelList.value.isNullOrEmpty()

  /**
   * 设置收藏模式
   */
  fun favoriteMode(enable: Boolean) {
    if (favoriteMode == enable) {
      return
    }
    if (enable) {
      "进入收藏模式播放".showToast()
    } else {
      "退出收藏模式播放".showToast()
    }
    favoriteMode = enable
    SPKeyConstants.FAVORITE_MODE.put(enable)
  }
}