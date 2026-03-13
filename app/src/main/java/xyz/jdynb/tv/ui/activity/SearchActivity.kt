package xyz.jdynb.tv.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.jdynb.tv.MainActivity
import xyz.jdynb.tv.R
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.constants.IntentActionConstants
import xyz.jdynb.tv.databinding.ActivitySearchBinding
import xyz.jdynb.tv.dialog.SettingDialog
import xyz.jdynb.tv.dialog.UserAuthDialog
import xyz.jdynb.tv.utils.NetworkUtils

class SearchActivity : EngineActivity<ActivitySearchBinding>(R.layout.activity_search) {

  private var searchJob: Job? = null

  private val userAuthBroadcastReceiver = UserAuthBroadcastReceiver()

  private var userAuthDialog: UserAuthDialog? = null // Initialize the dialog but keep it nullable

  override fun initData() {
    lifecycleScope.launch {
      NetworkUtils.requestSuspendResult<Unit>("/user/checkLogin")
    }
  }

  private fun getSuggestList(keyword: String) {
    if (keyword.isBlank()) {
      binding.rv.models = null
      binding.state.showEmpty()
      return
    }
    binding.state.showLoading()
    searchJob?.cancel(null)
    searchJob = lifecycleScope.launch {
      binding.rv.models =
        NetworkUtils.requestSuspendResult<List<String>>(Api.SEARCH_SUGGEST, mapOf("keyword" to keyword))
          .onFailure {
            binding.state.showError(it)
            Log.e("SearchActivity", "Error fetching search suggestions", it)
          }
          .getOrNull()
      binding.state.showContent()
    }
  }

  override fun onClick(v: View) {
    super.onClick(v)
    when (v.id) {
      R.id.btn_back -> {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
      }

      R.id.btn_setting -> {
        SettingDialog(this).show()
      }

      R.id.btn_delete -> {
        binding.tvKeyword.text = binding.tvKeyword.text.toString().dropLast(1)
      }

      R.id.btn_clear -> {
        binding.tvKeyword.text = ""
      }
    }
  }

  override fun initView() {
    for ((index, char) in ('A'..'Z').withIndex()) {
      binding.grid.addView(createTextView(char), index + 5)
    }

    (binding.btnDelete.layoutParams as GridLayout.LayoutParams).setMargins(4)
    (binding.btnClear.layoutParams as GridLayout.LayoutParams).setMargins(4)

    binding.rv.divider {
      setDivider(8, true)
      includeVisible = true
      orientation = DividerOrientation.GRID
    }.setup {
      addType<String>(R.layout.item_list_suggest)

      R.id.item_name.onClick {
        SearchListActivity.actionStart(this@SearchActivity, getModel())
      }
    }

    binding.tvKeyword.doAfterTextChanged {
      getSuggestList(it.toString())
    }

    binding.btnDelete.requestFocus()

    val intent = IntentFilter(IntentActionConstants.AUTHORIZED)
    intent.addAction(IntentActionConstants.UN_AUTHORIZED)
    registerReceiver(userAuthBroadcastReceiver, intent)
  }

  private fun createTextView(char: Char): TextView {
    val button = Button(this)
    button.text = char.toString()
    button.textSize = 23f
    button.setTextColor(Color.WHITE)
    button.background = ContextCompat.getDrawable(this, R.drawable.bg_select)
    button.layoutParams = GridLayout.LayoutParams().apply {
      setMargins(4, 4, 4, 4)
    }
    button.setOnClickListener {
      binding.tvKeyword.append(char.toString())
    }
    return button
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(userAuthBroadcastReceiver)
    searchJob?.cancel(null)
  }

  private inner class UserAuthBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == IntentActionConstants.UN_AUTHORIZED) {
        Toast.makeText(this@SearchActivity, "登录过期，请返回首页验证", Toast.LENGTH_LONG).show()
        if (userAuthDialog == null) {
          userAuthDialog = UserAuthDialog(this@SearchActivity).apply { show() }
        }
      } else if (intent?.action == IntentActionConstants.AUTHORIZED) {
        userAuthDialog?.dismiss()
        userAuthDialog = null
      }
    }
  }
}