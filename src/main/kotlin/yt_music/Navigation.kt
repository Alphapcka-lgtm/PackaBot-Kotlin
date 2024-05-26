package yt_music

import com.google.gson.JsonArray
import com.google.gson.JsonElement

open class Navigation {

    val CONTENT = listOf("contents", "0")
    val RUN_TEXT = listOf("runs", "0", "text")
    val TAB_CONTENT = listOf("tabs", "0", "tabRenderer", "content")
    val TAB_1_CONTENT = listOf("tabs", "1", "tabRenderer", "content")
    val SINGLE_COLUMN = listOf("contents", "singleColumnBrowseResultsRenderer")
    val SINGLE_COLUMN_TAB = SINGLE_COLUMN + TAB_CONTENT
    val SECTION_LIST = listOf("sectionListRenderer", "contents")
    val SECTION_LIST_ITEM = listOf("sectionListRenderer") + CONTENT
    val ITEM_SECTION = listOf("itemSectionRenderer") + CONTENT
    val MUSIC_SHELF = listOf("musicShelfRenderer")
    val GRID = listOf("gridRenderer")
    val GRID_ITEMS = GRID + listOf("items")
    val MENU = listOf("menu", "menuRenderer")
    val MENU_ITEMS = MENU + listOf("items")
    val MENU_LIKE_STATUS = MENU + listOf("topLevelButtons", "0", "likeButtonRenderer", "likeStatus")
    val MENU_SERVICE = listOf("menuServiceItemRenderer", "serviceEndpoint")
    val TOGGLE_MENU = "toggleMenuServiceItemRenderer"
    val PLAY_BUTTON = listOf(
        "overlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer"
    )
    val NAVIGATION_BROWSE = listOf("navigationEndpoint", "browseEndpoint")
    val NAVIGATION_BROWSE_ID = NAVIGATION_BROWSE + listOf("browseId")
    val PAGE_TYPE = listOf(
        "browseEndpointContextSupportedConfigs", "browseEndpointContextMusicConfig", "pageType"
    )
    val WATCH_VIDEO_ID = listOf("watchEndpoint", "videoId")
    val NAVIGATION_VIDEO_ID = listOf("navigationEndpoint") + WATCH_VIDEO_ID
    val NAVIGATION_PLAYLIST_ID = listOf("navigationEndpoint", "watchEndpoint", "playlistId")
    val NAVIGATION_WATCH_PLAYLIST_ID = listOf("navigationEndpoint", "watchPlaylistEndpoint", "playlistId")
    val NAVIGATION_VIDEO_TYPE = listOf(
        "watchEndpoint", "watchEndpointMusicSupportedConfigs", "watchEndpointMusicConfig",
        "musicVideoType"
    )
    val TITLE = listOf("title", "runs", "0")
    val TITLE_TEXT = listOf("title") + RUN_TEXT
    val TEXT_RUNS = listOf("text", "runs")
    val TEXT_RUN = TEXT_RUNS + listOf("0")
    val TEXT_RUN_TEXT = TEXT_RUN + listOf("text")
    val SUBTITLE = listOf("subtitle") + RUN_TEXT
    val SUBTITLE_RUNS = listOf("subtitle", "runs")
    val SUBTITLE2 = SUBTITLE_RUNS + listOf("2", "text")
    val SUBTITLE3 = SUBTITLE_RUNS + listOf("4", "text")
    val THUMBNAIL = listOf("thumbnail", "thumbnails")
    val THUMBNAILS = listOf("thumbnail", "musicThumbnailRenderer") + THUMBNAIL
    val THUMBNAIL_RENDERER = listOf("thumbnailRenderer", "musicThumbnailRenderer") + THUMBNAIL
    val THUMBNAIL_CROPPED = listOf("thumbnail", "croppedSquareThumbnailRenderer") + THUMBNAIL
    val FEEDBACK_TOKEN = listOf("feedbackEndpoint", "feedbackToken")
    val BADGE_PATH = listOf("0", "musicInlineBadgeRenderer", "accessibilityData", "accessibilityData", "label")
    val BADGE_LABEL = listOf("badges") + BADGE_PATH
    val SUBTITLE_BADGE_LABEL = listOf("subtitleBadges") + BADGE_PATH
    val CATEGORY_TITLE = listOf("musicNavigationButtonRenderer", "buttonText") + RUN_TEXT
    val CATEGORY_PARAMS = listOf("musicNavigationButtonRenderer", "clickCommand", "browseEndpoint", "params")
    val MRLIR = "musicResponsiveListItemRenderer"
    val MTRIR = "musicTwoRowItemRenderer"
    val TASTE_PROFILE_ITEMS = listOf("contents", "tastebuilderRenderer", "contents")
    val TASTE_PROFILE_ARTIST = listOf("title", "runs")
    val SECTION_LIST_CONTINUATION = listOf("continuationContents", "sectionListContinuation")
    val MENU_PLAYLIST_ID = MENU_ITEMS + listOf("0", "menuNavigationItemRenderer") + NAVIGATION_WATCH_PLAYLIST_ID
    val HEADER_DETAIL = listOf("header", "musicDetailHeaderRenderer")
    val DESCRIPTION_SHELF = listOf("musicDescriptionShelfRenderer")
    val DESCRIPTION = listOf("description") + RUN_TEXT
    val CAROUSEL = listOf("musicCarouselShelfRenderer")
    val IMMERSIVE_CAROUSEL = listOf("musicImmersiveCarouselShelfRenderer")
    val CAROUSEL_CONTENTS = CAROUSEL + listOf("contents")
    val CAROUSEL_TITLE = listOf("header", "musicCarouselShelfBasicHeaderRenderer") + TITLE
    val CARD_SHELF_TITLE = listOf("header", "musicCardShelfHeaderBasicRenderer") + TITLE_TEXT
    val FRAMEWORK_MUTATIONS = listOf("frameworkUpdates", "entityBatchUpdate", "mutations")

    fun nav(root: JsonElement, items: List<String>, nullIfAbsent: Boolean = false): JsonElement? {
        //Access a nested object in root by item sequence.
        var _root = root
        try {
            for (item in items) {
                _root =
                    if (_root.isJsonObject) _root.asJsonObject.get(item) else {
                        val i = item.toInt()
                        if (i == -1) {
                            _root.asJsonArray.last()
                        } else {
                            _root.asJsonArray.get(i)
                        }
                    }
            }
            return _root
        } catch (e: Exception) {
            if (nullIfAbsent) {
                return null
            } else {
                throw e
            }
        }
    }

    fun findObjectByKey(
        objectList: JsonArray,
        key: String,
        nested: String? = null,
        isKey: Boolean = false,
    ): JsonElement? {
        for (_item in objectList) {
            var item = _item
            if (nested != null) {
                item = if (item.isJsonObject) item.asJsonObject.get(nested) else item.asJsonArray.get(nested.toInt())
            }
            if (item.asJsonObject.has(key)) {
                return if (isKey) item.asJsonObject.get(key) else item
            }
        }
        return null
    }

    fun findObjectsByKey(objectList: JsonArray, key: String, nested: String? = null): ArrayList<JsonElement> {
        val objects = ArrayList<JsonElement>()
        for (_item in objectList) {
            var item = _item
            if (nested != null) {
                item = if (item.isJsonObject) item.asJsonObject.get(nested) else item.asJsonArray.get(nested.toInt())
            }
            // check if key is in item, item should always be a json object at this point
            if (item.asJsonObject.has(key)) {
                objects.add(item)
            }
        }

        return objects
    }
}