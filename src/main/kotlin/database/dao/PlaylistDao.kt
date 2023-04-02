package database.dao

import database.SqlStatementValueConverter
import database.dao.pojo.Playlist
import database.dao.pojo.Track
import java.sql.*
import java.util.*
import javax.annotation.Nonnull
import javax.annotation.Nullable


class PlaylistDao : Dao<Playlist> {

    companion object {
        /** name of the table  */
        val TABLE_NAME = "Playlist"

        /** name of the column with the id of the playlist  */
        val COLUMN_PLAYLIST_ID = "$TABLE_NAME.id"

        /** name of the column with the name of the playlist  */
        val COLUMN_PLAYLIST_NAME = "$TABLE_NAME.pl_Name"

        /** name of the column with the author of the playlist  */
        val COLUMN_PLAYLIST_AUTHOR = "$TABLE_NAME.pl_Author"
    }

    override fun get(con: Connection, id: Long): Optional<Playlist> {
        var statement: Statement? = null
        var result: ResultSet? = null
        return try {
            statement = con.createStatement()
            result = statement.executeQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_PLAYLIST_ID = $id"
            )
            if (!result.next()) {
                return Optional.empty()
            }
            val playlistId: Long = result.getLong(COLUMN_PLAYLIST_ID)
            val playlistName: String = result.getString(COLUMN_PLAYLIST_NAME)
            val playlistAuthor: String = result.getString(COLUMN_PLAYLIST_AUTHOR)
            result.close()
            result = statement.executeQuery(
                "SELECT " + TrackDao.COLUMN_TRACK_ID + ", " + TrackDao.COLUMN_TRACK_NAME + ", " + TrackDao.COLUMN_TRACK_AUTHOR + ", " + TrackDao.COLUMN_AUDIO_URL + " FROM " + TrackDao.TABLE_NAME + " JOIN Playlist_Track pt ON " + TrackDao.COLUMN_TRACK_ID + " = pt.id_Track WHERE pt.id_Playlist = " + id
            )
            val tracks: MutableList<Track> = ArrayList<Track>()
            while (result.next()) {
                val trackId: Long = result.getLong(TrackDao.COLUMN_TRACK_ID)
                val trackName: String = result.getString(TrackDao.COLUMN_TRACK_NAME)
                val trackAuthor: String = result.getString(TrackDao.COLUMN_TRACK_AUTHOR)
                val audioUrl: String = result.getString(TrackDao.COLUMN_AUDIO_URL)
                tracks.add(Track(trackId, trackName, trackAuthor, audioUrl))
            }
            val playlist = Playlist(playlistId, playlistName, playlistAuthor, tracks)
            Optional.of(playlist)
        } finally {
            statement?.close()
            result?.close()
        }
    }

    override fun getAll(con: Connection): Collection<Playlist> {
        con.createStatement().use { statement ->
            statement.executeQuery("SELECT $COLUMN_PLAYLIST_ID FROM $TABLE_NAME")
                .use { result ->
                    val playlists: MutableCollection<Playlist> = ArrayList()
                    while (result.next()) {
                        val playlistId = result.getLong(COLUMN_PLAYLIST_ID)
                        val playlist = get(con, playlistId)
                        playlist.ifPresent(playlists::add)
                    }
                    return playlists
                }
        }
    }

    override fun update(con: Connection, id: Long, params: Array<String>): Optional<Playlist> {
        TODO("Not yet implemented")
    }

    override fun delete(con: Connection, t: Playlist): Optional<Playlist> {
        TODO("Not yet implemented")
    }

    override fun save(con: Connection, playlist: Playlist): Playlist {
        var statement: PreparedStatement? = null
        var result: ResultSet? = null
        try {
            statement = con.prepareStatement(
                "REPLACE INTO $TABLE_NAME ($COLUMN_PLAYLIST_ID, $COLUMN_PLAYLIST_NAME, $COLUMN_PLAYLIST_AUTHOR ) VALUES (${playlist.id}, ${
                    SqlStatementValueConverter.convertStringValue(
                        playlist.name
                    )
                }, ${
                    SqlStatementValueConverter.convertStringValue(
                        playlist.author
                    )
                })", Statement.RETURN_GENERATED_KEYS
            )
            statement.executeUpdate()
            result = statement.generatedKeys
            // further checks should not be necessary, since it will always return one key
            result.next()
            val id = result.getLong(1)
            // after saving the playlist, set the id
            playlist.id = id
            val trackDao = TrackDao()

            // The list can not be modified, while in a loop.
            val tracks: MutableList<Track> = ArrayList(playlist.length)
            for (_track in playlist.tracks) {
                var track = _track
                // track.id can be null - IDK why the ide thinks otherwise
                if (track.id == null) {
                    track = trackDao.save(con, track)
                }
                tracks.add(track)
            }
            playlist.tracks = tracks

            // connect playlist db with track via Playlist_Track
            // playlist.id is never null at this position
            linkPlaylistTracks(con, playlist.id!!, tracks)
            return playlist
        } finally {
            statement?.close()
            result?.close()
        }
    }

    /**
     * Links the tracks to the playlist
     *
     * @param con        the database connection. (never `null`)
     * @param playlistId the id of the playlist to link.
     * @param tracks     the tracks to link. (never `null`)
     */
    @Throws(SQLException::class)
    private fun linkPlaylistTracks(con: Connection, playlistId: Long, tracks: Collection<Track>) {
        con.createStatement().use { statement ->
            for (track in tracks) {
                statement.executeUpdate(
                    "INSERT IGNORE INTO Playlist_Track (Playlist_Track.id_Playlist, Playlist_Track.id_Track) VALUES ($playlistId, ${track.id})"
                )
            }
        }
    }

    /**
     * Gets all playlists that have the given name.
     *
     * @param con  the database connection. (never <code>null</code>)
     * @param name the playlist name. (can be <code>null</code>)
     * @return a collection of all playlists with the given name. (never <code>null</code>)
     * @throws SQLException On error, when finding the playlists.
     */
    @Throws(SQLException::class)
    fun findPlaylistsWithName(@Nonnull con: Connection, @Nullable name: String?): Collection<Playlist> {
        con.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT $COLUMN_PLAYLIST_ID FROM $TABLE_NAME WHERE $COLUMN_PLAYLIST_NAME = ${
                    SqlStatementValueConverter.convertStringValue(
                        name
                    )
                }"
            ).use { result ->
                val playlists: MutableCollection<Playlist> = ArrayList()
                while (result.next()) {
                    val playlistId = result.getLong(COLUMN_PLAYLIST_ID)
                    val playlist = get(con, playlistId)
                    playlist.ifPresent(playlists::add)
                }
                return playlists
            }
        }
    }


    /**
     * Gets all playlists that have the given author.
     *
     * @param con    the database connection. (never <code>null</code>)
     * @param author the playlist author. (can be <code>null</code>)
     * @return all playlists that have the given author (never <code>null</code>)
     * @throws SQLException On error, when finding the playlists.
     */
    @Throws(SQLException::class)
    fun findTracksWithAuthor(@Nonnull con: Connection, @Nullable author: String?): Collection<Playlist> {
        con.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT $COLUMN_PLAYLIST_ID FROM $TABLE_NAME WHERE $COLUMN_PLAYLIST_AUTHOR = ${
                    SqlStatementValueConverter.convertStringValue(
                        author
                    )
                }"
            ).use { result ->
                val playlists: MutableCollection<Playlist> = ArrayList()
                while (result.next()) {
                    val playlistId = result.getLong(COLUMN_PLAYLIST_ID)
                    val playlist = get(con, playlistId)
                    playlist.ifPresent(playlists::add)
                }
                return playlists
            }
        }
    }
}