package yt_music.enums

enum class SearchFilters(filter: String) {
    SONGS("songs"),
    VIDEOS("videos"),
    ALBUMS("albums"),
    ARTISTS("artists"),
    PLAYLISTS("playlists"),
    COMMUNITY_PLAYLISTS("community_playlists"),
    FEATURED_PLAYLISTS("featured_playlists")
}