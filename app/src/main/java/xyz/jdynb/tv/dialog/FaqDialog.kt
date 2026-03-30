package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.get
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.jdynb.tv.R
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.databinding.DialogFaqBinding
import xyz.jdynb.tv.databinding.ItemListTitleBinding
import xyz.jdynb.tv.model.AppFaqModel
import xyz.jdynb.tv.utils.NetworkUtils
import xyz.jdynb.tv.utils.showToast

class FaqDialog(context: Context): EngineDialog<DialogFaqBinding>(context, R.style.Theme_BaseDialog) {

  private val faqList = mutableListOf<AppFaqModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_faq)
  }

  override fun initData() {
    CoroutineScope(Dispatchers.Main).launch {
      NetworkUtils.requestSuspendResult<List<AppFaqModel>>(Api.APP_FAQ_LIST)
        .onSuccess {
          faqList.clear()
          faqList.addAll(it)
          binding.rvTitle.models = it.map { model -> model.title }
          if (faqList.isNotEmpty()) {
            binding.rvTitle.post {
              val firstChild = binding.rvTitle.findViewHolderForAdapterPosition(0)?.itemView
              if (firstChild != null) {
                firstChild.requestFocus()
                Timber.d("Focused on first item: ${firstChild.hasFocus()}")
              } else {
                Timber.w("Failed to get first child view holder")
              }
            }
          }
        }.onFailure {
          it.message.showToast()
        }
    }
  }

  override fun initView() {
      binding.rvTitle.dividerSpace(20).setup {
        onCreate {
          getBinding<ItemListTitleBinding>().root.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            onFocusChangeListener = View.OnFocusChangeListener { v, focus ->
              if (focus) {
                val index = binding.rvTitle.getChildAdapterPosition(v)
                if (index != -1) {
                  binding.tvContent.text = faqList[index].content
                }
              }
            }
          }
        }

        addType<String>(R.layout.item_list_title)

        R.id.item_name.onClick {
          binding.tvContent.text = faqList[modelPosition].content
        }
      }
  }
}