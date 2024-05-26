package yt_music

import com.google.gson.JsonParser
import kong.unirest.Proxy
import kong.unirest.UnirestInstance
import yt_music.auth.YTMusicOAuth
import java.io.BufferedReader
import java.io.FileReader
import java.nio.file.Files
import kotlin.io.path.Path

fun prepareHeaders(unirest: UnirestInstance, proxy: Proxy?, auth: String? = null): HashMap<String, String> {
    // maybe add OAuth later

    var headers: HashMap<String, String> = HashMap()
    if (auth != null) {
        val inputJson = if (Files.exists(Path(auth))) {
            val reader = BufferedReader(FileReader(auth))
            JsonParser.parseReader(reader)
        } else {
            JsonParser.parseString(auth)
        }.asJsonObject

        if (auth.contains("oauth.json")) {
            val oauth = YTMusicOAuth(unirest, proxy)
            headers = oauth.loadHeaders(inputJson, auth)
        } else {
            inputJson.asMap().entries.forEach { entry -> headers[entry.key] = entry.value.toString() }
        }
    } else {
        headers = initializeHeaders()
    }

    return headers
}