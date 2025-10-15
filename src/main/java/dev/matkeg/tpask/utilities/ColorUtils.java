package dev.matkeg.tpask.utilities;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import dev.matkeg.tpask.PluginMain;

/* ---------------------- MAIN CLASS ---------------------- */
public final class ColorUtils {
    // Modules
    private final PluginMain plugin;

    // Constructor
    public ColorUtils(PluginMain plugin) { this.plugin = plugin; }
    
    /* ----------------------- APIs ----------------------- */

    /**
     * Parses a color name or hex string into a TextColor.
     * Supports both NamedTextColors (like "RED") and hex codes (like "#ff0000").
     *
     * @param colorString The color name or hex string
     * @return The parsed TextColor, or null if invalid
     */
    public TextColor parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) return null;

        // Try named color
        NamedTextColor named = NamedTextColor.NAMES.value(colorString.toLowerCase());
        if (named != null) return named;

        // Try hex color
        if (colorString.startsWith("#")) {
            return TextColor.fromHexString(colorString);
        }

        // No valid color found
        return null;
    }
}
