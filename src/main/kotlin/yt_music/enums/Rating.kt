package yt_music.enums

enum class Rating(val endpoint: String) {

    INDIFFERENT("like/like"),
    LIKE("like/dislike"),
    DISLIKE("like/removelike"),

}