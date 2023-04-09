package music.spotify

import net.dv8tion.jda.internal.utils.JDALogger
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.enums.ModelObjectType
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials
import se.michaelthelin.spotify.model_objects.specification.Album
import se.michaelthelin.spotify.model_objects.specification.Playlist
import se.michaelthelin.spotify.model_objects.specification.Track
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest

class SpotifyProvider(clientId: String, clientSecret: String) {

    private val LOG = JDALogger.getLog(SpotifyProvider::class.java)

    companion object {
        private val SPOTIFY_PATTERN = "^https://open.spotify.com/(.*)/([\\w]*).*$".toRegex()

        /**
         * @return `true` if the provided string is a spotify-share url.
         */
        fun isSpotifyUrl(url: String): Boolean {
            return SPOTIFY_PATTERN.matches(url)
        }
    }

    private val spotifyApi: SpotifyApi
    private val clientCredentialsRequest: ClientCredentialsRequest

    private var clientCredentials: ClientCredentials

    init {
        spotifyApi = SpotifyApi.builder().setClientId(clientId).setClientSecret(clientSecret).build()
        clientCredentialsRequest = spotifyApi.clientCredentials().build()

        // build custom client credentials. The real client credentials are only requested when needed.
        clientCredentials = ClientCredentials.Builder().setExpiresIn(0).build()
    }

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun load(spotifyUrl: String): SpotifyProviderResult {
        if (!isSpotifyUrl(spotifyUrl)) {
            throw IllegalArgumentException("The provided url is not a spotify url!")
        }

        val matchResult = SPOTIFY_PATTERN.find(spotifyUrl)!!
        val type = ModelObjectType.keyOf(matchResult.groupValues[1].lowercase())
        val id = matchResult.groupValues[2]

        if (isAccessTokenExpired()) {
            requestClientCredentials()
        }

        return when (type) {
            ModelObjectType.ALBUM -> SpotifyProviderResult(type, retrieveSpotifyAlbum(id))
            ModelObjectType.PLAYLIST -> SpotifyProviderResult(type, retrieveSpotifyPlaylist(id))
            ModelObjectType.TRACK -> SpotifyProviderResult(type, retrieveSpotifyTrack(id))
            else -> {
                throw IllegalStateException("Invalid spotify type: $type. Only request tracks, playlists or albums!")
            }
        }
    }

    private fun retrieveSpotifyAlbum(id: String): Album {
        LOG.info("Retrieving spotify album with id [$id].")
        return spotifyApi.getAlbum(id).build().execute()
    }

    private fun retrieveSpotifyPlaylist(id: String): Playlist {
        LOG.info("Retrieving spotify playlist with id [$id].")
        return spotifyApi.getPlaylist(id).build().execute()
    }

    private fun retrieveSpotifyTrack(id: String): Track {
        LOG.info("Retrieving spotify track with id [$id].")
        return spotifyApi.getTrack(id).build().execute()
    }

    /**
     * @return `true` if the access token is expired.
     */
    private fun isAccessTokenExpired(): Boolean {
        return System.currentTimeMillis() >= System.currentTimeMillis() + clientCredentials.expiresIn
    }

    private fun requestClientCredentials() {
        LOG.info("Requesting client credentials.")
        clientCredentials = clientCredentialsRequest.execute()
        spotifyApi.accessToken = clientCredentials.accessToken
    }
}