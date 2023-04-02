package database.dao

import java.sql.Connection
import java.sql.SQLException
import java.util.*

interface Dao<T> {

    /**
     * Gets the object with the given id.
     *
     * @param con the database connection. (never `null`)
     * @param id the id.
     * @return An optional of the object.
     * @throws SQLException On error.
     */
    @Throws(SQLException::class)
    fun get(con: Connection, id: Long): Optional<T>

    /**
     * Get all objects
     *
     * @param con the database connection. (never `null`)
     * @return all objects
     * @throws SQLException On error.
     */
    @Throws(SQLException::class)
    fun getAll(con: Connection): Collection<T>

    /**
     * Saves the object. Either creates a new one in the database if no primary key is given or updates the existing one.
     *
     * @param con the database connection. (never `null`).
     * @param t object to save.
     * @throws SQLException On error.
     */
    @Throws(SQLException::class)
    fun save(con: Connection, t: T): T

    /**
     * Updates the object with the id with the given params.
     *
     * @param con the database connection. (never `null`)
     * @param id the id.
     * @param params the params to update the object with. (never `null`)
     * @return an optional of the updated object. This is going to be empty, if the object to update was not found!
     * @throws SQLException On error.
     */
    @Throws(SQLException::class)
    fun update(con: Connection, id: Long, params: Array<String>): Optional<T>

    /**
     * Deletes the object
     *
     * @param con the database connection. (never `null`)
     * @param t object to delete. (never `null`)
     * @return an optional of the deleted object. This is going to be empty, if the object to delete was not found!
     * @throws SQLException On error.
     */
    @Throws(SQLException::class)
    fun delete(con: Connection, t: T): Optional<T>
}