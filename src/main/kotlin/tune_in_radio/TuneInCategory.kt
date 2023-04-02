package tune_in_radio

data class TuneInCategory(var available: Boolean, var genreId: String, val title: String) {
    companion object {
        private const val NOT_AVAILABLE_STR = "N/A"

        /** Object for when the tune in category is not available */
        val CATEGORY_NOT_AVAILABLE = TuneInCategory(false, NOT_AVAILABLE_STR, NOT_AVAILABLE_STR)
    }
}