import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import music.spotify.SpotifyProvider
import se.michaelthelin.spotify.enums.ModelObjectType
import se.michaelthelin.spotify.model_objects.specification.Track
import yt_music.YTMusic
import yt_music.enums.SearchFilters
import yt_music.pojos.ResultTypes
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpResponse
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
                    if (searchResult.resultType == ResultTypes.ALBUM) {
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
            // Files.writeString(Path("C:\\Users\\Michael\\Desktop\\Neuer Ordner\\spotpl.txt"), gson.toJson(je))
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

        println(headers.toString())

        val httpClient = HttpClient.newBuilder().build()
        val httpRequestBuilder = java.net.http.HttpRequest.newBuilder(URI(ytmBaseApi + endpoint + ytmParams))
        httpRequestBuilder.POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
        for (header in headers.entries) {
            httpRequestBuilder.header(header.key, header.value)
        }
        val httpRequest = httpRequestBuilder.build()
        println(httpRequest.headers())
        println(httpRequest.headers().map())
        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(Charsets.UTF_32))

        println("Status code: ${response.statusCode()}")
//        val input = BufferedReader(InputStreamReader(response.body()))
//        var inputLine: String? = null
//        val content = StringBuffer()
//        while (input.readLine().also { inputLine = it } != null) {
//            content.append(inputLine)
//        }
//        println("body:")
//        println(response.body())

        println(response.version())

//        println(headers.toString())

//        val response = get(
//            ytmBaseApi + endpoint + ytmParams,
//            json = body,
//            headers = headers,
//            cookies = mapOf("CONSENT" to "YES+1")
//        )

//        val gson = GsonBuilder().setPrettyPrinting().create()
//        val je = JsonParser.parseString(response.jsonObject.toString())
//        Files.writeString(Path("C:\\Users\\Michael\\Desktop\\Neuer Ordner\\spotpl.txt"), gson.toJson(je))

    }
}