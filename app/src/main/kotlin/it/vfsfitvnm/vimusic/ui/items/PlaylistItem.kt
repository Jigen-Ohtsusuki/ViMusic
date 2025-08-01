package it.vfsfitvnm.vimusic.ui.items

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.models.PlaylistPreview
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.thumbnail
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.onOverlay
import it.vfsfitvnm.core.ui.overlay
import it.vfsfitvnm.core.ui.shimmer
import it.vfsfitvnm.core.ui.utils.px
import it.vfsfitvnm.core.ui.utils.roundedShape
import it.vfsfitvnm.providers.innertube.models.PlaylistItem as InnertubePlaylistItem
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Composable
fun PlaylistItem(
    @DrawableRes icon: Int,
    colorTint: Color,
    name: String?,
    songCount: Int?,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = PlaylistItem(
    thumbnailContent = {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorTint),
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
        )
    },
    songCount = songCount,
    name = name,
    channelName = null,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
fun PlaylistItem(
    playlist: PlaylistPreview, // This refers to it.vfsfitvnm.vimusic.models.PlaylistPreview
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) {
    val thumbnailSizePx = thumbnailSize.px
    val thumbnails by remember {
        playlist.thumbnail?.let { flowOf(listOf(it)) }
            ?: Database.instance
                .playlistThumbnailUrls(playlist.playlist.id)
                .distinctUntilChanged()
                .map { urls ->
                    urls.map { it.thumbnail(thumbnailSizePx / 2) }
                }
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    PlaylistItem(
        thumbnailContent = {
            if (thumbnails.toSet().size == 1) AsyncImage(
                model = thumbnails.first().thumbnail(thumbnailSizePx),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = it
            ) else Box(modifier = it.fillMaxSize()) {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).forEachIndexed { index, alignment ->
                    AsyncImage(
                        model = thumbnails.getOrNull(index),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(alignment)
                            .fillMaxSize(.5f)
                    )
                }
            }
        },
        songCount = playlist.songCount,
        name = playlist.playlist.name,
        channelName = null,
        thumbnailSize = thumbnailSize,
        modifier = modifier,
        alternative = alternative
    )
}

@Composable
fun PlaylistItem(
    playlist: InnertubePlaylistItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = PlaylistItem(
    thumbnailUrl = playlist.thumbnail,
    songCount = playlist.songCountText?.filter { it.isDigit() }?.toIntOrNull(),
    name = playlist.title,
    channelName = playlist.author?.name,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
fun PlaylistItem(
    thumbnailUrl: String?,
    songCount: Int?,
    name: String?,
    channelName: String?,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = PlaylistItem(
    thumbnailContent = {
        AsyncImage(
            model = thumbnailUrl?.thumbnail(thumbnailSize.px),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = it
        )
    },
    songCount = songCount,
    name = name,
    channelName = channelName,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
fun PlaylistItem(
    thumbnailContent: @Composable BoxScope.(modifier: Modifier) -> Unit,
    songCount: Int?,
    name: String?,
    channelName: String?,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ItemContainer(
    alternative = alternative,
    thumbnailSize = thumbnailSize,
    modifier = Modifier.clip(
        (
            LocalAppearance.current.thumbnailShapeCorners +
                if (alternative) Dimensions.items.alternativePadding else Dimensions.items.horizontalPadding
            ).roundedShape
    ) then modifier
) { centeredModifier ->
    val (colorPalette, typography, thumbnailShapeCorners) = LocalAppearance.current

    Box(
        modifier = centeredModifier
            .clip(thumbnailShapeCorners.roundedShape)
            .background(color = colorPalette.background1)
            .let {
                if (alternative) it
                    .sizeIn(
                        minWidth = thumbnailSize,
                        minHeight = thumbnailSize
                    )
                    .fillMaxWidth()
                    .aspectRatio(1f)
                else it.requiredSize(thumbnailSize)
            }
    ) {
        thumbnailContent(Modifier.fillMaxSize())

        songCount?.let {
            BasicText(
                text = "$songCount",
                style = typography.xxs.medium.color(colorPalette.onOverlay),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(all = Dimensions.items.gap)
                    .background(
                        color = colorPalette.overlay,
                        shape = (thumbnailShapeCorners - Dimensions.items.gap).coerceAtLeast(0.dp).roundedShape
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }

    ItemInfoContainer(modifier = if (alternative && channelName.isNullOrBlank()) centeredModifier else Modifier) {
        BasicText(
            text = name.orEmpty(),
            style = typography.xs.semiBold.let { if (alternative && channelName.isNullOrBlank()) it.center else it },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (channelName?.isNotBlank() == true) BasicText(
            text = channelName,
            style = typography.xs.semiBold.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistItemPlaceholder(
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ItemContainer(
    alternative = alternative,
    thumbnailSize = thumbnailSize,
    modifier = modifier
) {
    val (colorPalette, _, _, thumbnailShape) = LocalAppearance.current

    Spacer(
        modifier = Modifier
            .background(color = colorPalette.shimmer, shape = thumbnailShape)
            .size(thumbnailSize)
    )

    ItemInfoContainer(
        horizontalAlignment = if (alternative) Alignment.CenterHorizontally else Alignment.Start
    ) {
        TextPlaceholder()
        TextPlaceholder()
    }
}
