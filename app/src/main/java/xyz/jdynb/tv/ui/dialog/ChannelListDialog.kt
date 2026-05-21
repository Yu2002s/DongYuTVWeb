package xyz.jdynb.tv.ui.dialog

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.divider
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.linear
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.jdynb.tv.MainViewModel
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogChannelListBinding
import xyz.jdynb.tv.databinding.ItemListGroupBinding
import xyz.jdynb.tv.model.LiveChannelTypeModel
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.response.main
import xyz.jdynb.tv.ui.activity.SearchActivity
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.SpUtils.put
import xyz.jdynb.tv.utils.isTv

class ChannelListDialog(
  private val activity: AppCompatActivity,
  private val mainViewModel: MainViewModel
) :
  EngineDialog<DialogChannelListBinding>(activity, R.style.ChannelDialogStyle) {

  companion object {

    private const val TAG = "ChannelListDialog"

    private const val AUTO_CLOSE_TIME = 60000L

  }

  private var closeTime = AUTO_CLOSE_TIME

  private var job: Job? = null

  private var beforeSelectedIndex = -1

  var onRefreshListener: (() -> Unit)? = null

  var onSwitchSourceListener: (() -> Unit)? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_channel_list)

    window?.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      (context.resources.displayMetrics.heightPixels * 0.95).toInt()
    )
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    closeTime = AUTO_CLOSE_TIME
    when (keyCode) {
      KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
        dismiss()
        return true
      }

      // 再次按菜单键进行收藏取消收藏操作
      KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_M -> {
        val checkedPosition = getCheckedPosition()
        if (checkedPosition == -1) {
          return super.onKeyDown(keyCode, event)
        }
        // 获取焦点的频道
        val models = binding.rvChannel.models
        if (models.isNullOrEmpty()) {
          return super.onKeyDown(keyCode, event)
        }
        val model = models.getOrNull(checkedPosition) as? LiveChannelModel ?: return super.onKeyDown(keyCode, event)
        mainViewModel.favoriteOrUnFavorite(model) {
          if (binding.rvGroup.bindingAdapter.checkedCount == 0) {
            mainViewModel.getFavoriteChannelList()
          }
        }
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onStart() {
    super.onStart()

    if (binding.rvGroup.bindingAdapter.checkedCount == 0) {
      mainViewModel.getFavoriteChannelList()
    }

    if (!isTv(context)) {
      binding.tvTips.isVisible = false
      return
    }
    binding.tvTips.isVisible = true
    closeTime = AUTO_CLOSE_TIME
    job = activity.lifecycleScope.launch {
      while (true) {
        if (closeTime <= 0) {
          dismiss()
          break
        }
        binding.tvTips.text = "${closeTime / 1000L}秒无操作后自动返回"
        delay(1000)
        closeTime -= 1000
      }
    }
  }

  override fun onStop() {
    super.onStop()
    job?.cancel()
  }

  override fun initView() {
    if (SPKeyConstants.CHANNEL_SINGLE.getRequired<Boolean>(false)) {
      binding.rvChannel.linear()
    }

    binding.rvChannel.divider {
      setDivider(10)
      orientation = DividerOrientation.GRID
    }.setup {
      singleMode = true

      addType<LiveChannelModel>(R.layout.item_list_channel)

      onChecked { position, checked, allChecked ->
        val model = getModel<LiveChannelModel>(position)
        model.isSelected = checked
      }

      R.id.tv_channel.onClick {
        val model = getModel<LiveChannelModel>()
        setChecked(modelPosition, true)

        mainViewModel.favoriteMode(mainViewModel.hasFavoriteChannelList)

        mainViewModel.changeCurrentIndex(model)

        dismiss()
      }

      R.id.tv_channel.onLongClick {
        val model = getModel<LiveChannelModel>()
        mainViewModel.favoriteOrUnFavorite(model) {
          if (binding.rvGroup.bindingAdapter.checkedCount == 0) {
            mainViewModel.getFavoriteChannelList()
          }
        }
      }
    }

    binding.rvGroup.dividerSpace(10).setup {
      singleMode = true

      addType<LiveChannelTypeModel>(R.layout.item_list_group)

      onCreate {
        getBinding<ItemListGroupBinding>().root.onFocusChangeListener =
          View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
              setChecked(modelPosition, true)
            }
          }
      }

      onChecked { position, checked, allChecked ->
        val model = getModel<LiveChannelTypeModel>(position)
        model.isSelected = checked

        mainViewModel.clearFavoriteChannelList()

        if (binding.rvChannel.isComputingLayout || !checked) {
          return@onChecked
        }
        binding.btnFavorite.isSelected = false
        binding.rvChannel.models = model.channelList.onEach { it.isSelected = false }
        if (mainViewModel.currentChannelType.value == model.channelType) {
          val checkedPosition = model.channelList.indexOfFirst {
            it.number == mainViewModel.currentChannelModel.value!!.number
          }
          if (checkedPosition == -1) {
            binding.rvChannel.post {
              binding.rvChannel.scrollToPosition(0)
            }
          } else {
            binding.rvChannel.bindingAdapter.setChecked(checkedPosition, true)

            binding.rvChannel.post {
              val layoutManager = binding.rvChannel.layoutManager
              if (layoutManager is GridLayoutManager) {
                layoutManager.scrollToPositionWithOffset(checkedPosition, 0)
              } else if (layoutManager is LinearLayoutManager) {
                layoutManager.scrollToPositionWithOffset(checkedPosition, 0)
              }
              if (window?.currentFocus == null) {
                binding.rvChannel.getChildAt(checkedPosition)?.requestFocus()
              }
            }
          }
        } else {
          binding.rvChannel.post {
            binding.rvChannel.scrollToPosition(0)
          }
        }
      }

      // 点击左侧的分类
      R.id.tv_group.onClick {
        setChecked(modelPosition, true)
      }
    }

    binding.btnFavorite.setOnClickListener {
      val bindingAdapter = binding.rvGroup.bindingAdapter
      if (bindingAdapter.checkedCount > 0) {
        bindingAdapter.setChecked(bindingAdapter.checkedPosition[0], false)
      }
      mainViewModel.getFavoriteChannelList()
    }

    /*binding.btnFavorite.onFocusChangeListener = View.OnFocusChangeListener { p0, hasFocus ->
      if (hasFocus) {
        binding.btnFavorite.callOnClick()
      }
    }*/

    // 返回
    binding.btnBack.setOnClickListener {
      dismiss()
    }

    binding.btnRefresh.setOnClickListener {
      onRefreshListener?.invoke()
    }

    binding.btnSearch.setOnClickListener {
      dismiss()
      context.startActivity(Intent(context, SearchActivity::class.java))
    }

    binding.btnSetting.setOnClickListener {
      dismiss()
      SettingDialog(it.context).show()
    }

    binding.btnSwitchChannel.setOnClickListener {
      onSwitchSourceListener?.invoke()
    }
  }

  override fun initData() {
    activity.lifecycleScope.launch {
      mainViewModel.currentChannelType.collect { channelType ->
        val checkedPosition = binding.rvGroup.models?.indexOfFirst { model ->
          model as LiveChannelTypeModel
          channelType == model.channelType
        } ?: return@collect
        if (checkedPosition == -1) {
          return@collect
        }
        binding.rvGroup.bindingAdapter.setChecked(checkedPosition, true)
      }
    }

    activity.lifecycleScope.launch {
      mainViewModel.channelTypeModelList.collect {
        binding.rvGroup.models = it
        val checkedPosition = it.indexOfFirst { model ->
          model.channelType == mainViewModel.currentChannelType.value
        }
        if (checkedPosition == -1) {
          return@collect
        }
        binding.rvGroup.bindingAdapter.setChecked(checkedPosition, true)
      }
    }

    activity.lifecycleScope.launch {
      mainViewModel.favoriteChannelModelList.collect {
        it ?: return@collect
        if (binding.rvChannel.isComputingLayout) {
          return@collect
        }

        var checkedPosition = getCheckedPosition()

        binding.rvChannel.models = it
        binding.btnFavorite.isSelected = true
        if (it.isEmpty()) {
          return@collect
        }
        if (checkedPosition == -1) {
          checkedPosition = it.indexOfFirst { model -> mainViewModel.currentChannelModel.value == model }
          if (checkedPosition == -1) {
            return@collect
          }
        }
        if (checkedPosition !in 0 until it.size) {
          return@collect
        }
        binding.rvChannel.bindingAdapter.setChecked(checkedPosition, true)
        binding.rvChannel.post {
          binding.rvChannel.getChildAt(checkedPosition).requestFocus()
          binding.rvChannel.scrollToPosition(checkedPosition)
        }
      }
    }
  }

  private fun getCheckedPosition(): Int {
    val view = window?.currentFocus ?: return -1
    val parentView = view.parent
    if (parentView is RecyclerView) {
      val parentId = parentView.id
      if (parentId == R.id.rv_channel) {
        return parentView.indexOfChild(view)
      }
    }
    return -1
  }

}