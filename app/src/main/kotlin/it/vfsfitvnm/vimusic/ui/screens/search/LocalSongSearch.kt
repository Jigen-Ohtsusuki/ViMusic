package it.vfsfitvnm.vimusic.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.Dimensions.Thumbnails.album
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.providers.innertube.models.WatchEndpoint
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.MenuState
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.InHistoryMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.utils.align
import it.vfsfitvnm.vimusic.utils.forcePlay
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.playingSong
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalSongSearch(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val menuState = LocalMenuState.current

    var items by persistList<Song>("search/local/songs")

    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.length > 1)
            Database.instance
                .search("%${textFieldValue.text}%")
                .collect { items = it.toImmutableList() }
    }

    val lazyListState = rememberLazyListState()

    Box(modifier = modifier) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                Header(
                    titleContent = {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = onTextFieldValueChange,
                            textStyle = typography.xxl.medium.align(TextAlign.End),
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            cursorBrush = SolidColor(colorPalette.text),
                            decorationBox = decorationBox
                        )
                    },
                    actionsContent = {
                        if (textFieldValue.text.isNotEmpty()) SecondaryTextButton(
                            text = stringResource(R.string.clear),
                            onClick = { onTextFieldValueChange(TextFieldValue()) }
                        )
                    }
                )
            }

            localSongList(
                items = items,
                menuState = menuState
            )
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }
}


/**
 * An extension function to convert a local database Song model to a MediaItem.
 */
fun Song.asMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setAlbumTitle(album.toString())
                .setArtworkUri(thumbnailUrl?.toUri())
                .build()
        )
        .build()
}

/**
 * A private, refactored composable for displaying a list of local songs within a LazyColumn.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.localSongList(
    items: List<Song>,
    menuState: MenuState
) {
    items(
        items = items,
        key = Song::id
    ) { song ->
        val binder = LocalPlayerServiceBinder.current
        val (currentMediaId, playing) = playingSong(binder)

        SongItem(
            modifier = Modifier
                .combinedClickable(
                    onLongClick = {
                        menuState.display {
                            InHistoryMediaItemMenu(
                                song = song,
                                onDismiss = menuState::hide
                            )
                        }
                    },
                    onClick = {
                        val mediaItem = song.asMediaItem()
                        binder?.stopRadio()
                        binder?.player?.forcePlay(mediaItem)
                        binder?.setupRadio(
                            WatchEndpoint(videoId = mediaItem.mediaId)
                        )
                    }
                )
                .animateItem(),
            song = song,
            thumbnailSize = Dimensions.thumbnails.song,
            isPlaying = playing && currentMediaId == song.id
        )
    }
}
