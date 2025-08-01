package it.vfsfitvnm.vimusic.ui.screens.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.components.ShimmerHost
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.HeaderPlaceholder
import it.vfsfitvnm.vimusic.ui.items.SongItemPlaceholder
import it.vfsfitvnm.vimusic.ui.screens.home.MoodItem
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.providers.innertube.YouTube
import it.vfsfitvnm.providers.innertube.requests.MoodAndGenres
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MoreMoodsList(
    onMoodClick: (mood: MoodAndGenres.Item) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2
) {
    val (colorPalette, typography) = LocalAppearance.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    // The data type has changed from BrowseResult to a List<MoodAndGenres>
    var moodsPage by persist<List<MoodAndGenres>>(tag = "more_moods/list")

    val data by remember {
        derivedStateOf {
            moodsPage?.flatMap { moodAndGenres ->
                // Flatten the list of MoodAndGenres into a list of pairs (title, items)
                listOf(moodAndGenres.title to moodAndGenres.items)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (moodsPage != null) return@LaunchedEffect

        val result = withContext(Dispatchers.IO) {
            // Call the new, specific API function
            YouTube.moodAndGenres()
        }

        result.onSuccess { newMoodsAndGenresList ->
            moodsPage = newMoodsAndGenresList
        }.onFailure {
            it.printStackTrace()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = windowInsets
            .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
            .asPaddingValues(),
        modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()
    ) {
        item(
            key = "header",
            contentType = 0,
            span = { GridItemSpan(columns) }
        ) {
            if (moodsPage == null) HeaderPlaceholder(modifier = Modifier.shimmer())
            else Header(
                title = stringResource(R.string.moods_and_genres),
                modifier = Modifier.padding(endPaddingValues)
            )
        }

        data?.let { page ->
            page.forEachIndexed { i, (title, moods) ->
                item(
                    key = "header:$i,$title",
                    contentType = 0,
                    span = { GridItemSpan(columns) }
                ) {
                    BasicText(
                        text = title,
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )
                }

                itemsIndexed(
                    items = moods,
                    key = { j, item -> "item:$j,${item.endpoint.browseId}" }
                ) { _, mood ->
                    MoodItem(
                        mood = mood,
                        onClick = { mood.endpoint.browseId.let { _ -> onMoodClick(mood) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                }
            }
        }

        if (moodsPage == null) item(
            key = "loading",
            contentType = 0,
            span = { GridItemSpan(columns) }
        ) {
            ShimmerHost(modifier = Modifier.fillMaxWidth()) {
                repeat(4) {
                    SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                }
            }
        }
    }
}
