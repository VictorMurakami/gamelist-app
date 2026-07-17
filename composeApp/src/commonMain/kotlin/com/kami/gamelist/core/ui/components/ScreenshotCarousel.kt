package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kami.gamelist.core.ui.model.ScreenshotUi

@Composable
fun ScreenshotCarousel(
    screenshots: List<ScreenshotUi>,
    modifier: Modifier = Modifier
) {
    if (screenshots.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { screenshots.size })

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        pageSpacing = 8.dp
    ) { page ->
        AsyncImage(
            model = screenshots[page].image,
            contentDescription = "Screenshot ${page + 1} of ${screenshots.size}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
        )
    }
}
