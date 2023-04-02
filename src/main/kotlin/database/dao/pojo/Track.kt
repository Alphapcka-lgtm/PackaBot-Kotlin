package database.dao.pojo

data class Track(var id: Long, var name: String, var author: String, var audioUrl: String) {
    override fun toString(): String {
        return "Track{id=$id, name='$name', author='$author', audioUrl='$audioUrl'}"
    }
}