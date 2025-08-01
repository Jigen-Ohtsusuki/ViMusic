package it.vfsfitvnm.vimusic.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.utils.isLandscape
import it.vfsfitvnm.providers.innertube.models.AlbumItem as InnertubeAlbumItem
import it.vfsfitvnm.providers.innertube.models.SongItem
import it.vfsfitvnm.providers.innertube.models.WatchEndpoint
import it.vfsfitvnm.providers.innertube.requests.ArtistPage
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.ShimmerHost
import it.vfsfitvnm.vimusic.ui.components.themed.*
import it.vfsfitvnm.vimusic.ui.items.AlbumItem
import it.vfsfitvnm.vimusic.ui.items.AlbumItemPlaceholder
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.items.SongItemPlaceholder
import it.vfsfitvnm.vimusic.utils.*

private val sectionTextModifier = Modifier
    .padding(horizontal = 16.dp)
    .padding(top = 24.dp, bottom = 8.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistOverview(
    youtubeArtistPage: ArtistPage?,
    onViewAllSongsClick: () -> Unit,
    onViewAllAlbumsClick: () -> Unit,
    onViewAllSinglesClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    thumbnailContent: @Composable () -> Unit,
    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
    modifier: Modifier = Modifier
) = LayoutWithAdaptiveThumbnail(
    thumbnailContent = thumbnailContent,
    modifier = modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val scrollState = rememberScrollState()

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
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
            Box(modifier = Modifier.padding(endPaddingValues)) {
                headerContent {
                    // Access shuffleEndpoint via the new 'artist' object
                    youtubeArtistPage?.artist?.shuffleEndpoint?.let { endpoint ->
                        SecondaryTextButton(
                            text = stringResource(R.string.shuffle),
                            onClick = {
                                binder?.stopRadio()
                                binder?.playRadio(endpoint)
                            }
                        )
                    }
                    // Note: subscribersCountText is not available in the new ArtistPage model.
                }
            }

            if (!isLandscape) thumbnailContent()

            if (youtubeArtistPage != null) {
                // Sections are now a list, so we must find the ones we need by title.
                val songsSection = youtubeArtistPage.sections.find { it.title == "Songs" }
                val albumsSection = youtubeArtistPage.sections.find { it.title == "Albums" }
                val singlesSection = youtubeArtistPage.sections.find { it.title == "Singles" }

                songsSection?.let { section ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(endPaddingValues)
                    ) {
                        BasicText(
                            text = stringResource(R.string.songs),
                            style = typography.m.semiBold,
                            modifier = sectionTextModifier
                        )

                        section.moreEndpoint?.let {
                            BasicText(
                                text = stringResource(R.string.view_all),
                                style = typography.xs.secondary,
                                modifier = sectionTextModifier.clickable(onClick = onViewAllSongsClick)
                            )
                        }
                    }

                    val (currentMediaId, playing) = playingSong(binder)

                    section.items.forEach { item ->
                        (item as? SongItem)?.let { song ->
                            SongItem(
                                song = song,
                                thumbnailSize = Dimensions.thumbnails.song,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem
                                                )
                                            }
                                        },
                                        onClick = {
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            // Replaced outdated NavigationEndpoint.Endpoint.Watch
                                            binder?.setupRadio(
                                                WatchEndpoint(videoId = mediaItem.mediaId)
                                            )
                                        }
                                    )
                                    .padding(endPaddingValues),
                                // Replaced .key with .id (which is the videoId)
                                isPlaying = playing && currentMediaId == song.id
                            )
                        }
                    }
                }

                albumsSection?.let { section ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(endPaddingValues)
                    ) {
                        BasicText(
                            text = stringResource(R.string.albums),
                            style = typography.m.semiBold,
                            modifier = sectionTextModifier
                        )

                        section.moreEndpoint?.let {
                            BasicText(
                                text = stringResource(R.string.view_all),
                                style = typography.xs.secondary,
                                modifier = sectionTextModifier.clickable(onClick = onViewAllAlbumsClick)
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = endPaddingValues,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = section.items,
                            // Replaced Innertube.AlbumItem::key with a lambda using the correct browseId
                            key = { (it as InnertubeAlbumItem).browseId }
                        ) { item ->
                            (item as? InnertubeAlbumItem)?.let { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSize = Dimensions.thumbnails.album,
                                    alternative = true,
                                    modifier = Modifier.clickable {
                                        onAlbumClick(album.browseId)
                                    }
                                )
                            }
                        }
                    }
                }

                singlesSection?.let { section ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(endPaddingValues)
                    ) {
                        BasicText(
                            text = stringResource(R.string.singles),
                            style = typography.m.semiBold,
                            modifier = sectionTextModifier
                        )

                        section.moreEndpoint?.let {
                            BasicText(
                                text = stringResource(R.string.view_all),
                                style = typography.xs.secondary,
                                modifier = sectionTextModifier.clickable(onClick = onViewAllSinglesClick)
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = endPaddingValues,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = section.items,
                            key = { (it as InnertubeAlbumItem).browseId }
                        ) { item ->
                            (item as? InnertubeAlbumItem)?.let { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSize = Dimensions.thumbnails.album,
                                    alternative = true,
                                    modifier = Modifier.clickable(onClick = { onAlbumClick(album.browseId) })
                                )
                            }
                        }
                    }
                }

                // Description is now a direct property on ArtistPage
                youtubeArtistPage.description?.let { description ->
                    Attribution(
                        text = description,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .padding(vertical = 16.dp, horizontal = 8.dp)
                    )
                }
            } else {
                ArtistOverviewBodyPlaceholder()
            }
        }

        // Access radioEndpoint via the new 'artist' object
        youtubeArtistPage?.artist?.radioEndpoint?.let { endpoint ->
            FloatingActionsContainerWithScrollToTop(
                scrollState = scrollState,
                icon = R.drawable.radio,
                onClick = {
                    binder?.stopRadio()
                    binder?.playRadio(endpoint)
                }
            )
        }
    }
}

@Composable
fun ArtistOverviewBodyPlaceholder(modifier: Modifier = Modifier) = ShimmerHost(
    modifier = modifier
) {
    TextPlaceholder(modifier = sectionTextModifier)

    repeat(5) {
        SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
    }

    repeat(2) {
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
