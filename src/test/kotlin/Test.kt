import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import khttp.get
import music.spotify.SpotifyProvider
import se.michaelthelin.spotify.enums.ModelObjectType
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test

class Test {

    private val SPOTIFY_PATTERN = "^https://open.spotify.com/(.*)/([\\w]*).*$" //*?\?si=.*

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
            spotifyProvider.load("https://open.spotify.com/playlist/6T8SNVokqsvarerVQ7rIy2?si=a86de2a6aca4423b")

        when (result.type) {
            ModelObjectType.ALBUM -> {
                val album = result.objectAsAlbum()
                val track = album.tracks.items[0]
            }

            ModelObjectType.PLAYLIST -> {
                val playlist = result.objectAsPlaylist()
                val track = playlist.tracks.items[0]
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
        //{'client': {'clientName': 'WEB_REMIX', 'clientVersion': '1.20230408.01.00', 'hl': 'en'}, 'user': {}}
        //{'query': 'Feel Again - Armin van Buuren', 'params': 'EgWKAQIYAWoMEA4QChADEAQQCRAF', 'context': {'client': {'clientName': 'WEB_REMIX', 'clientVersion': '1.20230408.01.00', 'hl': 'en'}, 'user': {}}}
        val ytmBaseApi = "https://music.youtube.com/youtubei/v1/" //later const
        val endpoint = "search"
        val ytmParams = "?alt=json&key=AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30" //later const
        val query = "Feel Again - Armin van Buuren"
        val body =
            "{'query': '$query', 'params': 'EgWKAQIYAWoMEA4QChADEAQQCRAF', 'context': {'client': {'clientName': 'WEB_REMIX', 'clientVersion': '1.20230408.01.00', 'hl': 'en'}, 'user': {}}}"
        val headers = mapOf(
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0",
            "accept" to "*/*",
            "accept-encoding" to "gzip, deflate",
            "content-type" to "application/json",
            "content-encoding" to "gzip",
            "origin" to "https://music.youtube.com",
            "X-Goog-Visitor-Id" to "Cgt5dWRjN25Ub1QxSSiw1MahBg%3D%3D"
        )
        val response = get(
            ytmBaseApi + endpoint + ytmParams,
            json = body,
            headers = headers,
            cookies = mapOf("CONSENT" to "YES+1")
        )

        val gson = GsonBuilder().setPrettyPrinting().create()
        val je = JsonParser.parseString(response.jsonObject.toString())
        Files.writeString(Path("C:\\Users\\Michael\\Desktop\\Neuer Ordner\\spotpl.txt"), gson.toJson(je))

    }
}