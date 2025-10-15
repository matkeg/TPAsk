package dev.matkeg.tpask.utilities;

import dev.matkeg.tpask.managers.*;
import dev.matkeg.tpask.utilities.*;
import dev.matkeg.tpask.PluginMain;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.Component;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;
import java.util.Map;

/* ---------------------- MAIN CLASS ---------------------- */
public class MessageUtils {
    // Modules
    private PlayerUtils plrU;
    private final PluginMain plugin;
    private final ColorUtils colU;
    private final ConfigUtils conU;
    private final OutputUtils output;
    private final LanguageManager langMan;

    // Constructor
    public MessageUtils (PluginMain plugin) {
        this.plugin = plugin;
        this.output = plugin.getOutput();
        this.colU = plugin.getColorUtils();
        this.conU = plugin.getConfigUtils();
        this.langMan = plugin.getLanguageManager();
    }
    
    // Storage
    private final Map<UUID, BukkitTask> actionBarTasks = new HashMap<>();
    
    // Prevent a circular dependency
    public void setPlayerUtils(PlayerUtils plrU) { 
        if (this.plrU == null) { this.plrU = plrU; }
    }
    
    /* --------------------- FUNCTIONS -------------------- */

    private Component constructMessage(String path, String tag, String replaced) {
        // Get the message, based on the path
        String msg = langMan.getUserMessage(path, null);
        if (msg == null || msg.isEmpty()) return null;

        // Add the appropriate color code to the message
        msg = langMan.getColorForMessage(path) + msg;

        // Replace placeholders, if they are given.
        if (tag != null && replaced != null) 
            msg = msg.replace(tag, replaced);

        Component comp = LegacyComponentSerializer
                .legacySection().deserialize(msg);
        
        return comp;
    };
    
    /* ----------------------- APIs ----------------------- */
    
    /**
     * Sends a localized user-facing message to the specified player
     * either to the action bar (if enabled) or the chat.
     * 
     * @param plr The player which will receive the message.
     * @param key The key from the messages entry inside the lang file.
     * @param tag A string representing the replacement tag.
     * @param replacement A string which will replace the replacement tag.
     */
    public void userMessage(Player plr, String key, String tag, String replacement) {
        if (conU.getBoolean("action_bar.enabled", true)) {
            this.actionMessage(plr, key, tag, replacement);
        } else {
            this.chatMessage(plr, key, tag, replacement);
        }
    }
    
    /** @see #userMessage(Player, String, String, String) */
    public void userMessage(Player plr, String key) {
        this.userMessage(plr, key, null, null);
    }

    /**
     * Sends a localized user-facing message to the chat of the given player.
     * 
     * @param plr The player which will receive the message.
     * @param key The key from the messages entry inside the lang file.
     * @param tag A string representing the replacement tag.
     * @param replacement A string which will replace the replacement tag.
     * @param addon A optional Component which will be appended to the base message.
     */
    public void chatMessage(Player plr, String key, String tag, String replacement, Component addon) {
        // Check if the player is valid.
        if (!plrU.isValid(plr)) return;

        // Construct the base message.
        Component baseMsg = constructMessage(key, tag, replacement);
        if (baseMsg == null) return;
        
        // Append the addon, if provided. 
        if (addon != null) {
            baseMsg = baseMsg.append(Component.text(" ")).append(addon);
        }
        
        // Send the message.
        plr.sendMessage(baseMsg);
    }

    /** @see #chatMessage(Player, String, String, String, Component) */
    public void chatMessage(Player plr, String key, String tag, String replacement) {
        chatMessage(plr, key, tag, replacement, null);
    }
    
    /** @see #chatMessage(Player, String, String, String, Component) */
    public void chatMessage(Player plr, String key) {
        chatMessage(plr, key, null, null, null);
    }
   
    /**
     * Sends a localized user-facing message to the action bar of the given player.
     * 
     * @param plr The player to send the action message to.
     * @param key The key from the messages entry inside the lang file.
     * @param tag A string representing the replacement tag.
     * @param replacement A string which will replace the replacement tag.
     */
    public void actionMessage(Player plr, String key, String tag, String replacement) {
        // Check if the player is valid.
        if (!plrU.isValid(plr)) return;

        // Construct the base message.
        Component baseMsg = constructMessage(key, tag, replacement);
        if (baseMsg == null) return;
        
        // Send the action message immediately.
        plr.sendActionBar(baseMsg);
        UUID plrId = plr.getUniqueId();
        
        // Cancel any existing task for this player
        BukkitTask previousTask = actionBarTasks.remove(plrId);
        if (previousTask != null) previousTask.cancel();

        // Determine how many repeats are needed
        int durationSeconds = conU.getInt("action_bar.duration", 5, 2, 30);
        int durationTicks = durationSeconds * 20, intervalTicks = 20;
        int repeats = Math.max(1, (durationTicks + intervalTicks - 1) / intervalTicks); // ceil

        // Schedule the repeating task (start immediately)
        BukkitTask task = new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (!plrU.isValid(plr) || count >= repeats) {
                    actionBarTasks.remove(plrId);
                    cancel();
                    return;
                }
                plr.sendActionBar(baseMsg);
                count++;
            }
        }.runTaskTimer(plugin, 0L, intervalTicks);


        // Store the task so it can be cancelled if a new message is sent
        actionBarTasks.put(plrId, task);
    }
    
    /**
     * Creates a clickable component using the given arguments.
     * 
     * @param text The text that will be clickable.
     * @param hover The text that will be shown when the text is hovered over.
     * @param command The command that will be executed when the user clicks on the text.
     * @param color The TextColor of the color that will be applied to the text.
     * 
     * @see dev.matkeg.tpask.utilities.ColorUtils
     */
    public Component createClickable(String text, String hover, String command, TextColor color) {
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
    
    /**
     * Creates a clickable component from the config files.
     * 
     * @param name The name of the clickable button config entry.
     * @param tag A string representing the replacement tag.
     * @param replacement A string which will replace the replacement tag.
     */
    public Component clickableFromConfig(String name, String tag, String replacement) {
        String path = "clickables." + name;
        String text = langMan.getLocalizedString(path + ".text", "[?]"),
               hover = langMan.getLocalizedString(path + ".hover", "N/A"), 
               color = langMan.getLocalizedString(path + ".color", "WHITE"),
               command = langMan.getLocalizedString(path + ".command", "/help");
        
        if (text.equals("[?]") || hover.equals("N/A") || command.equals("/help")) {
            output.warn("Clickable component", name, "has at least one missing string", 
                    "entry in language pack:", langMan.getCurrentLanguageSetting());
        }
        
        if (tag != null && replacement != null) {
            return createClickable(
                text.replace(tag, replacement),
                hover.replace(tag, replacement),
                command, colU.parseColor(color));
        } else return createClickable(text, hover, command, colU.parseColor(color));
    }
    
    /** Cancels all action bar tasks. */
    public void cancelAllActionBars() {
        for (var t : actionBarTasks.values()) if (t != null) t.cancel();
        actionBarTasks.clear();
    }
}
