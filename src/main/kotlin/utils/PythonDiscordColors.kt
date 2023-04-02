package utils

import java.awt.Color

/**
 * Enum containing the Colors from the discord.py bot api.
 *
 * @param color the color
 * @author Michael
 */
enum class PythonDiscordColors(color: Color) {
    BLUE(Color(52, 152, 219)),
    BLURPLE(Color(114, 137, 218)),
    DARK_BLUE(Color(32, 102, 148)),
    DARK_GOLD(Color(194, 124, 14)),
    DARK_GREEN(Color(31, 139, 76)),
    DARK_GREY(Color(96, 125, 139)),
    DARK_MAGENTA(Color(173, 20, 87)),
    DARK_ORANGE(Color(168, 67, 0)),
    DARK_PURPLE(Color(113, 54, 138)),
    DARK_RED(Color(153, 45, 34)),
    DARK_TEAL(Color(17, 128, 106)),
    DARK_THEME(Color(54, 57, 63)),
    DARKER_GRAY(Color(84, 110, 122)),
    GOLD(Color(241, 196, 15)),
    GREEN(Color(46, 204, 113)),
    GREYPLE(Color(153, 170, 181)),
    LIGHT_GREY(Color(151, 156, 159)),
    MAGENTA(Color(233, 30, 99)),
    ORANGE(Color(230, 126, 34)),
    PURPLE(Color(155, 89, 182)),
    RED(Color(231, 76, 60));

    private val m_Color: Color

    /**
     * Constructor.
     */
    init {
        m_Color = color
    }

    /**
     * Gets the color.
     *
     * @return the color.
     */
    val color = m_Color
}