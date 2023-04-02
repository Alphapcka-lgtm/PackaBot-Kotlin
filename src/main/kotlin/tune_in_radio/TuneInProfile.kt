package tune_in_radio

data class TuneInProfile(
    var available: Boolean,
    var title: String,
    var description: String,
    var type: String,
    var country: String,
    var shareUrl: String,
) {
    companion object {
        private const val NOT_AVAILABLE = "N/A"

        /** Object to use when the tune in profile is not available */
        val PROFILE_NOT_AVAILABLE =
            TuneInProfile(false, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE)
    }
}
