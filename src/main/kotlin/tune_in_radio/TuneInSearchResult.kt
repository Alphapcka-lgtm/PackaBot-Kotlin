package tune_in_radio

class TuneInSearchResult {

    /**
     * Creates a [TuneInSearchResult] object of the audio outlines.
     *
     * @param audioOutlines the audio outlines
     */
    constructor(audioOutlines: List<TuneInAudioOutline>) {
        this.audioOutlines = audioOutlines
        errorResponse = null
    }

    /**
     * Creates a [TuneInSearchResult] object of an error response.
     *
     * @param errorResponse the error response.
     */
    constructor(errorResponse: TuneInErrorResponse) {
        audioOutlines = null
        this.errorResponse = errorResponse
    }

    /** The [TuneInAudioOutline]s send from the search api. Will be `null` if [isError] returns `true`. */
    val audioOutlines: List<TuneInAudioOutline>?

    /** The [TuneInErrorResponse] send from the search api on error. Will be `null` if [isError] returns `false`. */
    val errorResponse: TuneInErrorResponse?

    /**
     * Indicates if the search has returned an error response or not.
     *
     * @return `true` when the search response was an error.
     */
    fun isError(): Boolean {
        return errorResponse != null
    }
}