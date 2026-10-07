package com.junkfood.seal.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
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
 * 用于"预览"类场景：由外部按钮触发，入场即播放胶囊→面板的 morph。
 */
@Composable
fun FluidMorphPanel(
    animationsEnabled: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    panelContent: @Composable ColumnScope.() -> Unit
) {
    // 入场：首帧为胶囊尺寸，下一帧切到面板尺寸，触发流体变换
    var entrance by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entrance = true }

    val morphSpec: androidx.compose.animation.core.FiniteAnimationSpec<Dp> =
        remember(animationsEnabled) {
            if (animationsEnabled) spring(dampingRatio = 0.9f, stiffness = 170f)
            else snap()
        }
    val containerWidth by animateDpAsState(
        targetValue = if (entrance) 320.dp else 220.dp,
        animationSpec = morphSpec,
        label = "fluidPanelWidth"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (entrance) 28.dp else 100.dp,
        animationSpec = morphSpec,
        label = "fluidPanelCorner"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() }
        )
        Surface(
            modifier = Modifier
                .width(containerWidth)
                .animateContentSize(
                    animationSpec = if (animationsEnabled)
                        spring(dampingRatio = 0.9f, stiffness = 170f)
                    else snap()
                )
                .clip(RoundedCornerShape(cornerRadius)),
            shape = RoundedCornerShape(cornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                panelContent()
            }
        }
    }
}

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
    val playedKeys = remember { mutableStateSetOf<Int>() }
    var replayKey by remember { mutableStateOf(0) }

    key(replayKey) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                StaggerEntranceItem(
                    index = index,
                    playAnimation = !playedKeys.contains(index),
                    onPlayed = { playedKeys.add(index) },
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
            playedKeys.clear()
            replayKey++
        }
    ) {
        Text("重播")
    }
}
