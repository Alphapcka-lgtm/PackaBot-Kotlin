package database.dao

import database.SqlStatementValueConverter.convertStringValue
import database.dao.pojo.Track
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import javax.annotation.Nullable


class TrackDao : Dao<Track> {

    companion object {
        /** name of the table */
        const val TABLE_NAME = "Track"

        /** name of the column with the id of the track */
        const val COLUMN_TRACK_ID = "$TABLE_NAME.id"

        /** name of the column with the name of the track */
        const val COLUMN_TRACK_NAME = "$TABLE_NAME.tr_Name"

        /** name of the column with the author of the track */
        const val COLUMN_TRACK_AUTHOR = "$TABLE_NAME.tr_Author"

        /** name of the column with the url to audio source of the track */
        const val COLUMN_AUDIO_URL = "$TABLE_NAME.audio_Url"
    }

    override fun get(con: Connection, id: Long): Optional<Track> {

        con.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT * FROM ${Companion.TABLE_NAME} WHERE$COLUMN_TRACK_ID= $id"
            ).use { result ->
                if (!result.next()) {
                    return Optional.empty()
                }
                val idTrack: Long = result.getLong(COLUMN_TRACK_ID)
                val name: String = result.getString(COLUMN_TRACK_NAME)
                val author: String = result.getString(COLUMN_TRACK_AUTHOR)
                val audioUrl: String = result.getString(COLUMN_AUDIO_URL)
                val track = Track(idTrack, name, author, audioUrl)
                return Optional.of(track)
            }
        }
    }

    override fun getAll(con: Connection): Collection<Track> {
        con.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM $TABLE_NAME").use { result ->
                val tracks: MutableCollection<Track> = ArrayList()
                while (result.next()) {
                    val id = result.getLong(COLUMN_TRACK_ID)
                    val name = result.getString(COLUMN_TRACK_NAME)
                    val author = result.getString(COLUMN_TRACK_AUTHOR)
                    val audioUrl = result.getString(COLUMN_AUDIO_URL)
                    tracks.add(Track(id, name, author, audioUrl))
                }
                return tracks
            }
        }
    }

    override fun save(con: Connection, track: Track): Track {
        var statement: PreparedStatement? = null
        var result: ResultSet? = null
        return try {
            /*
                 * To make sure, that there are no tracks with the same url, check if one with the url already exists.
                 * If it exists and the id of the track to save and the one from the bd does not equal, return the one from the db.
                 */
            val ex: Optional<Track> = findTrackWithAudioUrl(con, track.audioUrl)
            if (ex.isPresent) {
                if (ex.get().id != track.id) {
                    return ex.get()
                }
            }
            statement = con.prepareStatement(
                "REPLACE INTO $TABLE_NAME($COLUMN_TRACK_ID, $COLUMN_TRACK_NAME, $COLUMN_TRACK_AUTHOR, $COLUMN_AUDIO_URL) VALUES (${track.id}, ${
                    convertStringValue(
                        track.name
                    )
                }, ${
                    convertStringValue(
                        track.author
                    )
                },${
                    convertStringValue(
                        track.audioUrl
                    )
                })", PreparedStatement.RETURN_GENERATED_KEYS
            )
            statement.executeUpdate()
            result = statement.generatedKeys

            // further checks should not be necessary, since it will always return one key
            result.next()
            val id: Long = result.getLong(1)
            track.id = id
            track
        } finally {
            if (statement != null) {
                statement.close()
            }
            if (result != null) {
                result.close()
            }
        }
    }

    override fun update(con: Connection, id: Long, params: Array<String>): Optional<Track> {
        TODO("Not yet implemented")
    }

    override fun delete(con: Connection, t: Track): Optional<Track> {
        con.createStatement().use { statement ->
            val result = statement.executeUpdate(
                "DELETE FROM $TABLE_NAME WHERE $COLUMN_TRACK_ID = ${t.id}"
            )
            return Optional.of(t)
        }
    }

    /**
     * Finds all tracks that have the given name.
     *
     * @param con  the database connection. (never <code>null</code>)
     * @param name the name of the track (can be <code>null</code>)
     * @return All tracks that have the given name. (never <code>null</code>)
     * @throws SQLException On error, when getting the tracks.
     */
    @Throws(SQLException::class)
    fun findTracksWithName(con: Connection, name: String?): Collection<Track> {
        con.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_TRACK_NAME = ${
                    convertStringValue(
                        name
                    )
                }"
            ).use { result ->
                val tracks: MutableCollection<Track> = ArrayList()
                while (result.next()) {
                    val id = result.getLong(COLUMN_TRACK_ID)
                    val trackName = result.getString(COLUMN_TRACK_NAME)
                    val author = result.getString(COLUMN_TRACK_AUTHOR)
                    val audioUrl = result.getString(COLUMN_AUDIO_URL)
                    tracks.add(Track(id, trackName, author, audioUrl))
                }
                return tracks
            }
        }
    }

    /**
     * Finds all tracks that have the given author.
     *
     * @param con    the database connection. (never <code>null</code>)
     * @param author the author of the track. (can be <code>null</code>)
     * @return All tracks that have the given author. (never <code>null</code>)
     * @throws SQLException On error, when getting the tracks.
     */
    @Throws(SQLException::class)
    fun findTracksWithAuthor(con: Connection, @Nullable author: String?): Collection<Track> {
        Objects.requireNonNull(con, "con must not be null")
        con.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_TRACK_AUTHOR = ${
                    convertStringValue(
                        author
                    )
                }"
            ).use { result ->
                val tracks: MutableCollection<Track> = ArrayList()
                while (result.next()) {
                    val id = result.getLong(COLUMN_TRACK_ID)
                    val name = result.getString(COLUMN_TRACK_NAME)
                    val trackAuthor = result.getString(COLUMN_TRACK_AUTHOR)
                    val audioUrl = result.getString(COLUMN_AUDIO_URL)
                    tracks.add(Track(id, name, trackAuthor, audioUrl))
                }
                return tracks
            }
        }
    }

    /**
     * Finds the track with the given audio url.
     *
     * @param con     the database connection. (never <code>null</code>)
     * @param audioUrl the audio url of the track. (can be <code>null</code>)
     * @return An optional of the track. The optional will be empty, if no track was found. (never <code>null</code>)
     * @throws SQLException On error, when getting the track.
     */
    @Throws(SQLException::class)
    fun findTrackWithAudioUrl(con: Connection, @Nullable audioUrl: String?): Optional<Track> {
        Objects.requireNonNull(con, "con must not be null")
        con.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_AUDIO_URL = ${
                    convertStringValue(
                        audioUrl
                    )
                }"
            ).use { result ->
                if (result.next()) {
                    val id = result.getLong(COLUMN_TRACK_ID)
                    val name = result.getString(COLUMN_TRACK_NAME)
                    val trackAuthor = result.getString(COLUMN_TRACK_AUTHOR)
                    val url = result.getString(COLUMN_AUDIO_URL)
                    val track = Track(id, name, trackAuthor, audioUrl!!)
                    return Optional.of(track)
                }
                return Optional.empty()
            }
        }
    }

}