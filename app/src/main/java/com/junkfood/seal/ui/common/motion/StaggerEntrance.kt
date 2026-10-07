package com.junkfood.seal.ui.common.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * 交错入场动画（Stagger Delay + Spring Cascade）
 *
 * 每个子项按 [index] 计算延迟：delay = min(index * 30ms, 300ms)，
 * 入场为微弱弹性上滑（spring, dampingRatio=0.85, stiffness=260）+ 淡入。
 *
 * @param index 子项在列表中的位置，用于计算交错延迟
 * @param playAnimation 是否播放动画（用于"每 item 生命周期只播一次"的去重）
 * @param onPlayed 动画播放（或跳过）后回调，调用方在此标记该 item 已播过
 * @param animationsEnabled 动画总开关，关闭时直接渲染无动画
 */
@Composable
fun StaggerEntranceItem(
    index: Int,
    playAnimation: Boolean,
    onPlayed: () -> Unit,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!animationsEnabled || !playAnimation) {
        // 开关关闭或已播过：直接渲染，不做任何动画
        LaunchedEffect(Unit) { onPlayed() }
        Box(modifier = modifier) { content() }
        return
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(min(index * STAGGER_DELAY_PER_ITEM_MS, STAGGER_MAX_DELAY_MS))
        visible = true
        onPlayed()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(FADE_IN_DURATION_MS)) +
                slideInVertically(
                    initialOffsetY = { (it * ENTRANCE_SLIDE_FRACTION).toInt() },
                    animationSpec = spring(
                        dampingRatio = SPRING_DAMPING_RATIO,
                        stiffness = SPRING_STIFFNESS,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                ),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

private const val STAGGER_DELAY_PER_ITEM_MS = 30L
private const val STAGGER_MAX_DELAY_MS = 300L
private const val FADE_IN_DURATION_MS = 180

/** 上滑距离：item 高度的 12%（约 24~32dp，微弱弹性） */
private const val ENTRANCE_SLIDE_FRACTION = 0.12f

/** 弹簧参数：低刚度 + 高阻尼 = 微弱弹性，不晃眼 */
private const val SPRING_DAMPING_RATIO = 0.85f
private const val SPRING_STIFFNESS = 260f
