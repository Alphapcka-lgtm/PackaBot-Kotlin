package database

/**
 * Util class to convert values for a SQL statements.<br></br>
 * **Not suitable for Column names!**
 *
 * @author Michael
 */
object SqlStatementValueConverter {
    /**
     * Converts the value of the String to be usable in a SQL statement.
     *
     * @param str the value to convert. (can be `null`)
     * @return the converted value or `null` if the param was null.
     */
    fun convertStringValue(str: String?): String? {
        if (str == null) {
            return null
        }
        val sb = StringBuilder()
        sb.append("'")
        sb.append(str.replace("'", "\\'"))
        sb.append("'")
        return sb.toString()
    }
}