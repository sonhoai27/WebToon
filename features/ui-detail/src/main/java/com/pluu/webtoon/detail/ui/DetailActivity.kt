package com.pluu.webtoon.detail.ui

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import com.pluu.core.utils.lazyNone
import com.pluu.utils.ProgressDialog
import com.pluu.utils.SystemUiHelper
import com.pluu.utils.animatorColor
import com.pluu.utils.getRequiredParcelableExtra
import com.pluu.utils.getThemeColor
import com.pluu.utils.observeNonNull
import com.pluu.utils.setStatusBarColor
import com.pluu.utils.viewbinding.viewBinding
import com.pluu.webtoon.Const
import com.pluu.webtoon.detail.R
import com.pluu.webtoon.detail.databinding.ActivityDetailBinding
import com.pluu.webtoon.detail.model.getMessage
import com.pluu.webtoon.model.ShareItem
import com.pluu.webtoon.ui.model.PalletColor
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

/**
 * 상세화면 Activity
 * Created by pluu on 2017-05-09.
 */
@AndroidEntryPoint
class DetailActivity : AppCompatActivity(R.layout.activity_detail),
    ToggleListener,
    FirstBindListener {

    private val viewModel by viewModels<DetailViewModel>()

    private val binding by viewBinding(ActivityDetailBinding::bind)

    private val palletColor by lazyNone {
        intent.getRequiredParcelableExtra<PalletColor>(Const.EXTRA_PALLET)
    }

    private val toggleDelayTime = TimeUnit.MILLISECONDS.toMillis(150)
    private val toggleAnimTime = 200L

    private val toggleId = 0

    private val dlg by lazyNone {
        ProgressDialog.create(this, R.string.msg_loading)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbarActionbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initView()
        fragmentInit()
    }

    private fun initView() {
        variantAnimator().start()

        binding.tvSubTitle.text = ""
        binding.btnPrev.isEnabled = false
        binding.btnNext.isEnabled = false

        binding.btnPrev.setOnClickListener { viewModel.movePrev() }
        binding.btnNext.setOnClickListener { viewModel.moveNext() }

        viewModel.event.observeNonNull(this) { event ->
            when (event) {
                DetailEvent.START -> dlg.show()
                DetailEvent.LOADED -> dlg.dismiss()
                is DetailEvent.ERROR -> {
                    dlg.dismiss()
                    showError(event)
                }
                is DetailEvent.SHARE -> {
                    showShare(event.item)
                }
            }
        }
        viewModel.elementEvent.observeNonNull(this) { event ->
            binding.tvTitle.text = event.title
            binding.tvSubTitle.text = event.webToonTitle
            binding.btnPrev.isEnabled = event.prevEpisodeId.isNullOrEmpty().not()
            binding.btnNext.isEnabled = event.nextEpisodeId.isNullOrEmpty().not()
        }
    }

    private fun variantAnimator(): Animator = animatorColor(
        startColor = getThemeColor(R.attr.colorPrimary),
        endColor = palletColor.darkVibrantColor
    ).apply {
        duration = 1000L
        interpolator = DecelerateInterpolator()
        addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            binding.toolbarActionbar.setBackgroundColor(value)
            binding.btnPrev.backgroundTintList = stateListBgDrawable(value)
            binding.btnNext.backgroundTintList = stateListBgDrawable(value)

            this@DetailActivity.setStatusBarColor(value)
        }
    }

    private fun stateListBgDrawable(color: Int): ColorStateList = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_enabled)
        ),
        intArrayOf(
            Color.GRAY,
            color
        )
    )

    private fun fragmentInit() {
        supportFragmentManager.commit {
            replace(R.id.container, DetailFragment())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        when (item.itemId) {
            R.id.menu_item_share -> {
                // 공유하기
                viewModel.requestShare()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private val mToggleHandler = Handler(Looper.getMainLooper()) {
        toggleHideBar()
        true
    }

    /**
     * Detects and toggles immersive mode.
     */
    private fun toggleHideBar() {
        val isHide = SystemUiHelper.toggleHideBar(window)
        if (isHide) {
            moveToAxisY(binding.toolbarActionbar, true)
            moveToAxisY(binding.bottomMenu, false)
        } else {
            moveRevert(binding.toolbarActionbar)
            moveRevert(binding.bottomMenu)
        }
    }

    private fun moveToAxisY(view: View, isToTop: Boolean) {
        view.animate()
            .setDuration(toggleAnimTime)
            .translationY((if (isToTop) -view.height else view.height).toFloat())
            .start()
    }

    private fun moveRevert(view: View) {
        view.animate()
            .setDuration(toggleAnimTime)
            .translationY(0f)
            .start()
    }

    private fun toggleDelay(isDelay: Boolean) {
        mToggleHandler.removeMessages(toggleId)
        mToggleHandler.sendEmptyMessageDelayed(toggleId, if (isDelay) toggleDelayTime else 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        return true
    }

    override fun finish() {
        setResult(Activity.RESULT_OK)
        super.finish()
    }

    override fun childCallToggle(isDelay: Boolean) {
        toggleDelay(isDelay)
    }

    override fun loadingHide() {
        dlg.dismiss()
    }

    override fun firstBind() {
//        val currentItem = currentItem
//        if (currentItem?.list?.isNotEmpty() == true) {
//            fragmentAttach(currentItem.list)
//        }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // /////////////////////////////////////////////////////////////////////////

    private fun showError(event: DetailEvent.ERROR) {
        AlertDialog.Builder(this@DetailActivity)
            .setMessage(event.errorType.getMessage(this))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                supportFragmentManager.findFragmentByTag(Const.DETAIL_FRAG_TAG) ?: finish()
            }
            .show()
    }

    private fun showShare(item: ShareItem) {
        startActivity(
            Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtras(
                    bundleOf(
                        Intent.EXTRA_SUBJECT to item.title,
                        Intent.EXTRA_TEXT to item.url
                    )
                )
            }, "Share")
        )
    }
}
