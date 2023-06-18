import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import music.spotify.SpotifyProvider
import org.alphapacka.com.YTMusic
import org.alphapacka.com.enums.SearchFilters
import se.michaelthelin.spotify.enums.ModelObjectType
import se.michaelthelin.spotify.model_objects.specification.Track
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test

class Test {

    private val SPOTIFY_PATTERN = "^https://open.spotify.com/(.*)/([\\w]*).*$" //*?\?si=.*
    private val ytMusic = YTMusic(null, null, null)

    @Test
    fun testSpotifyPattern() {
        val regex = Regex(SPOTIFY_PATTERN)
        val urls = arrayOf(
            "https://open.spotify.com/track/5EbITZyzQ51x4y5vgTCoGw?si=f77e424771974bae",
            "https://open.spotify.com/playlist/6T8SNVokqsvarerVQ7rIy2?si=d9f71c25bf964f02",
            "https://open.spotify.com/album/7bwgYKZoe9F1uitFLxjPwO",
            "https://open.spotify.com/artist/0SfsnGyD8FpIN4U4WCkBZ5?si=O0mi7vI4RmCFecSBgtzkTg"
        )

        for (url in urls) {
            println(regex.matches(url))
            regex.findAll(url).forEach { matchResult -> println(matchResult.groupValues.joinToString()) }
            val matchResult = regex.find(url)!!
            println(matchResult.groupValues[0])
            println(matchResult.groupValues[1])
            println(matchResult.groupValues[2])
        }

        println(ModelObjectType.keyOf("foo"))
    }

    private val clientId = "9c8be88b1ea4444098ea8ee86970be90"
    private val clientSecret = "3d7592c87c0444a5a4b54394b21b5b2f"

    private val spotifyProvider: SpotifyProvider = SpotifyProvider(clientId, clientSecret)

    @Test
    fun testSpotify() {
        val result =
            spotifyProvider.retrieve("https://open.spotify.com/playlist/5Hou76tCX8ef4FEM0RvaM0?si=30a75f87033c4352")
        when (result.type) {
            ModelObjectType.ALBUM -> {
                val album = result.objectAsAlbum()
                val tracks = album.tracks.items
                val searchResults =
                    ytMusic.search(album.name, SearchFilters.ALBUMS).stream().map { it.asAlbum }.toList()
                searchResults.size
                searchResults.forEach { searchResult ->
                    if (searchResult.type == "Album") {
                        println(searchResult)
                    }
                }
            }

            ModelObjectType.PLAYLIST -> {
                val playlist = result.objectAsPlaylist()
                val tracks = playlist.tracks.items
                val t = tracks[0].track as Track
                println()
            }

            ModelObjectType.TRACK -> {
                val track = result.objectAsTrack()
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }

    @Test
    fun testYouTubeSearch() {
        val youtube = YouTube.Builder(NetHttpTransport(), GsonFactory(), object : HttpRequestInitializer {
            override fun initialize(request: HttpRequest?) {
            }
        }).setApplicationName("TestApp").build()

        val search = youtube.search().list(listOf("id", "snippet"))
        search.key = "AIzaSyDfzwJNIB4S69M0zYnia7oIQHCNiFeOQlc"
        search.type = listOf("playlist")
        search.q = "Feel Again - Armin van Buuren"

        val searchListResult = search.execute()
        val searchResultList = searchListResult.items
        for (result in searchResultList) {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val je = JsonParser.parseString(result.toString())
            Files.writeString(Path("C:\\Users\\Michael\\Desktop\\Neuer Ordner\\spotpl.txt"), gson.toJson(je))
        }
    }

    @Test
    fun testYouTubeMusicSearch() {
        val playerManager = DefaultAudioPlayerManager()
        AudioSourceManagers.registerRemoteSources(playerManager)
        val future = playerManager.loadItem(
            "https://music.youtube.com/playlist?list=OLAK5uy_mO7Xk_Vo9r28I6aI5O_f5B-WXfFIO5vzw",
            object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    println("track loaded $track")
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    println("playlist loaded: $playlist")
                }

                override fun noMatches() {
                    println("no matches")
                }

                override fun loadFailed(exception: FriendlyException) {
                    throw exception
                }

            })

        future.get()
    }

}