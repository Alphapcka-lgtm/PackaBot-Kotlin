package music

/**
 * Indicates the type the bot is looped (single or queue)
 *
 * @author Michael
 * @version 1.0
 * @see {@link TrackScheduler}
 *
 * @see {@link Player}
 *
 * @since 0.0.1-SNAPSHOT
 */
enum class LoopMode(val mode: String) {
    /**
     * When only the current playing track is looped
     */
    SINGLE("single"),

    /**
     * When the
     */
    QUEUE("queue");

    override fun toString(): String {
        return super.name + "(" + mode + ")"
    }
}