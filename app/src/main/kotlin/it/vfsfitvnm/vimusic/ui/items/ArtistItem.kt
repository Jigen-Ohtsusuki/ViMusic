package it.vfsfitvnm.vimusic.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.models.Artist
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.thumbnail
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.shimmer
import it.vfsfitvnm.core.ui.utils.px
import it.vfsfitvnm.providers.innertube.models.ArtistItem as InnertubeArtistItem
import coil3.compose.AsyncImage

@Composable
fun ArtistItem(
    artist: Artist,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ArtistItem(
    thumbnailUrl = artist.thumbnailUrl,
    name = artist.name,
    subscribersCount = null,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
fun ArtistItem(
    artist: InnertubeArtistItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ArtistItem(
    thumbnailUrl = artist.thumbnail,
    name = artist.title,
    subscribersCount = null,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
fun ArtistItem(
    thumbnailUrl: String?,
    name: String?,
    subscribersCount: String?,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ItemContainer(
    alternative = alternative,
    thumbnailSize = thumbnailSize,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clip(LocalAppearance.current.thumbnailShape) then modifier
) {
    val (_, typography) = LocalAppearance.current

    AsyncImage(
        model = thumbnailUrl?.thumbnail(thumbnailSize.px),
        contentDescription = null,
        modifier = Modifier
            .clip(CircleShape)
            .requiredSize(thumbnailSize)
    )

    ItemInfoContainer(
        horizontalAlignment = if (alternative) Alignment.CenterHorizontally else Alignment.Start
    ) {
        BasicText(
            text = name.orEmpty(),
            style = typography.xs.semiBold,
            maxLines = if (alternative) 1 else 2,
            overflow = TextOverflow.Ellipsis
        )

        subscribersCount?.let {
            BasicText(
                text = it,
                style = typography.xxs.semiBold.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ArtistItemPlaceholder(
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) {
    val (colorPalette) = LocalAppearance.current

    ItemContainer(
        alternative = alternative,
        thumbnailSize = thumbnailSize,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier
                .background(color = colorPalette.shimmer, shape = CircleShape)
                .size(thumbnailSize)
        )

        ItemInfoContainer(
            horizontalAlignment = if (alternative) Alignment.CenterHorizontally else Alignment.Start
        ) {
            TextPlaceholder()
            TextPlaceholder(modifier = Modifier.padding(top = 4.dp))
        }
    }
}
