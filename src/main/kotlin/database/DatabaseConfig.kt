package database

import java.util.*

data class DatabaseConfig(val databaseUrl: String, val databaseUser: String, val databasePassword: String) {
    companion object {
        /**
         * Loads the database config from a properties object.
         * @param properties the properties to load from.
         * @return a database config.
         */
        fun loadFromProperties(properties: Properties): DatabaseConfig {
            return DatabaseConfig(
                properties.getProperty("database"),
                properties.getProperty("database.user"),
                properties.getProperty("database.password")
            )
        }

        /**
         * Loads the database config from a properties object.
         * @param resourcePath the path of the resource file.
         * @return a database config.
         */
        fun loadFromResource(resourcePath: String): DatabaseConfig {
            val stream = DatabaseConfig::class.java.classLoader.getResource(resourcePath).openStream()
            val properties = Properties()
            properties.load(stream)

            return loadFromProperties(properties)
        }
    }
}