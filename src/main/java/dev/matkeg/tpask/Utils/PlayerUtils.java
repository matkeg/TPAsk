package dev.matkeg.tpask.Utils;

import dev.matkeg.tpask.Managers.LanguageManager;
import dev.matkeg.tpask.TPAsk;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.Registry;
import org.bukkit.Location;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.Component;

import java.util.Map;

/* --------------------------- MAIN --------------------------- */
public class PlayerUtils {
    // Modules
    private final TPAsk plugin;
    private final Output output;
    private final LanguageManager langMan;
    private final FileConfiguration config;

    // Class Constructor
    public PlayerUtils (TPAsk plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.output = plugin.getOutput();
        this.langMan = plugin.getLanguageManager();
    }
    
    /* ----------------------- APIs ----------------------- */

    /**
     * Checks whether the passed player object is a valid one
     * and whether the player is connected to the server.
     * 
     * @param plr The player object.
     */
    public boolean isValid(Player player) {
        // Return false if the player object is
        // falde or if they are not online.
        if (player == null) return false;
        return player.isOnline();
    }
    
    /**
     * Safely fetches the player's name.
     * 
     * @param plr The player object.
     * @param disconn A optional boolean value which changes
     * the default fallback name to its disconnected variant.
     * 
     * @return A string representing the player's name, or 
     * a fallback name, depending on the disconnected value,
     * in case the player's name cannot be fetched.
     */
    public String getName(Player plr, String def) { 
        if (def != null) {
            if (!this.isValid(plr)) return def;
                return (plr.getName() != null) ?
                        plr.getName() : def;
        } else {
            if (!this.isValid(plr)) return "N/A";
                return (plr.getName() != null) ?
                        plr.getName() : langMan.getLocalizedString(
                                "messages.unknown_player_name", "N/A");
        }
    }
    
    /**
     * Teleports the given player to another player,
     * with penalties taken into account
     * 
     * @param subject The player which gets teleported.
     * @param destination The player who the 'subject' player is teleported to.
     * 
     * @see config.yml - hunger_penalty entry to configure the effect
     * and debufs applied to the player who teleported.
     */
    public void teleport(Player subject, Player destination) {
        // Check if both players are valid
        if (!this.isValid(subject) && !this.isValid(destination)) return;
        
        // Apply the hunger / saturation penalty
        if (config.getBoolean("hunger_penalty.enabled", false)) {
            String penalty_type = config.getString("hunger_penalty.type", "DISTANCE").toUpperCase();
            
            int pointsLost = 0, new_points_value = 20;
            float divideFactor = (float) config.getDouble("hunger_penalty.saturation_divide_on_tp", 4.0);
            float new_saturation_value = (float) Math.max(0, subject.getSaturation() / divideFactor);
            
            switch (penalty_type) {
                case "DISTANCE":
                    Location subjLoc = subject.getLocation();
                    Location destLoc = destination.getLocation();
                    double distance = subjLoc.distance(destLoc);
                    int blocks_per_point = config.getInt("hunger_penalty.blocks_per_point_lost", 64);

                    output.print("Distance: ", distance);
                    
                    pointsLost = (int) Math.round(distance / blocks_per_point);
                    break;

                default: // FIXED
                    pointsLost = config.getInt("hunger_penalty.points_lost_fixed", 4);
                    break;
            }
            
            // Check the prevent_starving value, in order to know the minimum allowed food value
            int currentFoodLevel = subject.getFoodLevel();
            if (config.getBoolean("prevent_starving", true) && currentFoodLevel > 0) {
                new_points_value = Math.max(1, Math.min(20, currentFoodLevel - pointsLost));
            } else {
                new_points_value = Math.max(0, Math.min(20, currentFoodLevel - pointsLost));
            }
            
            // Change the food and saturation;
            subject.setFoodLevel(new_points_value);
            subject.setSaturation(new_saturation_value);
            
            // Update the health status client-side;
            subject.sendHealthUpdate();
        }
        
        // Teleports the subject player to the destination player.
        subject.teleport(destination, PlayerTeleportEvent.TeleportCause.COMMAND);
    }
    
    /**
     * Plays a preset sound, either client-side 
     * or globally, based on config.yml entries
     *
     * @param plr  The target player.
     * @param path The sound.key's element, e.g., "timedout".
     */
    public void playPresetSound(Player plr, String name) {
        // Check if the player is valid
        if (plr == null || !plr.isOnline()) return;

        // Get the sound name from config
        String soundKeyId = config.getString("sounds.keys." + name, "").toLowerCase();
        if (soundKeyId.isEmpty()) return;

        try { 
            // Determine if the sound is global and get the sound key
            boolean global = config.getBoolean("sounds.played_globally." + name, false);
            
            NamespacedKey soundKey =  NamespacedKey.minecraft(soundKeyId);
            var soundObject = Registry.SOUNDS.getOrThrow(soundKey);
            
            if (global) { // Play the sounds
                plr.getWorld().playSound(plr.getLocation(), 
                        soundObject, SoundCategory.PLAYERS, 1f, 1f);
            } else {
                plr.playSound(plr.getLocation(),
                        soundObject, SoundCategory.MASTER, 1f, 1f);
            }

        } catch (Exception e) {
            // Report to the console about the missing sound.
            output.warn("playPresetSound(): The requested sound ",
                    name, " (", soundKeyId, ") is not available.");
        }
    }
    
    /**
     * Sends a localized, pre-made message to the given player.
     * 
     * @param plr The player which will receive the message.
     * @param path The langFile path to the localized message.
     * @param placeholders A map of the placeholders in the message.
     * <br><br>
     * <b>placeholders</b> variable replace the templates in texts with the
     * dynamic text, usually used to place usernames within the message.
     * <br><br>
     * Mapped like: (placeholder tag -> dynamic text)
     */
    public void sendPresetMessage(Player plr, String path, Map<String, String> placeholders) {
        // Check if the player argument is a valid one
        if (!this.isValid(plr)) return;

        // Get the message, based on the path
        String msg = langMan.getLocalizedString(path, null);
        if (msg == null || msg.isEmpty()) return;

        // Add the appropriate color code to the message
        msg = langMan.getColorForString(path) + msg;

        // Replace placeholders one by one
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String placeholder = entry.getKey();
                String value = entry.getValue();

                if (placeholder != null && value != null) {
                    msg = msg.replace(placeholder, value);
            // Send the message to the player        
        }}} 
        
        // Send the message as a text component.
        plr.sendMessage(msg);
    }
    
    /**
     * Creates a clickable component using the given arguments.
     * 
     * @param text The text that will be clickable.
     * @param hover The text that will be shown when the text is hovered over.
     * @param command The command that will be executed when the user clicks on the text.
     * @param color The TextColor of the color that will be applied to the text.
     * 
     * @see dev.matkeg.tpask.Utils.ColorUtils
     */
    public Component clickable(String text, String hover, String command, TextColor color) {
        // Create the base component
        Component base = Component.text(text == null ? "" : text);

        // Apply color if provided
        if (color != null) {
            base = base.color(color);
        }

        // Add hover event if provided
        if (hover != null && !hover.isEmpty()) {
            base = base.hoverEvent(HoverEvent.showText(Component.text(hover)));
        }

        // Add click event if provided
        if (command != null && !command.isEmpty()) {
            base = base.clickEvent(ClickEvent.runCommand(command));
        }

        return base;
    }
}
