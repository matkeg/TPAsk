package dev.matkeg.tpask.Managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import dev.matkeg.tpask.Managers.LanguageManager;
import dev.matkeg.tpask.Utils.*;
import dev.matkeg.tpask.TPAsk;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;

/* --------------------------- MAIN --------------------------- */
public class StateManager {
    // Modules
    private final TPAsk plugin;
    private final Output output;
    private final PlayerUtils plrU;
    private final FileConfiguration config;
    private final LanguageManager langMan;

    // Variables
    private PotionEffect Freeze;
    
    // Class Constructor
    public StateManager(TPAsk plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.output = plugin.getOutput();
        this.plrU = plugin.getPlayerUtils();
        this.langMan = plugin.getLanguageManager();
        
        this.Freeze = new PotionEffect(
            PotionEffectType.SLOWNESS,
            config.getInt("tpa.delay") * 20,
            255, true, true, false
        );
    }
    
    // receiver -> requester
    private final Map<UUID, UUID> incoming = new HashMap<>();
    // requester -> receiver
    private final Map<UUID, UUID> outgoing = new HashMap<>();
    // receiver -> timeout task
    private final Map<UUID, BukkitTask> timeouts = new HashMap<>();
    // requester cooldown (ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
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
    
    /* --------------------- REQUEST HANDLERS --------------------- */
    
    /**
     * Creates a teleportation request between two players.
     * 
     * @param requester The requesting player.
     * @param receiver The receiving player.
     * <br><br><i>
     * A player who receives the request is refereed to as 
     * a "receiver", they are the one who the "requester" 
     * will be potentially teleported to.</i> 
     */
    public synchronized void createRequest(Player requester, Player receiver) {
        // Get the UUIDs of the two players
        UUID requestUUID = requester.getUniqueId(), 
              receiveUUID = receiver.getUniqueId();
        
        // Map the incoming and outgoing requests
        incoming.put(receiveUUID, requestUUID); outgoing.put(requestUUID, receiveUUID);

        // Store the cooldown.
        cooldowns.put(
                requestUUID, 
                System.currentTimeMillis() + 
                config.getInt("tpa.cooldown", 60) * 1000L);

        // Store the timout task
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(
                plugin, () -> expire(receiveUUID),
                config.getInt("tpa.timeout", 20) * 20L);
        
        // Store the timeoutTask for later removal
        timeouts.put(receiveUUID, timeoutTask);
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
            plrU.sendPresetMessage(Bukkit.getPlayer(receiverId),
            "messages.no_active_requests", null); 
        return; }
        outgoing.remove(requesterId);
        
        // Remove the bukkit expire task
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
        if (plrU.isValid(receiver)) {
            plrU.playPresetSound(receiver, "accepted"); 
            plrU.sendPresetMessage(receiver, 
                    "messages.request_accepted_receiver", 
                    Map.of("%requester%", requesterName));
        }
        
        if (plrU.isValid(requester)) {
            plrU.playPresetSound(requester, "accepted"); 
            plrU.sendPresetMessage(requester, 
                    "messages.request_accepted_requester", 
                    Map.of("%receiver%", receiverName));
        }
        
        // Add a temp freeze effect while teleporting
        if (config.getBoolean("tpa.freeze", true)) requester.addPotionEffect(Freeze);
        
        // Get the requester's starting position
        Location startLoc = requester.getLocation();

        // Setup the task which teleports the requester to the receiver
        Bukkit.getScheduler().runTaskLater(plugin, () -> { 
            // MAIN TELEPORTATION TASK
            if (plrU.isValid(requester) && plrU.isValid(receiver)) { 
                if (config.getBoolean("tpa.must_stand_still", false)) {
                    // We'll give a 1 block leeway
                    if (startLoc.distance(requester.getLocation()) < 1.0) {
                        plrU.teleport(requester, receiver);   
                    } else {
                        plrU.playPresetSound(receiver, "denied"); 
                        plrU.sendPresetMessage(receiver, 
                            "messages.request_error_moved_receiver",
                              Map.of("%requester%", requesterName));
                        
                        plrU.playPresetSound(requester, "denied"); 
                        plrU.sendPresetMessage(requester, 
                            "messages.request_error_moved_requester", null);      
                    }
                } else {
                   plrU.teleport(requester, receiver);  
                }
            } else {
                if (plrU.isValid(receiver)) {
                    plrU.playPresetSound(receiver, "denied"); 
                    plrU.sendPresetMessage(receiver, 
                            "messages.request_error_disconnect_receiver",
                            Map.of("%requester%", requesterName));
                }
                if (plrU.isValid(requester)) { 
                    plrU.playPresetSound(requester, "denied"); 
                    plrU.sendPresetMessage(requester, 
                            "messages.request_error_disconnect_requester",
                            Map.of("%receiver%", receiverName));
                }
            }
        }, config.getInt("tpa.delay", 1) * 20L);
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
            plrU.sendPresetMessage(Bukkit.getPlayer(receiverId),
                           "messages.no_active_requests", null); 
        return; }
        outgoing.remove(requesterId);
        
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
        if (plrU.isValid(receiver)) {
            plrU.playPresetSound(receiver, "denied"); 
            plrU.sendPresetMessage(receiver, 
                    "messages.request_denied_receiver", 
                    Map.of("%requester%", requesterName));
        }
        
        if (plrU.isValid(requester)) {
            plrU.playPresetSound(requester, "denied"); 
            plrU.sendPresetMessage(requester, 
                    "messages.request_denied_requester", 
                    Map.of("%receiver%", receiverName));
        }
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
            plrU.sendPresetMessage(Bukkit.getPlayer(requesterId),
                            "messages.no_active_requests", null); 
        return; }
        incoming.remove(receiverId);
        
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
        if (plrU.isValid(receiver)) {
            plrU.playPresetSound(receiver, "canceled"); 
            plrU.sendPresetMessage(receiver, 
                    "messages.request_canceled_receiver", 
                    Map.of("%requester%", requesterName));
        }    
        
        if (plrU.isValid(requester)) {
            plrU.playPresetSound(requester, "canceled"); 
            plrU.sendPresetMessage(requester, 
                    "messages.request_canceled_requester", 
                    Map.of("%receiver%", receiverName));
        }
    }

    public synchronized void cancelAll() {
        for (var t : timeouts.values()) if (t != null) t.cancel();
        incoming.clear(); outgoing.clear(); timeouts.clear(); cooldowns.clear();
    }
    
    /* -------------------- INTERNAL / PRIVATE -------------------- */
    
    private synchronized void expire(UUID receiverId) {
        // Check if the reciever has an active request
        if (!incoming.containsKey(receiverId)) return;
        
        // Remove the request between the reciever and requester
        // Additionally, we will cancel the timeoutTask
        UUID requesterId = incoming.remove(receiverId);
        outgoing.remove(requesterId);

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
        if (plrU.isValid(receiver)) {
            plrU.playPresetSound(receiver, "timedout");
            plrU.sendPresetMessage(receiver, 
                    "messages.request_expired_receiver", 
                    Map.of("%requester%", requesterName));
        }
        
        if (plrU.isValid(requester)) {
            plrU.playPresetSound(receiver, "timedout");
            plrU.sendPresetMessage(requester, 
                    "messages.request_expired_requester", 
                    Map.of("%receiver%", receiverName));
        }
    }
    
}
