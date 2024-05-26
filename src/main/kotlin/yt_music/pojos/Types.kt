package yt_music.pojos

enum class Types(val str: String) {
    SINGLE("Single"),
    ALBUM("Album");

    companion object {
        fun ofString(str: String): Types? {
            for (type in values()) {
                if (type.str == str) {
                    return type
                }
            }

            return null
        }
    }
}