package it.vfsfitvnm.vimusic.ui.screens.mood

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.components.ShimmerHost
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.HeaderPlaceholder
import it.vfsfitvnm.vimusic.ui.items.AlbumItem
import it.vfsfitvnm.vimusic.ui.items.AlbumItemPlaceholder
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.providers.innertube.YouTube
import it.vfsfitvnm.providers.innertube.models.AlbumItem as InnertubeAlbumItem
import it.vfsfitvnm.providers.innertube.requests.BrowseResult
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_BROWSE_ID = "FEmusic_new_releases_albums"

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MoreAlbumsList(
    onAlbumClick: (browseId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    var albumsPage by persist<BrowseResult>(tag = "more_albums/list")
    val data by remember {
        derivedStateOf {
            albumsPage
                ?.items
                ?.firstOrNull()
                ?.items
                ?.filterIsInstance<InnertubeAlbumItem>()
                ?.toImmutableList()
        }
    }

    LaunchedEffect(Unit) { // This provides the coroutine scope
        if (albumsPage != null) return@LaunchedEffect

        // All the code inside this block is running in a coroutine
        val result = withContext(Dispatchers.IO) { // withContext is fine here
            runCatching {
                YouTube.browse( // YouTube.browse is fine here
                    browseId = DEFAULT_BROWSE_ID,
                    params = null
                )
            }
        }

        result.onSuccess { newAlbumsPage ->
        }.onFailure {
            it.printStackTrace()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(Dimensions.thumbnails.album + Dimensions.items.horizontalPadding),
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
            span = { GridItemSpan(maxLineSpan) }
        ) {
            if (albumsPage == null) HeaderPlaceholder(modifier = Modifier.shimmer())
            else Header(
                title = stringResource(R.string.new_released_albums),
                modifier = Modifier.padding(endPaddingValues)
            )
        }

        data?.let { page ->
            itemsIndexed(
                items = page,
                key = { i, item -> "item:$i,${item.browseId}" }
            ) { _, album ->
                BoxWithConstraints {
                    AlbumItem(
                        album = album,
                        thumbnailSize = maxWidth - Dimensions.items.horizontalPadding * 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAlbumClick(album.browseId)
                            },
                        alternative = true
                    )
                }
            }
        }

        if (albumsPage == null) item(
            key = "loading",
            contentType = 0,
            span = { GridItemSpan(maxLineSpan) }
        ) {
            ShimmerHost(modifier = Modifier.fillMaxWidth()) {
                repeat(16) {
                    AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album)
                }
            }
        }
    }
}
