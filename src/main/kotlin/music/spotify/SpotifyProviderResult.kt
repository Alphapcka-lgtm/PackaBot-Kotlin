package music.spotify

import se.michaelthelin.spotify.enums.ModelObjectType
import se.michaelthelin.spotify.model_objects.AbstractModelObject
import se.michaelthelin.spotify.model_objects.specification.Album
import se.michaelthelin.spotify.model_objects.specification.Playlist
import se.michaelthelin.spotify.model_objects.specification.Track

data class SpotifyProviderResult(
    val type: ModelObjectType,
    private val obj: AbstractModelObject,
) {
    fun objectAsAlbum(): Album {
        return obj as Album
    }

    fun objectAsPlaylist(): Playlist {
        return obj as Playlist
    }

    fun objectAsTrack(): Track {
        return obj as Track
    }
}
