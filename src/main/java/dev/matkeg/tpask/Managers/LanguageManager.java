package dev.matkeg.tpask.Managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.PluginCommand;

import dev.matkeg.tpask.Utils.Output;
import dev.matkeg.tpask.TPAsk;

import java.util.List;
import java.util.Map;
import java.io.File;

/* --------------------------- MAIN --------------------------- */
public class LanguageManager {
    // Modules
    private final TPAsk plugin;
    private final Output output;
    
    // Private Variables
    private FileConfiguration langFile;
    private FileConfiguration colorFile;
    private FileConfiguration config;
    private String langSetting;
    
    // Class Constructor
    public LanguageManager(TPAsk plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.output = plugin.getOutput();
    }
    
    /* --------------------- FUNCTIONS -------------------- */
   
    private synchronized void loadLangFile() {
        // Check if the lang file is already loaded
        if (langFile != null) return;
        
        // Load language from config.yml
        langSetting = config.getString("language", "en_us").toLowerCase();
        File langFilePath = new File(plugin.getDataFolder(), "languages/" + langSetting + ".yml");

        // If the lang file doesn't exist, fallback to en_US
        if (!langFilePath.exists()) {
            output.warn("No language pack file found for language '" + langSetting + "'.");
            
            langSetting = "en_us";
            langFilePath = new File(plugin.getDataFolder(), "languages/" + langSetting + ".yml");

            // If en_US is also missing, throw an exception
            if (!langFilePath.exists()) {
                throw new IllegalStateException("Default language pack 'en_us' is missing!");
            }
        }
        
        // Load the language file, and set the langFile variable;
        langFile = YamlConfiguration.loadConfiguration(langFilePath);
    }
    
    private synchronized void loadColorsFile() {
        // Check if the colorFile is already loaded
        if (colorFile != null) return;

        // Path to colors.yml
        File colorFilePath = new File(plugin.getDataFolder(), "colors.yml");

        if (colorFilePath.exists()) {
            // Load the actual file
            colorFile = YamlConfiguration.loadConfiguration(colorFilePath);
        } else {
            // Warn about missing file
            output.warn("No color map file found.");

            // Use an empty yml config.
            colorFile = new YamlConfiguration();
        }
    }

    /* ----------------------- APIs ----------------------- */
    
    /**
     * Saves the default language packs (and colors.yml) at 
     * src/main/resources/languages to the server's plugin data folder.
    */
    public synchronized void saveDefaultPacks() {
        // Save the colors.yml file
        File colorsFile = new File(plugin.getDataFolder(), "colors.yml");
        if (!colorsFile.exists()) {
            plugin.saveResource("colors.yml", false);
        }
        
        // Save the en_us.yml language pack file
        File englishFile = new File(plugin.getDataFolder(), "languages/en_us.yml");
        if (!englishFile.exists()) plugin.saveResource("languages/en_us.yml", false);

        List<String> languages = config.getStringList("available_language_packs");
        
        for (String lang : languages) {
            String fileName = lang.toLowerCase() + ".yml";
            File langFile = new File(plugin.getDataFolder(), "languages/" + fileName);

            if (!langFile.exists()) {
                plugin.saveResource("languages/" + fileName, false);
            }
        }
    }
    
    /**
     * Sets up the command descriptions and usage strings,
     * based on the currently specified language in config.yml
    */
    public void localizeCommands() {
        for (PluginCommand cmd : plugin.getDescription().getCommands().keySet().stream()
                .map(plugin::getCommand).toList()) {
            if (cmd == null) continue;

            String desc = this.getLocalizedString(cmd.getName() + ".description", "");
            String usage = this.getLocalizedString(cmd.getName() + ".usage", "");

            if (desc != null && !desc.isEmpty())
                cmd.setDescription(desc);

            if (usage != null && !usage.isEmpty())
                cmd.setUsage(usage);
        }
    }

    /**
     * Safely retrieves a localized string from the loaded language file.
     * @param key The path in the language YAML
     * @param def Default value to return if missing
     * @return Localized string or fallback
     * 
     * @see src/main/resources/languages/*
     */
    public String getLocalizedString(String key, String def) {
        // Get the lang file;
        this.loadLangFile();

        // Return the localized string, fallback to def if missing
        return langFile.getString(key, def != null ? def :
            key.toUpperCase()+"::MISSING_LANG_FILE_ENTRY");
    }
    
    /**
     * Safely retrieves a color code string from the loaded colors file.
     * @param key The path in the language YAML
     * @return A string with the color code or §r
     * 
     * @see src/main/resources/languages/colors.yml
     */
    public String getColorForString(String key) {
        // Get the colors file;
        this.loadColorsFile();
        
        // Return the color code string, or "§r" to reset.
        return colorFile.getString(key, "§r");
    }
}
