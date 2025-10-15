package dev.matkeg.tpask.utilities;

import dev.matkeg.tpask.utilities.VersionUtils;
import dev.matkeg.tpask.utilities.OutputUtils;

import dev.matkeg.tpask.PluginMain;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/* ---------------------- MAIN CLASS ---------------------- */
public final class ConfigUtils {
    // Modules
    private final PluginMain plugin;
    private final VersionUtils verU;
    private final OutputUtils output;
    private FileConfiguration config;
    
    // Constructor
    public ConfigUtils(PluginMain plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.output = plugin.getOutput();
        this.verU = plugin.getVersionUtils();
    }
    
    /* ----------------------- APIs ----------------------- */

    /**
     * Checks whether the plugin is compatible and warns
     * the server operators to update the config file.
     */
    public boolean checkConfigCompatibility() {
        String maximumRequiredVersion = config.getString("version_compatibility.maximum", null);
        String minimumRequiredVersion = config.getString("version_compatibility.minimum", null);
        String currentVersion = plugin.getDescription().getVersion();

        if (minimumRequiredVersion == null || maximumRequiredVersion == null) {
            output.error("Cannot read which plugin versions the plugin's configuration file is compatible with. " +
                                                 "Please add or reconfigure the missing 'version_compatibility' " +
                                                                "entry with the 'minimum' and 'maximum' fields. ");
            output.warn("OR delete the current configuration file and restart the server in order " +
                           "for the plugin to automatically generate a compatible config.yml file.");
            return false;
        }

        try {
            // If plugin version < minimum then the plugin is too old for this config
            if (verU.isVersionLower(currentVersion, minimumRequiredVersion)) {
                output.error("This plugin is outdated for its configuration file!");
                output.warn("Please downgrade the configuration file manually or delete the " +
                        "current configuration file and restart the server in order for the " +
                             "plugin to automatically generate a compatible config.yml file.");

                return false;
            }

            // If plugin version > maximum then the config is too old for this plugin
            if (verU.isVersionHigher(currentVersion, maximumRequiredVersion)) {
                output.error("This plugin's configuration file is outdated!");
                output.warn("Please update the configuration file manually or delete the " +
                     "current configuration file and restart the server in order for the " +
                          "plugin to automatically generate a compatible config.yml file.");
                return false;
            }

            // In range [minimum, maximum]
            return true;
        } catch (IllegalArgumentException ex) {
            output.error("Failed to parse version string: " + ex.getMessage());
            return false;
        }
    }

    
    /**
     * Reloads the plugin's configuration file.
     * 
     * @param invoker The player attempting to reload the plugin's config.
     */
    public void reloadConfig(Player invoker) {
        if (invoker.hasPermission("tpask.reload")) {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
            output.print("Reloaded the plugin's configuration!");
            invoker.sendMessage("Reloaded the plugin's configuration!");
            
        } else if (config.getBoolean("debug.invalid_permissions", false)) { 
            output.warn(invoker.getName(), "attempted to reload the plugin's",
                               "configuration with insufficient permissions.");
        }
    }
    
    /**
     * Gets a string value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     */
    public String getString(String path, String def) {
        return config.getString(path, def);
    }
    
    /**
     * Gets a string value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     */
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    /**
     * Gets a string value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     */
    public boolean getBoolean(String path, Boolean def) {
        if (def == null) def = false;
        return config.getBoolean(path, def);
    }
    
    /**
     * Gets a integer value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     * 
     * @param min The minimum value which could be returned.
     * @param max The maximum value which could be returned.
     * 
     * @exception Min cannot be greater than max!
     */
    public int getInt(String path, Integer def, Integer min, Integer max) {
        if (min != null && max != null && min > max)
            throw new IllegalArgumentException("getInt(): min cannot be greater than max");
        
        int value;
        if (def != null) {
            value = config.getInt(path, def);
        } else {
            output.error("getInt(): Default cannot be null - setting to 1");
            value = config.getInt(path, 1);
        }
        
        if (min != null && value < min) value = min;
        if (max != null && value > max) value = max;
        
        return value;
    }
    
    /** @see #getInt(String, Integer, Integer, Integer) */
    public int getInt(String path, Integer def, Integer min) {
        return this.getInt(path, def, min, null);
    }
    
    /** @see #getInt(String, Integer, Integer, Integer) */
    public int getInt(String path, Integer def) {
        return this.getInt(path, def, null, null);
    }
    
    /**
     * Gets a long value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     * 
     * @param min The minimum value which could be returned.
     * @param max The maximum value which could be returned.
     * 
     * @exception Min cannot be greater than max!
     */
    public long getLong(String path, Long def, Long min, Long max) {
        if (min != null && max != null && min > max)
            throw new IllegalArgumentException("getLong(): min cannot be greater than max");

        long value;
        if (def != null) {
            value = config.getLong(path, def);
        } else {
            output.error("getLong(): Default cannot be null - setting to 1");
            value = config.getLong(path, 1);
        }
        
        if (min != null && value < min) value = min;
        if (max != null && value > max) value = max;

        return value;
    }
    
    /** @see #getLong(String, Long, Long, Long) */
    public long getLong(String path, Long def, Long min) {
        return this.getLong(path, def, min, null);
    }
    
    /** @see #getLong(String, Integer, Integer, Integer) */
    public long getLong(String path, Long def) {
        return this.getLong(path, def, null, null);
    }
    
    /**
     * Gets a double value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     * 
     * @param min The minimum value which could be returned.
     * @param max The maximum value which could be returned.
     * 
     * @exception Min cannot be greater than max!
     */
    public double getDouble(String path, Double def, Double min, Double max) {
        if (min != null && max != null && min > max)
            throw new IllegalArgumentException("getDouble(): min cannot be greater than max");

        double value;
        if (def != null) {
            value = config.getDouble(path, def);
        } else {
            output.error("getDouble(): Default cannot be null - setting to 1");
            value = config.getDouble(path, 1.0);
        }

        if (min != null && value < min) value = min;
        if (max != null && value > max) value = max;

        return value;
    }
    
    /** @see #getDouble(String, Double, Double, Double) */
    public double getDouble(String path, Double def, Double min) {
        return this.getDouble(path, def, min, null);
    }
    
    /** @see #getDouble(String, Double, Double, Double) */
    public double getDouble(String path, Double def) {
        return this.getDouble(path, def, null, null);
    }
    
    /**
     * Gets a int list value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     * 
     * @param min The minimum value which could be returned.
     * @param max The maximum value which could be returned.
     * 
     * @exception Min cannot be greater than max!
     */
    public List<Integer> getIntList(String path, List<Integer> def, Integer min, Integer max) {
        if (min != null && max != null && min > max)
            throw new IllegalArgumentException("getIntList(): min cannot be greater than max");
        
        List<Integer> list = config.getIntegerList(path);
        if (list == null || list.isEmpty()) {
            list = def != null ? new ArrayList<>(def) : new ArrayList<>();
        }

        if (min != null || max != null) {
            for (int i = 0; i < list.size(); i++) {
                int v = list.get(i);
                if (min != null && v < min) v = min;
                if (max != null && v > max) v = max;
                list.set(i, v);
            }
        }

        return list;
    }
    
    /**
     * Gets a long list value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     * 
     * @param min The minimum value which could be returned.
     * @param max The maximum value which could be returned.
     * 
     * @exception Min cannot be greater than max!
     */
    public List<Long> getLongList(String path, List<Long> def, Long min, Long max) {
        if (min != null && max != null && min > max)
            throw new IllegalArgumentException("getLongList(): min cannot be greater than max");
        
        List<Long> list = config.getLongList(path);
        if (list == null || list.isEmpty()) {
            list = def != null ? new ArrayList<>(def) : new ArrayList<>();
        }

        if (min != null || max != null) {
            for (int i = 0; i < list.size(); i++) {
                long v = list.get(i);
                if (min != null && v < min) v = min;
                if (max != null && v > max) v = max;
                list.set(i, v);
            }
        }

        return list;
    }
    
    /**
     * Gets a double list value from the plugin's configuration file.
     * 
     * @param path The path to the entry where the wanted value is.
     * @param def The default value returned if there is no such entry.
     * 
     * @param min The minimum value which could be returned.
     * @param max The maximum value which could be returned.
     * 
     * @exception Min cannot be greater than max!
     */
    public List<Double> getDoubleList(String path, List<Double> def, Double min, Double max) {
        if (min != null && max != null && min > max)
            throw new IllegalArgumentException("getDoubleList(): min cannot be greater than max");
        
        List<Double> list = config.getDoubleList(path);
        if (list == null || list.isEmpty()) {
            list = def != null ? new ArrayList<>(def) : new ArrayList<>();
        }

        if (min != null || max != null) {
            for (int i = 0; i < list.size(); i++) {
                double v = list.get(i);
                if (min != null && v < min) v = min;
                if (max != null && v > max) v = max;
                list.set(i, v);
            }
        }

        return list;
    }
}
