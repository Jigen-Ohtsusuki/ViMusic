package it.vfsfitvnm.providers.innertube.requests

import it.vfsfitvnm.providers.innertube.models.Album
import it.vfsfitvnm.providers.innertube.models.AlbumItem
import it.vfsfitvnm.providers.innertube.models.Artist
import it.vfsfitvnm.providers.innertube.models.ArtistItem
import it.vfsfitvnm.providers.innertube.models.MusicResponsiveListItemRenderer
import it.vfsfitvnm.providers.innertube.models.MusicTwoRowItemRenderer
import it.vfsfitvnm.providers.innertube.models.PlaylistItem
import it.vfsfitvnm.providers.innertube.models.SongItem
import it.vfsfitvnm.providers.innertube.models.YTItem
import it.vfsfitvnm.providers.innertube.models.oddElements
import it.vfsfitvnm.providers.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
