package database.dao.pojo

data class Playlist(var id: Long?, var name: String, var author: String, var tracks: MutableCollection<Track>) {
    var length = tracks.size

    override fun toString(): String {
        return "Playlist{tracks=$tracks, id=$id, name='$name', author='$author'}"
    }
}
