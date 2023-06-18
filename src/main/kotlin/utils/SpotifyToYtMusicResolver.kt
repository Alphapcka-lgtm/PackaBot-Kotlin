package utils

import PROPERTIES
import music.spotify.SpotifyProvider
import org.alphapacka.com.YTMusic
import org.alphapacka.com.enums.SearchFilters
import org.alphapacka.com.pojos.search.SearchResult
import se.michaelthelin.spotify.enums.ModelObjectType
import se.michaelthelin.spotify.model_objects.specification.Album
import se.michaelthelin.spotify.model_objects.specification.Playlist
import se.michaelthelin.spotify.model_objects.specification.Track
import java.util.*

/**
 * Class providing methods to resolve a spotify url to a possible YouTube music item.
 *
 * @author alphapacka
 */
class SpotifyToYtMusicResolver {

    private val provider =
        SpotifyProvider(PROPERTIES.getProperty("spotify.clientId"), PROPERTIES.getProperty("spotify.clientSecret"))
    private val ytMusic = YTMusic(null, null, null)

    private val ytMusicBaseUrl = "https://music.youtube.com/"
    private val songVideoUrlKey = "watch?v="
    private val albumPlaylistUrlKey = "playlist?list="

    /**
     * Resolves the given spotify url to an equivalent YouTube music item.
     * The spotify item to resolve needs to be of the type [ModelObjectType.ALBUM], [ModelObjectType.PLAYLIST] or [ModelObjectType.TRACK].
     *
     * @return a collection of a strings. The collection will be empty if an equivalent YouTube music item has not been found. The collection will contain only one item when the found item was a track or an album. If the item was a playlist, the collection will contain multiple items.
     * @throws IllegalArgumentException if the provided argument is not a spotify url.
     * @throws IllegalStateException if the provided spotify item is not of the [ModelObjectType.ALBUM], [ModelObjectType.PLAYLIST] or [ModelObjectType.TRACK].
     */
    fun resolve(spotifyUrl: String): Collection<String> {
        if (!SpotifyProvider.isSpotifyUrl(spotifyUrl)) throw IllegalArgumentException("Provided argument is not a spotify url!")

        val res = provider.retrieve(spotifyUrl)

        return when (res.type) {
            ModelObjectType.TRACK -> {
                val opt = resolveTrack(res.objectAsTrack())
                if (opt.isPresent) {
                    listOf<String>(opt.get())
                } else {
                    emptyList<String>()
                }
            }

            ModelObjectType.ALBUM -> {
                val opt = resolveAlbum(res.objectAsAlbum())
                if (opt.isPresent) {
                    listOf<String>(opt.get())
                } else {
                    emptyList()
                }
            }

            ModelObjectType.PLAYLIST -> resolvePlaylist(res.objectAsPlaylist())

            else -> throw IllegalStateException("Invalid spotify type: ${res.type}. Only request tracks, playlists or albums!")
        }
    }

    private fun resolveTrack(track: Track): Optional<String> {
        val songsResult =
            ytMusic.search("${track.artists.joinToString()} - ${track.name}", SearchFilters.SONGS).stream()
                .map(SearchResult::asSong).toList()
        val spotifyArtistNames = track.artists.asList().map { el -> el.name }
        for (song in songsResult) {
            val ytArtistNames = song.artists.map { el -> el.name }
            var hasAllArtists = true
            for (ytArtist in ytArtistNames) {
                if (!spotifyArtistNames.contains(ytArtist)) {
                    hasAllArtists = false
                }
            }

            if (song.title == track.name && hasAllArtists) {
                return Optional.of("$ytMusicBaseUrl$songVideoUrlKey${song.videoId}")
            }
        }

        // when no song was found check if there are videos
        val videoResults =
            ytMusic.search("${track.artists.joinToString()} - ${track.name}", SearchFilters.VIDEOS).stream()
                .map(SearchResult::asVideo).toList()

        for (video in videoResults) {
            val ytArtistNames = video.artists.map { it.name }
            var hasAllArtists = true
            for (ytArtist in ytArtistNames) {
                if (!spotifyArtistNames.contains(ytArtist)) {
                    hasAllArtists = false
                }
            }

            if (video.title == track.name && hasAllArtists) {
                return Optional.of("$ytMusicBaseUrl$songVideoUrlKey${video.videoId}")
            }
        }

        return Optional.empty<String>()
    }

    private fun resolveAlbum(album: Album): Optional<String> {
        val albumResults =
            ytMusic.search("${album.name} - ${album.artists.joinToString()}", SearchFilters.ALBUMS).stream()
                .map(SearchResult::asAlbum).toList()

        val spotifyArtistsName = album.artists.asList().map { it.name }
        for (ytAlbum in albumResults) {
            val ytAlbumArtistsName = ytAlbum.artists.map { it.name }
            var hasAllArtists = true
            for (ytArtist in ytAlbumArtistsName) {
                if (!spotifyArtistsName.contains(ytArtist)) {
                    hasAllArtists = false
                }
            }

            if (ytAlbum.title == album.name && hasAllArtists) {
                return Optional.of(ytMusic.getAlbum(ytAlbum.browseId).audioPlaylistId)
            }
        }

        return Optional.empty()
    }

    private fun resolvePlaylist(playlist: Playlist): Collection<String> {
        val playlistTracks = LinkedList<Track>()
        for (pt in playlist.tracks.items) {
            if (pt.track.type == ModelObjectType.TRACK) {
                playlistTracks.add(pt.track as Track)
            }
        }

        var _playlist = playlist
        while (_playlist.tracks.next != null) {
            _playlist = provider.retrieve(_playlist.tracks.next).objectAsPlaylist()
            for (pt in _playlist.tracks.items) {
                if (pt.track.type == ModelObjectType.TRACK) {
                    playlistTracks.add(pt.track as Track)
                }
            }
        }

        val tracksUrl = ArrayList<String>(playlistTracks.size)
        for (pt in playlistTracks) {
            resolveTrack(pt).ifPresent { url ->
                tracksUrl.add(url)
            }
        }

        return tracksUrl
    }

}