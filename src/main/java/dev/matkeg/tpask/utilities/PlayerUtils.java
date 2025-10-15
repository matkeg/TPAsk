package dev.matkeg.tpask.utilities;

import dev.matkeg.tpask.managers.*;
import dev.matkeg.tpask.PluginMain;

import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.World;

/* ---------------------- MAIN CLASS ---------------------- */
public class PlayerUtils {
    // Modules
    private final PluginMain plugin;
    private final MessageUtils msgU;
    private final ConfigUtils conU;
    private final OutputUtils output;
    private final LanguageManager langMan;

    // Constructor
    public PlayerUtils(PluginMain plugin) {
        this.plugin = plugin;
        this.output = plugin.getOutput();
        this.msgU = plugin.getMessageUtils();
        this.conU = plugin.getConfigUtils();
        this.langMan = plugin.getLanguageManager();
    }

    /* -------------------- FUNCTIONS --------------------- */
    
    private void warnDebug(Object... args) {
        boolean requestDebugEnabled = conU.getBoolean("debug.teleport", false);
         if (requestDebugEnabled) output.warn(args);
    }
    
    private void printDebug(Object... args) {
        boolean requestDebugEnabled = conU.getBoolean("debug.teleport", false);
         if (requestDebugEnabled) output.print(args);
    }
    
    /* ----------------------- APIs ----------------------- */
    
    /**
     * Checks whether the passed player object is valid and whether the player is
     * connected to the server.
     *
     * @param plr The player object.
     */
    public boolean isValid(Player plr) {
        if (plr == null) {
            return false;
        }
        return plr.isOnline();
    }

    /**
     * Checks whether both of the passed player objects are are valid and whether the
     * players are connected to the server.
     *
     * @param plr1 The first player object.
     * @param plr2 The second player object.
     */
    public boolean areValid(Player plr1, Player plr2) {
        return plr1 != null && plr1.isOnline()
            && plr2 != null && plr2.isOnline();
    }

    /**
     * Safely fetches the player's name.
     *
     * @param plr The player object.
     * @param def A optional default
     *
     * @return A string representing the player's name, or a fallback name, depending on
     * the disconnected value, in case the player's name cannot be fetched.
     */
    public String getName(Player plr, String def) {
        if (def != null) {
            if (!this.isValid(plr)) {
                return def;
            }
            return (plr.getName() != null)
                    ? plr.getName() : def;
        } else {
            if (!this.isValid(plr)) {
                return "N/A";
            }
            return (plr.getName() != null)
                    ? plr.getName() : langMan.getUserMessage("player_noun", "N/A");
        }
    }

    public String getName(Player plr) {
        if (!this.isValid(plr)) {
            return "N/A";
        }
        return (plr.getName() != null)
                ? plr.getName() : langMan.getUserMessage("player_noun", "N/A");
    }

    /**
     * Penalizes the given player by reducing their food level.
     *
     * @param subject The player who will be penalized
     * @param teleportingTo The location where the player will be teleported to.
     * @param tpToAnotherWorld Whether the subject is being teleported to another world.
     *
     * @see config.yml - hunger_penalty entry to configure the effect and debuffs applied
     * to the player who got teleported.
     */
    public void applyTeleportPenalty(Player subject, Location teleportingTo, boolean tpingToAnotherWorld) {
        String penaltyType = conU.getString("hunger_penalty.type", "DISTANCE");

        float saturationDivideBy = (float) conU.getDouble("hunger_penalty.saturation_divide_on_tp", 4.0, 1.0, 20.0);

        int blocksPerPointLost = conU.getInt("hunger_penalty.blocks_per_point_lost", 24, 1, 81920);
        int pointsLostFixed = conU.getInt("hunger_penalty.points_lost_fixed", 32, 1, 20);

        boolean preventStarving = conU.getBoolean("hunger_penalty.prevent_starving", true);
        boolean playSound = conU.getBoolean("hunger_penalty.penalized_sound", false);

        float newSaturationValue = 20.0f / saturationDivideBy;
        int newFoodPointValue = 20;

        float currSaturationLevel = subject.getSaturation();
        int currFoodLevel = subject.getFoodLevel();

        if (!tpingToAnotherWorld) {
            // Calculate the points lost based on the penalty type.
            int pointsLost = 0;
            switch (penaltyType) {
                case "DISTANCE":
                    Location subjLoc = subject.getLocation();
                    double distance = subjLoc.distance(teleportingTo);

                    pointsLost = (int) Math.round(distance / blocksPerPointLost);
                    break;

                default: // FIXED
                    pointsLost = pointsLostFixed;
                    break;
            }

            // Check the prevent_starving value, in order to know the minimum allowed food value
            if (preventStarving && currFoodLevel > 0) {
                newFoodPointValue = Math.max(1, Math.min(20, currFoodLevel - pointsLost));
            } else {
                newFoodPointValue = Math.max(0, Math.min(20, currFoodLevel - pointsLost));
            }
        } else { // TPing to another world will leave one fourth of the food level.
            newSaturationValue = currSaturationLevel / 4;
            newFoodPointValue = currFoodLevel / 4;
        }
        
        // Print out debug information.
        printDebug("Hunger penalty applied to", 
                    this.getName(subject), "-",
                             "New Food Level:",
                             newFoodPointValue, 
                                 "Food Delta:",
             currFoodLevel - newFoodPointValue,
                             "New Saturation:",
                            newSaturationValue, 
                           "Saturation Delta:",
      currSaturationLevel - newSaturationValue);

        // Change the food and saturation;
        subject.setSaturation(newSaturationValue);
        subject.setFoodLevel(newFoodPointValue);

        // Update the health status client-side;
        subject.sendHealthUpdate();
    }

    /**
     * Teleports the given player to another player.
     *
     * @param subject The player which gets teleported.
     * @param destination The player who the 'subject' player is teleported to.
     */
    public void teleport(Player subject, Player dest, boolean applyPenalty) {
        // Check if both players are valid
        if (!this.areValid(subject, dest)) {
            return;
        }

        World subWrld = subject.getWorld(), destWrld = dest.getWorld();
        boolean crossWorldTpEnabled = conU.getBoolean("world.cross_teleportation", true);
        boolean tpingToAnotherWorld = !subWrld.equals(destWrld);

        // Check the world of the subjects
        if (!crossWorldTpEnabled && tpingToAnotherWorld) {
            // Print out debug information.
            warnDebug("Could not teleport", 
                     this.getName(subject), "to", 
                    this.getName(dest), "because",
                    "cross world teleportation is disabled!");
        
            // Message the players
            msgU.userMessage(subject, "error_cross_world_disabled_self");
            msgU.userMessage(dest, "error_cross_world_disabled",
                    "%OTHER%", this.getName(subject));
            return;
        } if (!tpingToAnotherWorld) {
            // Calculate and print out the distance
            printDebug("Teleported", this.getName(subject) , "to", this.getName(dest),
                   "- Distance:", subject.getLocation().distance(dest.getLocation()));
        }

        // Apply the hunger penalty to the subject.
        if (applyPenalty)
            applyTeleportPenalty(subject, dest.getLocation(), tpingToAnotherWorld);

        // Teleports the subject player to the destination player.
        subject.teleport(dest, PlayerTeleportEvent.TeleportCause.COMMAND);
    }

    /**
     * Teleports the given player to the given location
     *
     * @param subject The player which gets teleported.
     * @param destination The location where the 'subject' player is teleported to.
     */
    public void teleport(Player subject, Location dest, boolean applyPenalty) {
        // Check if both players are valid
        if (!this.isValid(subject)) {
            return;
        }

        World subWrld = subject.getWorld(), destWrld = dest.getWorld();
        boolean crossWorldTpEnabled = conU.getBoolean("world.cross_world_teleportation", true);
        boolean tpingToAnotherWorld = !subWrld.equals(destWrld);

        // Check the world of the subjects
        if (!crossWorldTpEnabled && tpingToAnotherWorld) {
            // Print out debug information.
            warnDebug("Could not teleport", this.getName(subject), "to their previous",
                             "location because cross world teleportation is disabled!");
            
            msgU.userMessage(subject, "error_cross_world_disabled_self");
            return;
        } if (!tpingToAnotherWorld) {
            // Calculate and print out the distance
            printDebug("Teleported", this.getName(subject) , "to", dest.x(), dest.y(), dest.z(),
                       "(previous location) - Distance:", subject.getLocation().distance(dest));
        }

        // Apply the hunger penalty to the subject.
        if (applyPenalty)
            applyTeleportPenalty(subject, dest, tpingToAnotherWorld);

        // Teleports the subject player to the destination location.
        subject.teleport(dest, PlayerTeleportEvent.TeleportCause.COMMAND);
    }

    /**
     * Plays a preset sound, either client-side or globally, based on config.yml entries
     *
     * @param plr The target player.
     * @param path The sound.key's element, e.g., "timedout".
     */
    public void playPresetSound(Player plr, String name) {
        // Check if the player is valid
        if (!this.isValid(plr)) {
            return;
        }

        // Get the sound name from config
        String soundKeyId = conU.getString("sounds.keys." + name, "").toLowerCase();
        if (soundKeyId.isEmpty()) {
            return;
        }

        try {
            // Determine if the sound is global and get the sound key
            boolean global = conU.getBoolean("sounds.played_globally." + name, false);

            NamespacedKey soundKey = NamespacedKey.minecraft(soundKeyId);
            var soundObject = Registry.SOUNDS.getOrThrow(soundKey);

            float volume = (float) conU.getDouble("sounds.volume." + name, 1.0, 0.0, 1.0);

            if (global) { // Play the sounds
                plr.getWorld().playSound(plr.getLocation(),
                        soundObject, SoundCategory.PLAYERS, volume, 1f);
            } else {
                plr.playSound(plr.getLocation(),
                        soundObject, SoundCategory.MASTER, volume, 1f);
            }

        } catch (Exception e) {
            // Report to the console about the missing sound.
            output.warn("playPresetSound(): The requested sound ",
                    name, " (", soundKeyId, ") is not available.");
        }
    }
}
