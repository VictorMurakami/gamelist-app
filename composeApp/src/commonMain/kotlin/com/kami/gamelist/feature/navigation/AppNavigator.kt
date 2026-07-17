package com.kami.gamelist.feature.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.kami.gamelist.core.ui.components.LocalScrollToTop
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun AppNavigator() {
    val colors = GameTheme.colors

    TabNavigator(HomeTab) {
        val tabNavigator = LocalTabNavigator.current
        val tabs = remember { listOf(HomeTab, SearchTab, FavoritesTab, ListsTab, SettingsTab) }
        val scrollToTop = LocalScrollToTop.current

        Scaffold(
            containerColor = colors.backgroundDark,
            bottomBar = {
                GameTabBar(
                    tabs = tabs,
                    currentTab = tabNavigator.current,
                    onTabSelected = { tab ->
                        if (tabNavigator.current == tab) {
                            scrollToTop.requestScrollToTop()
                        } else {
                            tabNavigator.current = tab
                        }
                    }
                )
            }
        ) { paddingValues ->
            Crossfade(
                targetState = tabNavigator.current,
                animationSpec = tween(200),
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
            ) { tab ->
                tab.Content()
            }
        }
    }
}

@Composable
private fun GameTabBar(
    tabs: List<Tab>,
    currentTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GameTheme.colors
    val density = LocalDensity.current
    val tabCount = tabs.size
    val selectedIndex = tabs.indexOfFirst { it == currentTab }

    var barWidthPx by remember { mutableIntStateOf(0) }

    val indicatorOffsetFraction by animateFloatAsState(
        targetValue = if (tabCount > 0) selectedIndex.toFloat() / tabCount else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "indicator_offset"
    )

    Column(modifier = modifier.navigationBarsPadding()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.borderSubtle)
        )

        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.surfaceBase)
                .onSizeChanged { barWidthPx = it.width }
        ) {
            if (barWidthPx > 0 && selectedIndex >= 0) {
                val tabWidthPx = barWidthPx.toFloat() / tabCount
                val indicatorWidthDp = 28.dp
                val indicatorWidthPx = with(density) { indicatorWidthDp.toPx() }
                val offsetPx = indicatorOffsetFraction * barWidthPx + (tabWidthPx - indicatorWidthPx) / 2

                val glowWidthDp = 40.dp
                val glowWidthPx = with(density) { glowWidthDp.toPx() }
                val glowOffsetPx = indicatorOffsetFraction * barWidthPx + (tabWidthPx - glowWidthPx) / 2

                Box(
                    Modifier
                        .offset(x = with(density) { glowOffsetPx.toDp() }, y = 1.dp)
                        .width(glowWidthDp)
                        .height(5.dp)
                        .background(
                            colors.accent.copy(alpha = 0.12f),
                            RoundedCornerShape(2.5.dp)
                        )
                )

                Box(
                    Modifier
                        .offset(x = with(density) { offsetPx.toDp() }, y = 1.5.dp)
                        .width(indicatorWidthDp)
                        .height(3.dp)
                        .background(colors.accent, RoundedCornerShape(1.5.dp))
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp)
            ) {
                tabs.forEach { tab ->
                    GameTabItem(
                        tab = tab,
                        isSelected = tab == currentTab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTabItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GameTheme.colors

    val tintColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.textMuted,
        animationSpec = tween(250),
        label = "tab_tint"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "icon_scale"
    )

    Column(
        modifier = modifier
            .pressScale(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tab.options.icon?.let {
            Icon(
                painter = it,
                contentDescription = tab.options.title,
                tint = tintColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.options.title,
            color = tintColor,
            style = GameTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
