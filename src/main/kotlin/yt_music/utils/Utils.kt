package yt_music.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import yt_music.Navigation
import java.util.concurrent.TimeUnit

private val NAVIGATION = Navigation()

/**
 * Extension function for [JsonObject]
 *
 * Adds key:value elements to the json object
 * @param jsonObject the json object with the update value for the json.
 */
fun JsonObject.update(jsonObject: JsonObject) {
    for (entry in jsonObject.entrySet()) {
        this.add(entry.key, entry.value)
    }
}

fun parseMenuPlaylists(data: JsonObject, searchResult: JsonObject) {
    val watchMenu =
        NAVIGATION.findObjectsByKey(
            NAVIGATION.nav(data, NAVIGATION.MENU_ITEMS)!!.asJsonArray,
            "menuNavigationItemRenderer"
        )
    val itemsStream = watchMenu.stream().filter { _x -> _x.asJsonObject.has("menuNavigationItemRenderer") }
        .map { _x -> _x.asJsonObject.get("menuNavigationItemRenderer") }
    // filters for all items that have the key 'menuNavigationItemRenderer'
    for (item in itemsStream) {
        var watchKey: String
        val icon = NAVIGATION.nav(item, listOf("icon", "iconType"))!!
        watchKey = if (icon.asString == "MUSIC_SHUFFLE") {
            "shuffleId"
        } else if (icon.asString == "MIX") {
            "radioId"
        } else continue

        var watchId = NAVIGATION.nav(item, listOf("navigationEndpoint", "watchPlaylistEndpoint", "playlistId"), true)
        if (watchId == null) {
            watchId = NAVIGATION.nav(item, listOf("navigationEndpoint", "watchEndpoint", "playlistId"), true)
        }
        if (watchId != null) {
            searchResult.add(watchKey, watchId)
        }
    }
}


fun getItemText(item: JsonObject, index: Int, runIndex: Int = 0, nullIfAbsent: Boolean = false): String? {
    val column = getFlexColumnItem(item, index) ?: return null
    return column.getAsJsonObject("text").getAsJsonArray("runs")
        .get(runIndex).asJsonObject.getAsJsonPrimitive("text").asString
}

fun getFlexColumnItem(item: JsonObject, index: Int): JsonObject? {
    if (item.getAsJsonArray("flexColumns").size() <= index || !item.getAsJsonArray("flexColumns")
            .get(index).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            .has("text") || !item.getAsJsonArray("flexColumns")
            .get(index).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            .getAsJsonObject("text").has("runs")
    ) {
        return null
    }

    return item.getAsJsonArray("flexColumns")
        .get(index).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
}

fun getFixedColumnItem(item: JsonObject, index: Int): JsonObject? {
    if (!item.getAsJsonArray("fixedColumns")[index].asJsonObject.getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
            .has("text") || !item.getAsJsonArray("fixedColumns")[index].asJsonObject.getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
            .getAsJsonObject("text").has("runs")
    ) {
        return null
    }

    return item.getAsJsonArray("fixedColumns")[index].asJsonObject.getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
}

fun getBrowseId(item: JsonObject, index: Int): JsonElement? {
    if (!item.getAsJsonObject("text").getAsJsonArray("runs")[index].asJsonObject.has("navigationEndpoint")) {
        return null
    }

    return NAVIGATION.nav(item.getAsJsonObject("text").getAsJsonArray("runs")[index], NAVIGATION.NAVIGATION_BROWSE_ID)!!
}

fun getDotSeparatorIndex(runs: JsonArray): Int {
    val index = runs.size()
    val _index = runs.indexOf(JsonObject().also { json -> json.addProperty("text", " • ") })
    if (_index == -1) {
        return index
    }

    return _index
}

/**
 * @return the duration in seconds
 */
fun parseDuration(duration: String): Int {
    val mappedIncrements = zip(listOf("1", "60", "3600"), duration.split(':').reversed())
    val seconds =
        mappedIncrements.stream().mapToInt { lst ->
            val multiplier = lst[0]
            val time = lst.getOrElse(1) { "0" }
            return@mapToInt multiplier.toInt() * time.toInt()
        }.sum()
    return seconds
}

fun <T> zip(vararg lists: Collection<T>): List<MutableList<T>> {
    val zipped: MutableList<MutableList<T>> = ArrayList()
    for (list in lists) {
        var i = 0
        val listSize = list.size
        while (i < listSize) {
            var list2: MutableList<T>
            if (i >= zipped.size) zipped.add(ArrayList<T>().also { list2 = it }) else list2 = zipped[i]
            list2.add(list.elementAt(i))
            i++
        }
    }
    return zipped
}

fun getDatestamp(): Long = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

fun <T> collectionToJsonArray(collection: Collection<T>): JsonArray {
    return JsonParser.parseString(collection.joinToString(prefix = "[", postfix = "]")).asJsonArray
}

fun convertSearchDuration(str: String): Long {
    val regex = "(\\d*)(.*)".toRegex()
    val res = regex.find(str) ?: throw NumberFormatException("No match result for string: $str")
    val identifier = res.groupValues[2]
    if (identifier == "") {
        return str.toLong()
    }

    if (identifier == "k") {
        return res.groupValues[1].toLong() * 1000
    } else {
        throw Exception("Unknown identifier: $identifier")
    }

}

fun htmlToText(htmlText: String): String {
    var ret = htmlText
    val tags = "<[^>]+>".toRegex().findAll(htmlText)
    for (tag in tags) {
        ret = ret.replace(tag.value, "")
    }

    return ret
}