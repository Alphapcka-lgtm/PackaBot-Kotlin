package tune_in_radio

import net.dv8tion.jda.internal.utils.JDALogger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.stream.IntStream
import javax.xml.parsers.DocumentBuilderFactory

class TuneIn {

    /** the logger */
    private val LOG = JDALogger.getLog(TuneIn::class.java)

    private val SEARCH_URL = "https://opml.radiotime.com/Search.ashx?query="

    private val CATEGORY_URL = "https://api.tunein.com/categories/"

    private val PROFILES_URL = "https://api.tunein.com/profiles/"

    /**
     * Searches for the set [searchQuery].
     *
     * @return [TuneInSearchResult]
     */
    fun search(searchQuery: String): TuneInSearchResult {
        val url = URL(SEARCH_URL + encodeValue(searchQuery))
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection
        try {


            con.requestMethod = "GET"
            con.doOutput = true

            val status = con.responseCode

            if (status > 299) {
                val content = readResponse(InputStreamReader(con.inputStream))
                val errorResponse = parseErrorResponse(content.byteInputStream())
                return TuneInSearchResult(errorResponse)
            }

            val content = readResponse(InputStreamReader(con.inputStream))

            val outlines = parseSuccessResponseToElementList(content.toString().byteInputStream())

            return TuneInSearchResult(outlines)
        } finally {
            con.disconnect()
        }
    }

    /**
     * Parses the opml response with code 200 to a [MutableList] which contains the TuneIn audio outlines (all outline elements where the attribute "type" equals `audio`)
     *
     * @param input the input stream to read from.
     * @return a [MutableList] which contains all [TuneInAudioOutline]
     */
    private fun parseSuccessResponseToElementList(input: InputStream): MutableList<TuneInAudioOutline> {
        val factory = DocumentBuilderFactory.newInstance()
        val documentBuilder = factory.newDocumentBuilder()
        val doc = documentBuilder.parse(input)
        doc.documentElement.normalize()

        val outlines = doc.getElementsByTagName("outline")

        /* filters all nodes so that only element nodes where the attribute value equals "audio" are contained,
         * casts them to Elements and accumulates them in a list.
         */
        val audioOutlines = IntStream.range(0, outlines.length).mapToObj(outlines::item).filter { node ->
            if (node.nodeType == Node.ELEMENT_NODE && ((node as Element).getAttribute("type") == "audio")) {
                return@filter true
            }

            return@filter false
        }.map { node ->
            val element = node as Element
            val text = element.getAttribute("text")
            val url = URL(element.getAttribute("URL"))
            val bitrate = element.getAttribute("bitrate").toInt()
            val reliability = element.getAttribute("reliability").toInt()
            val guideId = element.getAttribute("guide_id")
            val subtext = element.getAttribute("subtext")
            val genreId = element.getAttribute("genre_id")
            val formats = element.getAttribute("formats")
            val item = element.getAttribute("item")
            val image = URL(element.getAttribute("image"))
            val nowPlayingId = element.getAttribute("now_playing_id")
            val presetId = element.getAttribute("preset_id")

            return@map TuneInAudioOutline(
                text,
                url,
                bitrate,
                reliability,
                guideId,
                subtext,
                genreId,
                formats,
                item,
                image,
                nowPlayingId,
                presetId
            )
        }.toList()

        return audioOutlines
    }

    private fun parseErrorResponse(input: InputStream): TuneInErrorResponse {
        val factory = DocumentBuilderFactory.newInstance()
        val documentBuilder = factory.newDocumentBuilder()
        val doc = documentBuilder.parse(input)
        doc.documentElement.normalize()

        val status = doc.getElementsByTagName("status").item(0).textContent.toInt()
        val fault = doc.getElementsByTagName("fault").item(0).textContent
        val faultCode = doc.getElementsByTagName("fault_code").item(0).textContent

        return TuneInErrorResponse(status, fault, faultCode)
    }

    /**
     * Reads the api response text from the response.
     *
     * @param input the input stream reader to read from.
     * @return the response text.
     */
    private fun readResponse(input: InputStreamReader): String {
        BufferedReader(input).use { reader ->
            var inputLine: String?
            val content = StringBuffer()
            while (reader.readLine().also { inputLine = it } != null) {
                content.append(inputLine)
            }

            return content.toString()
        }
    }

    /**
     * Encodes the given value for urls with the UTF-8 Charset.
     */
    private fun encodeValue(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    /**
     * Retrieve the genre with the given tune in genre id.
     *
     * @return the tune in category or [TuneInCategory.CATEGORY_NOT_AVAILABLE] if no category could be retrieved or an exception occurred.
     */
    fun genre(genreId: String?): TuneInCategory {
        try {

            if (genreId == null) {
                return TuneInCategory.CATEGORY_NOT_AVAILABLE
            }

            val client = OkHttpClient()

            val request = Request.Builder()
                .url(CATEGORY_URL + genreId)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.code > 299) {
                return TuneInCategory.CATEGORY_NOT_AVAILABLE
            }

            val content = response.body?.string() ?: return TuneInCategory.CATEGORY_NOT_AVAILABLE
            val json = JSONObject(content)
            val title = json.getJSONObject("Header").getString("Title")
            return TuneInCategory(true, genreId, title)

        } catch (e: Exception) {
            LOG.error("Error when retrieving category from api.tunein.com!", e)
            return TuneInCategory.CATEGORY_NOT_AVAILABLE
        }
    }

    /**
     * Retrieve the profile with the given tune in guid id.
     *
     * @return the tune in profile or [TuneInProfile.PROFILE_NOT_AVAILABLE] if no profile could be retrieved or an exception occurred.
     */
    fun profile(guidId: String?): TuneInProfile {
        try {

            if (guidId == null) {
                return TuneInProfile.PROFILE_NOT_AVAILABLE
            }

            val client = OkHttpClient()

            val request = Request.Builder()
                .url(PROFILES_URL + guidId)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.code > 299) {
                return TuneInProfile.PROFILE_NOT_AVAILABLE
            }

            val content = response.body?.string() ?: return TuneInProfile.PROFILE_NOT_AVAILABLE
            val json = JSONObject(content)
            val item = json.getJSONObject("Item")
            val title = item.getString("Title")
            val description = item.getString("Description")
            val type = item.getString("Type")

            val properties = item.getJSONObject("Properties")
            val location = properties.getJSONObject("Location")
            val country = location.getString("DisplayName")

            val actions = item.getJSONObject("Actions")
            val share = actions.getJSONObject("Share")
            val shareUrl = share.getString("ShareUrl")

            return TuneInProfile(true, title, description, type, country, shareUrl)
        } catch (e: Exception) {
            LOG.error("Error when retrieving profile from api.tunein.com!", e)
            return TuneInProfile.PROFILE_NOT_AVAILABLE
        }
    }
}