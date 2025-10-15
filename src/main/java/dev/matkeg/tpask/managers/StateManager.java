package dev.matkeg.tpask.managers;

import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Bukkit;

import dev.matkeg.tpask.managers.LanguageManager;
import dev.matkeg.tpask.utilities.*;
import dev.matkeg.tpask.PluginMain;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;

/* ---------------------- MAIN CLASS ---------------------- */
public class StateManager {
    // Modules
    private final PlayerUtils plrU;
    private final ConfigUtils conU;
    private final PluginMain plugin;
    private final MessageUtils msgU;
    private final LanguageManager langMan;

    // Limits and defaults
    private int timeDef = 20, timeMin = 5, timeMax = 180;
    private int delayDef = 1, delayMin = 0, delayMax = 60;
    private int rememberDef = 60, rememberMin = 5, rememberMax = 900;
    private int cooldownDef = 60, cooldownMin = 0, cooldownMax = 900;
    private double leewayDef = 1.0, leewayMin = 0.8, leewayMax = 128.0;
    
    // Constructor
    public StateManager(PluginMain plugin) {
        this.plugin = plugin;
        this.conU = plugin.getConfigUtils();
        this.plrU = plugin.getPlayerUtils();
        this.msgU = plugin.getMessageUtils();
        this.langMan = plugin.getLanguageManager();
    }

    // Enums
    public enum RequestType { TPA, TPAHERE }

    // Storage
    
    // receiver -> requester
    private final Map<UUID, UUID> incoming = new HashMap<>();
    // requester -> receiver
    private final Map<UUID, UUID> outgoing = new HashMap<>();
    // requester -> cooldown (ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // receiver -> timeout task
    private final Map<UUID, BukkitTask> timeouts = new HashMap<>();
    // receiver -> request type
    private final Map<UUID, RequestType> requestTypes = new HashMap<>();
    // player -> previous Location
    private final Map<UUID, Location> previousLocations = new HashMap<>();
    // player -> forget location task
    private final Map<UUID, BukkitTask> previousLocationTasks = new HashMap<>();
    
    /* ---------------------- APIs ------------------------ */
    
   /**
     * Checks if the given player has an incoming TPA request.
     * @param receiver The UUID of the player receiving the request.
     * @return <b>True</b> if there is an incoming request and vice versa.
     */
    public synchronized boolean hasIncoming(UUID receiver) {
        return incoming.containsKey(receiver);
    }

    /**
     * Checks if the given player has an outgoing TPA request.
     * @param requester The UUID of the requesting player.
     * @return <b>True</b> if there is an outgoing request and vice versa.
     */
    public synchronized boolean hasOutgoing(UUID requester) {
        return outgoing.containsKey(requester);
    }
    
    /**
     * Checks if the given player has a known last location prior
     * to their last TP. NOTE: Last locations can be forgotten.
     * 
     * @param requester The UUID of the requesting player.
     * @return <b>True</b> if there is a known last location and vice versa.
     */
    public synchronized boolean hasLastKnownLocation(UUID playerId) {
        return previousLocations.containsKey(playerId);
    }
    
    /**
     * Checks if the given player is currently on a request cooldown.
     * @param requester The UUID of the player.
     * @return <b>True</b> if the player is on cooldown.
     */
    public synchronized boolean onCooldown(UUID requester) {
        Long until = cooldowns.get(requester);
        if (until == null) return false;
        return System.currentTimeMillis() < until;
    }

    /**
     * Returns the remaining cooldown time (in seconds) for the given player.
     * @param requester The UUID of the player.
     * @return Remaining cooldown time in seconds.<br> 
     * <i>0 if no cooldown is active.</i>
     */
    public synchronized long cooldownRemaining(UUID requester) {
        Long until = cooldowns.get(requester);
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000L : 0;
    }

    /* ----------------- REQUEST HANDLERS ----------------- */
    
    /**
     * Creates a TPA teleportation request between two players.
     * 
     * @param requester The requesting player.
     * @param receiver The receiving player.
     * <br><br><i>
     * A player who receives the request is refereed to as 
     * a "receiver", they are the one who the "requester" 
     * will be potentially teleported to.</i> 
     */
    public synchronized void createTpaRequest(Player requester, Player receiver) {
        // Get the UUIDs of the two players
        UUID reqUUID = requester.getUniqueId(), 
              recUUID = receiver.getUniqueId();

        // Map the incoming and outgoing requests
        incoming.put(recUUID, reqUUID); outgoing.put(reqUUID, recUUID);
        requestTypes.put(recUUID, RequestType.TPA);

        // Store the cooldown.
        cooldowns.put(reqUUID, System.currentTimeMillis() +
                conU.getInt("tpa.cooldown", cooldownDef, cooldownMin, cooldownMax) * 1000L);

        // Store the timout task
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(
                plugin, () -> expire(recUUID),
                conU.getInt("tpa.timeout", timeDef, timeMin, timeMax) * 20L);
        
        // Store the timeoutTask for later removal        
        timeouts.put(recUUID, timeoutTask);
    }
    
    /**
     * Creates a TPA here teleportation request between two players.
     * 
     * @param requester The requesting player.
     * @param receiver The receiving player.
     * <br><br><i>
     * A player who receives the request is refereed to as 
     * a "receiver", they are the one who will be potentially 
     * teleported.</i> 
     */
    public synchronized void createTpaHereRequest(Player requester, Player receiver) {
        // Get the UUIDs of the two players
        UUID reqUUID = requester.getUniqueId(), 
              recUUID = receiver.getUniqueId();
        
        // Map the incoming and outgoing requests
        incoming.put(recUUID, reqUUID); outgoing.put(reqUUID, recUUID);
        requestTypes.put(recUUID, RequestType.TPAHERE);

        // Store the cooldown.
        cooldowns.put(reqUUID, System.currentTimeMillis() +
                conU.getInt("tpa.cooldown", cooldownDef, cooldownMin, cooldownMax) * 1000L);

        // Store the timout task
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(
                plugin, () -> expire(recUUID),
                conU.getInt("tpa.timeout", timeDef, timeMin, timeMax) * 20L);

        // Store the timeoutTask for later removal
        timeouts.put(recUUID, timeoutTask);
    }
    
    /**
     * Accepts the receiver's active TPA request.
     * 
     * @param receiverId The UUID of the receiving player.
     * <br><br><i>
     * A player who receives the request is refereed to as 
     * a "receiver", they are the one who the "requester" 
     * will be potentially teleported to.</i> 
     */
    public synchronized void accept(UUID receiverId) {
        // Get the requester's UUID and remove
        // the incoming and outgoing entries
        UUID requesterId = incoming.remove(receiverId);
        if (requesterId == null) {
            Player plr = Bukkit.getPlayer(receiverId);
            plrU.playPresetSound(plr, "error");
            msgU.userMessage(plr, "no_active");
            return;
        }
        outgoing.remove(requesterId);
        // Remove request type mapping for this receiver
        RequestType type = requestTypes.remove(receiverId);
        
        // Remove the bukkit expire task
        BukkitTask t = timeouts.remove(receiverId);
        if (t != null) t.cancel();

        // Get the request's players' data
        Player requester = Bukkit.getPlayer(requesterId);
        Player receiver = Bukkit.getPlayer(receiverId);
        
        // Use null-safe name retrieval (fallback to localized "Player")
        String requesterName = plrU.getName(requester, 
            langMan.getLocalizedString("player_noun", "Player"));
        String receiverName = plrU.getName(receiver, 
            langMan.getLocalizedString("player_noun", "Player"));
        
        // Issue out messages and sound effects to the requester and reciever
        playSoundToPlayers(receiver, "accepted", requester, "accepted");
        msgU.userMessage(receiver, "accepted", "%OTHER%", requesterName); 
        msgU.userMessage(requester, "accepted_self", "%OTHER%", receiverName);

        // Determine who should be affected by freeze
        Player affected = (type == RequestType.TPAHERE) ? receiver : requester;
        
        // Add a temp freeze effect while teleporting (if configured to do so)
        if (conU.getBoolean("tpa.freeze", true) && plrU.isValid(affected)) {
            affected.addPotionEffect( new PotionEffect(
                PotionEffectType.SLOWNESS,
                conU.getInt("tpa.delay", delayDef, delayMin, delayMax) * 20,
                255, true, true, false
            ));
        
            affected.addPotionEffect( new PotionEffect(
                PotionEffectType.MINING_FATIGUE,
                conU.getInt("tpa.delay", delayDef, delayMin, delayMax) * 20,
                255, true, true, false
            ));
        }
        
        // Get the requester's starting position
        Location requesterStartLoc = requester != null ? requester.getLocation() : null;
        Location receiverStartLoc = receiver != null ? receiver.getLocation() : null;
        
        // Setup the task which teleports the requester to the receiver
        Bukkit.getScheduler().runTaskLater(plugin,
                // This is kinda messy, but we NEED to know
                // data on the two player's in this request.
                () -> {
                    if (type == RequestType.TPAHERE) {
                        teleportAcceptTask(
                                requester, receiverStartLoc, 
                                receiver, receiverName, 
                                requester, requesterName
                        );
                    } else /* TPA */ {
                        teleportAcceptTask(
                                requester, requesterStartLoc,  
                                requester, requesterName, 
                                receiver, receiverName
                        );
                    }
                }, conU.getInt("tpa.delay", delayDef, delayMin, delayMax) * 20L);
    }
        
    /**
     * Denies the receiver's active TPA request.
     * 
     * @param receiverId The UUID of the receiving player.
     * <br><br><i>
     * A player who receives the request is refereed to as 
     * a "receiver", they are the one who the "requester" 
     * will be potentially teleported to.</i> 
     */
    public synchronized void deny(UUID receiverId) {
        // Get the requester's UUID and remove
        // the incoming and outgoing entries
        UUID requesterId = incoming.remove(receiverId);
        if (requesterId == null) {
            Player plr = Bukkit.getPlayer(receiverId);
            plrU.playPresetSound(plr, "error");
            msgU.userMessage(plr, "no_active");
            return;
        }
        
        outgoing.remove(requesterId);
        // Remove request type mapping
        requestTypes.remove(receiverId);
        
        // Remove the bukkit task;
        BukkitTask t = timeouts.remove(receiverId);
        if (t != null) t.cancel();
        
        // Get the request's players' data
        Player requester = Bukkit.getPlayer(requesterId);
        Player receiver = Bukkit.getPlayer(receiverId);

        String requesterName = plrU.getName(requester, 
            langMan.getLocalizedString("player_noun", "Player"));
        String receiverName = plrU.getName(receiver, 
            langMan.getLocalizedString("player_noun", "Player"));
        
        // Issue out messages and sound effects to the requester and reciever
        playSoundToPlayers(receiver, "denied", requester, "denied");
        msgU.userMessage(receiver, "denied", "%OTHER%", requesterName);
        msgU.userMessage(requester, "denied_self", "%OTHER%", receiverName);
    }

    /**
     * Cancels the requester's outgoing TPA request.
     * 
     * @param requesterId The UUID of the requesting player.
     * <br><br><i>
     * A player who requires the request is refereed to as 
     * a "requester", they are the one who is going to
     * teleport to the "receiver".</i> 
     */
    public synchronized void cancel(UUID requesterId) {
        // Get the receiver's UUID and remove
        // the incoming and outgoing entries
        UUID receiverId = outgoing.remove(requesterId);
        if (receiverId == null) {
            Player plr = Bukkit.getPlayer(requesterId);
            plrU.playPresetSound(plr, "error");
            msgU.userMessage(plr, "no_active");
            return;
        }
        
        incoming.remove(receiverId);
        // Remove request type mapping
        requestTypes.remove(receiverId);
        
        // Remove the bukkit task;
        BukkitTask t = timeouts.remove(receiverId);
        if (t != null) t.cancel();

        // Get the request's players' data
        Player requester = Bukkit.getPlayer(requesterId);
        Player receiver = Bukkit.getPlayer(receiverId);

        String requesterName = plrU.getName(requester, 
            langMan.getLocalizedString("player_noun", "Player"));
        String receiverName = plrU.getName(receiver, 
            langMan.getLocalizedString("player_noun", "Player"));
        
        // Issue out messages and sound effects to the requester and reciever
        playSoundToPlayers(receiver, "canceled", requester, "canceled");
        msgU.userMessage(receiver, "canceled", "%OTHER%", requesterName); 
        msgU.userMessage(requester, "canceled_self", "%OTHER%", receiverName);
    }

    public synchronized void cancelAll() {
        for (var t : timeouts.values()) if (t != null) t.cancel();
        incoming.clear(); outgoing.clear(); timeouts.clear(); cooldowns.clear();
        // Clear request types as well
        requestTypes.clear();
        
        // Cancel and clear previous location tasks and storage
        for (var t : previousLocationTasks.values()) if (t != null) t.cancel();
        previousLocationTasks.clear(); previousLocations.clear();
    }

    /**
    * Attempts to return the given player to their
    * previous location (if it exists in memory)
    *
    * @param player The requesting player.
    */
   public synchronized void back(Player player) {
       if (!plrU.isValid(player)) return;
       
       // Check if the back command is enabled.
       if (!conU.getBoolean("back.enabled", true)) {
            if (player.isOp()) {
                msgU.chatMessage(player, "disabled_cmd_bypassed", 
                                             "%COMMAND%", "back");
                // Don't return, as we will continue on...
            } else {
                plrU.playPresetSound(player, "error");
                msgU.userMessage(player, "cmd_disabled",
                               "%COMMAND%", "back");
            return; }
       }

       // Check for a previous location
       UUID plrId = player.getUniqueId();
       Location prevLoc = previousLocations.get(plrId);
       if (prevLoc == null) {
           Integer rememberFor = conU.getInt("back.available_for", 
                        rememberDef, rememberMin, rememberMax);
           msgU.userMessage(player, "no_previous_location", 
                       "%SECONDS%", rememberFor.toString());
       return; }


       // Get the appropriate values
       boolean useTpaValues = conU.getBoolean("back.use_tpa_values", true);
       boolean hungerPenalty = conU.getBoolean("hunger_penalty.applied_on_return", true);

       int delaySeconds;
       boolean shouldFreeze;
       boolean mustStandStill;
       double movementLeeway;  

       if (useTpaValues) {
           shouldFreeze = conU.getBoolean("tpa.freeze", true);
           mustStandStill = conU.getBoolean("tpa.must_stand_still", true);
           delaySeconds = conU.getInt("tpa.delay", delayDef, delayMin, delayMax);
           movementLeeway = conU.getDouble("tpa.movement_leeway", leewayDef, leewayMin, leewayMax); 
       } else {
           shouldFreeze = conU.getBoolean("back.freeze", true);
           mustStandStill = conU.getBoolean("back.must_stand_still", true);
           delaySeconds = conU.getInt("back.delay", delayDef, delayMin, delayMax);
           movementLeeway = conU.getDouble("back.movement_leeway", leewayDef, leewayMin, leewayMax);
       }

       // capture location at time of scheduling (if needed)
       Location startLoc = player.getLocation().clone();

       msgU.userMessage(player, "back_to_previous_location");
       plrU.playPresetSound(player, "accepted");

       // Add a temp freeze effect while teleporting (if configured to do so)
       if (shouldFreeze && plrU.isValid(player)) {
            player.addPotionEffect( new PotionEffect(
                PotionEffectType.SLOWNESS,
                delaySeconds * 20,
                255, true, true, false
            ));
        
            player.addPotionEffect( new PotionEffect(
                PotionEffectType.MINING_FATIGUE,
                delaySeconds * 20,
                255, true, true, false
            ));
       }

       Bukkit.getScheduler().runTaskLater(plugin, () -> {
           // Player disconnected?
           if (!plrU.isValid(player)) return;

           if (mustStandStill) {
               if (startLoc.distance(player.getLocation()) > movementLeeway) {
                   plrU.playPresetSound(player, "denied");
                   msgU.userMessage(player, "error_moved_self");
                   return;
               }
           }

           // Clear stored location before teleport to prevent re-use
           clearPreviousLocation(plrId);

           // Teleport the player back to the stored previous location.
           plrU.teleport(player, prevLoc, hungerPenalty);

       }, delaySeconds * 20L);

       return;
   }

    
    /* --------------------- FUNCTIONS -------------------- */
    
    private synchronized void teleportAcceptTask(
        Player requester, Location startLoc,
        Player subject, String subName, 
        Player target,  String tarName
    ) { // -------------------------------------------------------
        // MAIN TELEPORTATION TASK
        if (plrU.areValid(subject, target)) {
            boolean penaltyEnabled = conU.getBoolean("hunger_penalty.enabled", true);
            boolean mustStandStill = conU.getBoolean("tpa.must_stand_still", true);
            double movementLeeway = conU.getDouble("tpa.movement_leeway", leewayDef, leewayMin, leewayMax);
            
            if (mustStandStill) {
                if (startLoc != null && startLoc.distance(subject.getLocation()) <= movementLeeway) {
                    rememberPreviousLocation(subject, startLoc);
                    plrU.teleport(subject, target, penaltyEnabled);
                } else {
                    playSoundToPlayers(subject, "denied", target, "denied");
                    msgU.userMessage(target, "error_moved", "%OTHER%", subName);
                    msgU.userMessage(subject, "error_moved_self");
                }
            } else { // We can move, just need an accepted request;
                rememberPreviousLocation(subject, startLoc);
                plrU.teleport(subject, target, penaltyEnabled);
            }
        } else { // One of the players is invalid
            playSoundToPlayers(subject, "denied", target, "denied");
            msgU.userMessage(target, "error_disconnect", "%OTHER%", subName);
            msgU.userMessage(subject, "error_disconnect_self", "%OTHER%", tarName);
        }
    }
    
    private synchronized void expire(UUID receiverId) {
        // Check if the reciever has an active request
        if (!incoming.containsKey(receiverId)) return;
        
        // Remove the request between the reciever and requester
        // Additionally, we will cancel the timeoutTask
        UUID requesterId = incoming.remove(receiverId);
        outgoing.remove(requesterId);

        // Remove request type mapping
        requestTypes.remove(receiverId);

        BukkitTask timeoutTask = timeouts.remove(receiverId);
        if (timeoutTask != null) timeoutTask.cancel();

        // Get the request's players' data
        Player requester = Bukkit.getPlayer(requesterId);
        Player receiver = Bukkit.getPlayer(receiverId);
        
        String requesterName = plrU.getName(requester, 
            langMan.getLocalizedString("player_noun", "Player"));
        String receiverName = plrU.getName(receiver, 
            langMan.getLocalizedString("player_noun", "Player"));
        
        // Issue out messages and sound effects to the requester and reciever
        playSoundToPlayers(receiver, "timedout", requester, "timedout");
        msgU.userMessage(receiver, "expired", "%OTHER%", requesterName); 
        msgU.userMessage(requester, "expired_self", "%OTHER%", receiverName); 
    }
     
    private synchronized void setPreviousLocation(UUID playerId, Location loc) {
        if (loc == null) return;

        int rememberFor = conU.getInt("back.available_for", rememberDef, rememberMin, rememberMax);
        if (rememberFor <= 0) return;

        // Cancel existing forget task if present
        BukkitTask existing = previousLocationTasks.remove(playerId);
        if (existing != null) existing.cancel();

        // Create and store the forget task
        previousLocations.put(playerId, loc);
        BukkitTask forgetTask = Bukkit.getScheduler().runTaskLater(
            plugin, () -> clearPreviousLocation(playerId),
            rememberFor * 20L
        );
        previousLocationTasks.put(playerId, forgetTask);
    }
    
    private synchronized void rememberPreviousLocation(Player subject, Location startLoc) {
        if (!plrU.isValid(subject)) return;
        
        // Check whether the location can be remembered
        boolean backEnabled = conU.getBoolean("back.enabled", true);
        boolean subjectIsOp = subject.isOp();
        
        if (backEnabled || subjectIsOp) {
            if (startLoc != null) {
                setPreviousLocation(subject.getUniqueId(), startLoc);
            }
        }
    }
    
    public synchronized void clearPreviousLocation(UUID playerId) {
        previousLocations.remove(playerId);
        BukkitTask t = previousLocationTasks.remove(playerId);
        if (t != null) t.cancel();
    }
    
    private void playSoundToPlayers(Player receiver, String recSound, Player requester, String reqSound) {
        if (plrU.isValid(receiver)) plrU.playPresetSound(receiver, recSound);
        if (plrU.isValid(requester)) plrU.playPresetSound(requester, reqSound);
    } 
}
