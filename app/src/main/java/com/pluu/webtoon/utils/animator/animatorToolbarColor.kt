package com.pluu.webtoon.utils.animator

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import com.pluu.utils.animatorColor
import com.pluu.utils.animatorColorFromThemeAttrId
import com.pluu.utils.getThemeColor
import com.pluu.webtoon.R

fun AppCompatActivity.animatorToolbarColor(endColor: Int) =
    animatorColor(
        startColor = getThemeColor(R.attr.colorPrimary),
        endColor = endColor,
        listener = ValueAnimator.AnimatorUpdateListener {
            supportActionBar?.apply {
                setBackgroundDrawable(ColorDrawable(it.animatedValue as Int))
            }
        })

fun Activity.animatorStatusBarColor(color: Int) =
    animatorColorFromThemeAttrId(
        themeAttrId = R.attr.colorPrimaryVariant,
        color = color
    )