package com.junkfood.seal.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.junkfood.seal.ui.common.motion.StaggerEntranceItem

/**
 * 流体形态变换（Fluid Morph）：胶囊按钮 <-> 弹窗面板
 *
 * 同一个容器，尺寸（宽 + 内容高度自适应）与圆角同时做高阻尼弹簧变换，
 * 内容用淡入淡出交叉过渡。关闭动画总开关时退化为无动画的直接切换。
 *
 * @param expanded 是否展开为面板
 * @param onExpandedChange 展开状态变化回调
 * @param animationsEnabled 动画总开关
 * @param buttonContent 胶囊按钮内容
 * @param panelContent 弹窗面板内容
 */
@Composable
fun FluidMorphDialog(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    collapsedWidth: Dp = 220.dp,
    expandedWidth: Dp = 320.dp,
    buttonContent: @Composable RowScope.() -> Unit,
    panelContent: @Composable ColumnScope.() -> Unit
) {
    // 高阻尼流体曲线：阻尼比 0.9（几乎无回弹）+ 低刚度（舒展的流体感）
    val morphSpec = remember(animationsEnabled) {
        if (animationsEnabled) spring<Dp>(dampingRatio = 0.9f, stiffness = 170f)
        else snap()
    }
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 28.dp else 100.dp, // 100dp ≈ 胶囊
        animationSpec = morphSpec,
        label = "fluidMorphCorner"
    )
    val containerWidth by animateDpAsState(
        targetValue = if (expanded) expandedWidth else collapsedWidth,
        animationSpec = morphSpec,
        label = "fluidMorphWidth"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 展开时的背景蒙层，点击关闭
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onExpandedChange(false) }
            )
        }

        // 同一个容器做形态变换：宽 + 圆角动画，高度随内容自适应动画
        Surface(
            modifier = Modifier
                .width(containerWidth)
                .animateContentSize(
                    animationSpec = if (animationsEnabled)
                        spring(dampingRatio = 0.9f, stiffness = 170f)
                    else snap()
                )
                .clip(RoundedCornerShape(cornerRadius))
                .clickable(
                    enabled = !expanded,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpandedChange(true) },
            shape = RoundedCornerShape(cornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = if (expanded) 6.dp else 1.dp,
            shadowElevation = if (expanded) 8.dp else 2.dp
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "fluidMorphContent"
            ) { isExpanded ->
                if (isExpanded) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        panelContent()
                    }
                } else {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        buttonContent()
                    }
                }
            }
        }
    }
}

/**
 * 流体面板（Overlay 版）：全屏蒙层 + 面板从胶囊尺寸流体变换为完整面板。
 * 用于"按钮→弹窗"场景：由外部按钮触发，入场播放胶囊→面板的 morph；
 * 关闭时反向 morph 收回胶囊再消失（开合对称）。
 *
 * 尺寸（宽）与圆角同时做高阻尼弹簧变换（damping 0.9，几乎无回弹），
 * 内容用淡入淡出交叉过渡。关闭动画总开关时退化为无动画的直接切换。
 */
@Composable
fun FluidMorphPanel(
    animationsEnabled: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    panelWidth: Dp = 340.dp,
    capsuleContent: (@Composable RowScope.() -> Unit)? = null,
    panelContent: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // 用 Animatable 保证反向 morph 完成的时机精确
    val widthAnim = remember { Animatable(COLLAPSED_WIDTH, Dp.VectorConverter) }
    val cornerAnim = remember { Animatable(COLLAPSED_CORNER, Dp.VectorConverter) }
    // 位置动画：关闭时胶囊飞回右下角 FAB 处
    val offsetAnim = remember { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }
    // 落位后的淡出，避免胶囊在右下角突兀消失
    val alphaAnim = remember { Animatable(1f) }
    var contentExpanded by remember { mutableStateOf(false) }
    var scrimTarget by remember { mutableStateOf(0f) }
    val scrimAlpha by animateFloatAsState(
        targetValue = scrimTarget,
        animationSpec = tween(300),
        label = "fluidScrimAlpha"
    )

    val morphSpec: FiniteAnimationSpec<Dp> = remember(animationsEnabled) {
        if (animationsEnabled) spring(dampingRatio = 0.9f, stiffness = 170f)
        else snap()
    }
    val offsetSpec: FiniteAnimationSpec<IntOffset> = remember(animationsEnabled) {
        if (animationsEnabled) spring(dampingRatio = 0.9f, stiffness = 170f)
        else snap()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 下载 FAB 中心相对屏幕中心的位置（右下角，约 24dp 边距 + 28dp 半径）
        val fabOffset = remember(maxWidth, maxHeight) {
            with(density) {
                IntOffset(
                    x = ((maxWidth - 104.dp) / 2).roundToPx(),
                    y = ((maxHeight - 104.dp) / 2).roundToPx()
                )
            }
        }
        // 面板最高占屏幕 85%，超出部分内部滚动
        val maxPanelHeight = maxHeight * 0.85f

        // 入场：胶囊 → 面板（内容延迟淡入，等宽度展开约 60%）
        LaunchedEffect(Unit) {
            if (animationsEnabled) {
                scrimTarget = 0.4f
                launch { widthAnim.animateTo(panelWidth, morphSpec) }
                launch { cornerAnim.animateTo(EXPANDED_CORNER, morphSpec) }
                launch {
                    delay(180)
                    contentExpanded = true
                }
            } else {
                widthAnim.snapTo(panelWidth)
                cornerAnim.snapTo(EXPANDED_CORNER)
                scrimTarget = 0.4f
                contentExpanded = true
            }
        }

        // 关闭：面板 → 胶囊（飞回 FAB 位置）→ 淡出消失
        fun dismiss() {
            scope.launch {
                if (animationsEnabled) {
                    contentExpanded = false
                    scrimTarget = 0f
                    coroutineScope {
                        launch { widthAnim.animateTo(COLLAPSED_WIDTH, morphSpec) }
                        launch { cornerAnim.animateTo(COLLAPSED_CORNER, morphSpec) }
                        launch { offsetAnim.animateTo(fabOffset, offsetSpec) }
                    }
                    alphaAnim.animateTo(0f, tween(150))
                }
                onDismissRequest()
            }
        }

        // 背景蒙层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismiss() }
        )
        // 面板：居中 + 位置偏移
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .offset { offsetAnim.value }
                    .graphicsLayer { alpha = alphaAnim.value }
                    .width(widthAnim.value)
                    .heightIn(max = maxPanelHeight)
                    .clip(RoundedCornerShape(cornerAnim.value)),
                shape = RoundedCornerShape(cornerAnim.value),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                AnimatedContent(
                    targetState = contentExpanded,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "fluidPanelContent"
                ) { isExpanded ->
                    if (isExpanded) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            panelContent()
                        }
                    } else if (capsuleContent != null) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            capsuleContent()
                        }
                    }
                }
            }
        }
    }
}

private val COLLAPSED_WIDTH = 220.dp
private val COLLAPSED_CORNER = 100.dp
private val EXPANDED_CORNER = 28.dp

/**
 * 动画预览面板内容：迷你交错演示 + 说明
 */
@Composable
fun ColumnScope.AnimationPreviewContent(
    animationsEnabled: Boolean,
    onDismissRequest: () -> Unit
) {
    Text(
        text = "动画效果预览",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "关闭「界面动画」开关后，所有动效退化为直接切换。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "交错入场 · Stagger",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    MiniStaggerDemo(animationsEnabled = animationsEnabled)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "流体形态 · Fluid Morph",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "当前面板即由胶囊按钮流体变换而来：圆角 100dp→28dp，宽度同步舒展。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onDismissRequest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("关闭")
    }
}

@Composable
private fun MiniStaggerDemo(animationsEnabled: Boolean) {
    var playedKeys by remember { mutableStateOf(setOf<Int>()) }
    var replayKey by remember { mutableStateOf(0) }

    key(replayKey) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                StaggerEntranceItem(
                    index = index,
                    playAnimation = !playedKeys.contains(index),
                    onPlayed = { playedKeys = playedKeys + index },
                    animationsEnabled = animationsEnabled
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "示例条目 ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
    TextButton(
        onClick = {
            playedKeys = emptySet()
            replayKey++
        }
    ) {
        Text("重播")
    }
}
