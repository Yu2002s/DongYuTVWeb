package xyz.jdynb.tv.ui.activity

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineActivity
import kotlinx.coroutines.launch
import xyz.jdynb.tv.R
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.databinding.ActivitySearchListBinding
import xyz.jdynb.tv.model.MovieModel
import xyz.jdynb.tv.model.PageModel
import xyz.jdynb.tv.utils.NetworkUtils

class SearchListActivity :
  EngineActivity<ActivitySearchListBinding>(R.layout.activity_search_list) {

  companion object {

    private const val PARAM_KEYWORD = "keyword"

    @JvmStatic
    fun actionStart(context: Context, keyword: String) {
      context.startActivity(
        Intent(context, SearchListActivity::class.java)
          .putExtra(PARAM_KEYWORD, keyword)
      )
    }

  }

  override fun initData() {
    val keyword = intent.getStringExtra(PARAM_KEYWORD) ?: return
    lifecycleScope.launch {
      binding.state.showLoading()

      val result =
        NetworkUtils.requestSuspendResult<PageModel<MovieModel>>(Api.SEARCH, mapOf(PARAM_KEYWORD to keyword))

      result.onSuccess {
        binding.state.showContent()
        binding.rvSearchList.models = it.data.filter { item -> !item.director.isNullOrEmpty() }
      }.onFailure {
        binding.state.showError(it)
        Toast.makeText(this@SearchListActivity, it.message, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun initView() {
    binding.btnBack.requestFocus()

    binding.rvSearchList.divider {
      setDivider(8, true)
      orientation = DividerOrientation.GRID
      includeVisible = true
    }.setup {
      addType<MovieModel>(R.layout.item_list_movie)

      R.id.item_movie.onClick {
        VideoActivity.play(getModel())
      }
    }
  }

  override fun onClick(v: View) {
    super.onClick(v)
    when (v.id) {
      R.id.btn_back -> {
        finish()
      }
    }
  }
}