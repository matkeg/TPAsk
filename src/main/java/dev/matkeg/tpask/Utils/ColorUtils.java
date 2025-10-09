package dev.matkeg.tpask.Utils;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import dev.matkeg.tpask.TPAsk;

/* --------------------------- MAIN --------------------------- */
public final class ColorUtils {
    // Modules
    private final TPAsk plugin;

    // Class Constructor
    public ColorUtils(TPAsk plugin) {
        this.plugin = plugin;
    }
    
    /* ----------------------- APIs ----------------------- */

    /**
     * Parses a color name or hex string into a TextColor.
     * Supports both NamedTextColor (like "RED") and hex codes (like "#ff0000").
     *
     * @param colorString The color name or hex string
     * @return The parsed TextColor, or null if invalid
     */
    public TextColor parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) return null;

        // Try named color (case-insensitive)
        NamedTextColor named = NamedTextColor.NAMES.value(colorString.toLowerCase());
        if (named != null) return named;

        // Try hex color (#RRGGBB)
        if (colorString.startsWith("#")) {
            return TextColor.fromHexString(colorString);
        }

        // No valid color found
        return null;
    }
}
