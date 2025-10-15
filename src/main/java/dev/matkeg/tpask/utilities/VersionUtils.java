package dev.matkeg.tpask.utilities;

import dev.matkeg.tpask.PluginMain;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* ---------------------- MAIN CLASS ---------------------- */
public final class VersionUtils {
    // Modules
    private final PluginMain plugin;

    // Class Constructor
    public VersionUtils(PluginMain plugin) {
        this.plugin = plugin;
    }

    /* -------------------- FUNCTIONS --------------------- */
    
    /** Extract the numeric prefix ( eg. "1.2.0" ) */
    private static String numericPrefix(String v) {
        if (v == null) 
            throw new IllegalArgumentException("version is null");
        
        Matcher m = Pattern.compile("^([0-9]+(?:\\.[0-9]+)*)").matcher(v.trim());
        if (!m.find()) 
            throw new IllegalArgumentException("No numeric version found in: " + v);
        return m.group(1);
    }

    /** Main function for version comparison */
    private static int compareVersions(String v1, String v2) {
        String n1 = numericPrefix(v1);
        String n2 = numericPrefix(v2);

        String[] p1 = n1.split("\\.");
        String[] p2 = n2.split("\\.");

        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int a = i < p1.length ? Integer.parseInt(p1[i]) : 0;
            int b = i < p2.length ? Integer.parseInt(p2[i]) : 0;
            if (a != b) return a - b;
        }
        return 0;
    }

    /* ----------------------- APIs ----------------------- */
    
    /**
     * Whether the v1 is lower than v2.
     * 
     * @param v1 The subject version.
     * @param v2 The version to be compared to.
     * 
     * @return True if v1 is lower than v2 and vice versa.
     */
    public boolean isVersionLower(String v1, String v2) {
        return compareVersions(v1, v2) < 0;
    }

    /**
     * Whether the v1 is higher than v2.
     * 
     * @param v1 The subject version.
     * @param v2 The version to be compared to.
     * 
     * @return True if v1 is higher than v2 and vice versa.
     */
    public boolean isVersionHigher(String v1, String v2) {
        return compareVersions(v1, v2) > 0;
    }

    /**
     * Whether the v1 is equal to v2.
     * 
     * @param v1 The subject version.
     * @param v2 The version to be compared to.
     * 
     * @return True if v1 is equal to v2 and false if not.
     */
    public boolean isVersionEqual(String v1, String v2) {
        return compareVersions(v1, v2) == 0;
    }
}

