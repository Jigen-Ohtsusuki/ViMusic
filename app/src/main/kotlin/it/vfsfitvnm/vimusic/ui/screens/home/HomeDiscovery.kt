package it.vfsfitvnm.vimusic.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.shimmer
import it.vfsfitvnm.core.ui.utils.isLandscape
import it.vfsfitvnm.providers.innertube.YouTube
import it.vfsfitvnm.providers.innertube.requests.ExplorePage
import it.vfsfitvnm.providers.innertube.requests.MoodAndGenres
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.components.FadingRow
import it.vfsfitvnm.vimusic.ui.components.ShimmerHost
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.ui.items.AlbumItem
import it.vfsfitvnm.vimusic.ui.items.AlbumItemPlaceholder
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Route
@Composable
fun HomeDiscovery(
    onMoodClick: (mood: MoodAndGenres.Item) -> Unit,
    onNewReleaseAlbumClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMoreMoodsClick: () -> Unit,
    onMoreAlbumsClick: () -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val scrollState = rememberScrollState()
    val moodGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets
        .only(WindowInsetsSides.End)
        .asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    var explorePage by persist<Result<ExplorePage>?>("home/discovery")

    LaunchedEffect(Unit) {
        if (explorePage?.isSuccess != true) {
            withContext(Dispatchers.IO) {
                explorePage = YouTube.explore()
            }
        }
    }

    BoxWithConstraints {
        val widthFactor = if (isLandscape && maxWidth * 0.475f >= 320.dp) 0.475f else 0.75f
        val itemWidth = maxWidth * widthFactor

        Column(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Vertical)
                        .asPaddingValues()
                )
        ) {
            Header(
                title = stringResource(R.string.discover),
                modifier = Modifier.padding(endPaddingValues)
            )

            explorePage?.getOrNull()?.let { page ->
                if (page.moodAndGenres.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FadingRow(
                            modifier = Modifier.weight(
                                weight = 1f,
                                fill = false
                            )
                        ) {
                            BasicText(
                                text = stringResource(R.string.moods_and_genres),
                                style = typography.m.semiBold,
                                modifier = sectionTextModifier
                            )
                        }

                        SecondaryTextButton(
                            text = stringResource(R.string.more),
                            onClick = onMoreMoodsClick,
                            modifier = sectionTextModifier
                        )
                    }

                    LazyHorizontalGrid(
                        state = moodGridState,
                        rows = GridCells.Fixed(4),
                        contentPadding = endPaddingValues,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((4 * (64 + 4)).dp)
                    ) {
                        val moods = page.moodAndGenres.flatMap { moodAndGenres: MoodAndGenres -> moodAndGenres.items }
                        items(
                            items = moods.sortedBy { mood: MoodAndGenres.Item -> mood.title },
                            key = { mood: MoodAndGenres.Item -> mood.endpoint.params ?: mood.title }
                        ) { mood: MoodAndGenres.Item ->
                            MoodItem(
                                mood = mood,
                                onClick = { mood.endpoint.browseId.let { _ -> onMoodClick(mood) } },
                                modifier = Modifier
                                    .width(itemWidth)
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                if (page.newReleaseAlbums.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FadingRow(
                            modifier = Modifier.weight(
                                weight = 1f,
                                fill = false
                            )
                        ) {
                            BasicText(
                                text = stringResource(R.string.new_released_albums),
                                style = typography.m.semiBold,
                                modifier = sectionTextModifier
                            )
                        }

                        SecondaryTextButton(
                            text = stringResource(R.string.more),
                            onClick = onMoreAlbumsClick,
                            modifier = sectionTextModifier
                        )
                    }

                    LazyRow(contentPadding = endPaddingValues) {
                        items(items = page.newReleaseAlbums, key = { it.browseId }) {
                            AlbumItem(
                                album = it,
                                thumbnailSize = Dimensions.thumbnails.album,
                                alternative = true,
                                modifier = Modifier.clickable(onClick = { onNewReleaseAlbumClick(it.browseId) })
                            )
                        }
                    }
                }

            } ?: explorePage?.exceptionOrNull()?.let {
                BasicText(
                    text = stringResource(R.string.error_message),
                    style = typography.s.secondary.center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(all = 16.dp)
                )
            } ?: ShimmerHost {
                TextPlaceholder(modifier = sectionTextModifier)
                LazyHorizontalGrid(
                    state = moodGridState,
                    rows = GridCells.Fixed(4),
                    contentPadding = endPaddingValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4 * (Dimensions.items.moodHeight + 4.dp))
                ) {
                    items(16) {
                        MoodItemPlaceholder(
                            width = itemWidth,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                TextPlaceholder(modifier = sectionTextModifier)
                Row {
                    repeat(2) {
                        AlbumItemPlaceholder(
                            thumbnailSize = Dimensions.thumbnails.album,
                            alternative = true
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            scrollState = scrollState,
            icon = R.drawable.search,
            onClick = onSearchClick
        )
    }
}

@Composable
fun MoodItem(
    mood: MoodAndGenres.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalAppearance.current.typography
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    val color by remember { derivedStateOf { Color(mood.stripeColor.toULong()) } }

    ElevatedCard(
        modifier = modifier.height(Dimensions.items.moodHeight),
        shape = thumbnailShape,
        colors = CardDefaults.elevatedCardColors(containerColor = color)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicText(
                text = mood.title,
                style = typography.xs.semiBold.color(
                    if (color.luminance() >= 0.5f) Color.Black else Color.White
                ),
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

@Composable
fun MoodItemPlaceholder(
    width: Dp,
    modifier: Modifier = Modifier
) = Spacer(
    modifier = modifier
        .background(
            color = LocalAppearance.current.colorPalette.shimmer,
            shape = LocalAppearance.current.thumbnailShape
        )
        .size(
            width = width,
            height = Dimensions.items.moodHeight
        )
)
