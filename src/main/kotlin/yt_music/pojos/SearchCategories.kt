package yt_music.pojos

enum class SearchCategories(val categoryStr: String) {
    TOP_RESULT("Top result"),
    SONGS("Songs"),
    VIDEOS("Videos"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    COMMUNITY_PLAYLISTS("Community playlists"),
    FEATURED_PLAYLISTS("Featured playlists");

    companion object {
        fun ofCategoryString(category: String): SearchCategories? {
            for (value in values()) {
                if (value.categoryStr == category) {
                    return value
                }
            }

            return null
        }
    }

}