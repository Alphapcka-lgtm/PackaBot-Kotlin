package yt_music.pojos

enum class ResultTypes(val typeStr: String) {
    SONG("song"),
    VIDEO("video"),
    ALBUM("album"),
    ARTIST("artist"),
    PLAYLIST("playlist"),
    COMMUNITY_PLAYLIST("community_playlist"),
    FEATURED_PLAYLIST("featured_playlist");

    companion object {
        fun ofString(typeStr: String): ResultTypes? {
            for (c in values()) {
                if (c.typeStr == typeStr) {
                    return c
                }
            }

            return null
        }
    }

}