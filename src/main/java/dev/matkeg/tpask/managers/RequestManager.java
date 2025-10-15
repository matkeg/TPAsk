package dev.matkeg.tpask.managers;

import net.kyori.adventure.text.Component;

import dev.matkeg.tpask.managers.StateManager;
import dev.matkeg.tpask.utilities.*;
import dev.matkeg.tpask.PluginMain;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.UUID;

/* ---------------------- MAIN CLASS ---------------------- */
public class RequestManager {
    // Modules
    private final PluginMain plugin;
    private final PlayerUtils plrU;
    private final ConfigUtils conU;
    private final MessageUtils msgU;
    private final OutputUtils output;
    private final StateManager statMan;
    
    // Constructor
    public RequestManager(PluginMain plugin) {
        this.plugin = plugin;
        this.output = plugin.getOutput();
        this.msgU = plugin.getMessageUtils();
        this.conU = plugin.getConfigUtils();
        this.plrU = plugin.getPlayerUtils();
        this.statMan = plugin.getStateManager();
    }

    /* --------------------- FUNCTIONS -------------------- */
        
    private void warnDebug(Object... args) {
        boolean requestDebugEnabled = conU.getBoolean("debug.requests", false);
         if (requestDebugEnabled) output.warn(args);
    }
    
    private void printDebug(Object... args) {
        boolean requestDebugEnabled = conU.getBoolean("debug.requests", false);
         if (requestDebugEnabled) output.print(args);
    }
    
    private boolean canRequestInitiate(Player requester, Player other, String otherInput) {
        // Check if both players are valid and online
        if (!plrU.areValid(requester, other)) {
            msgU.userMessage(requester, "not_online", "%OTHER%", otherInput);
            plrU.playPresetSound(requester, "error");
            return false;
        }

        // Check if both players aren't the same person.
        if (requester.equals(other)) {
            msgU.userMessage(requester, "request_yourself");
            plrU.playPresetSound(requester, "error");
            return false;
        }
        
        String otherName = plrU.getName(other);
        UUID requesterUUID = requester.getUniqueId();
        UUID otherUUID = other.getUniqueId();

        // Check whether the requester can send an request
        if (statMan.hasOutgoing(requesterUUID)) {
            msgU.userMessage(requester, "self_outgoing_busy");
            plrU.playPresetSound(requester, "error");
            return false;
        }

        if (statMan.hasIncoming(requesterUUID)) {
            msgU.userMessage(requester, "self_incoming_busy");
            plrU.playPresetSound(requester, "error");
            return false;
        }

        if (statMan.onCooldown(requesterUUID)) {
            Long remaining = statMan.cooldownRemaining(requesterUUID);
            msgU.userMessage(requester, "cooldown", "%SECONDS%", remaining.toString());
            plrU.playPresetSound(requester, "error");
            return false;
        }

        // Check if the reciever can recieve the request
        if (statMan.hasIncoming(otherUUID) || statMan.hasOutgoing(otherUUID)) {
            msgU.userMessage(requester, "other_is_busy", "%OTHER%", otherName);
            plrU.playPresetSound(requester, "error");
            return false;
        }

        return true;
    }

    private void sendMessagesAndSounds(Player requester, Player other, 
            String msgSent, String msgRecieved, String soundSent, String soundRecieved
    ) { // --------------------------------------------------------
        // Get info about the players.
        String requesterName = plrU.getName(requester);
        String otherName = plrU.getName(other);

        // Build the messages: RECEIVER CLICKABLE
        Component accept = msgU.clickableFromConfig("accept", "%OTHER%", requesterName);
        Component deny = msgU.clickableFromConfig("deny", "%OTHER%", requesterName);

        // Build the messages: REQUESTER CLICKABLE
        Component cancel = msgU.clickableFromConfig("cancel", "%OTHER%", otherName);

        // Send the message: RECEIVER
        Component acceptAndDeny = Component.empty();
        if (accept != null) acceptAndDeny = accept;
        if (deny != null) acceptAndDeny = acceptAndDeny.append(Component.text(" ")).append(deny);
        msgU.chatMessage(other, msgRecieved, "%OTHER%", requesterName, acceptAndDeny);

        // Send the message: REQUESTER
        Component cancelComp = cancel != null ? cancel : Component.empty();
        msgU.chatMessage(requester, msgSent, "%OTHER%", otherName, cancelComp);

        // Play sounds for both players (message/sound helpers already validate)
        plrU.playPresetSound(other, soundRecieved);
        plrU.playPresetSound(requester, soundSent);
    }
    
    private boolean handleTPA(Player requester, Player target, String input) {
        // Null-check target early and notify requester if offline
        if (target == null || !plrU.isValid(target)) {
            msgU.userMessage(requester, "not_online", "%OTHER%", input);
            plrU.playPresetSound(requester, "error");
            return true;
        }

        // Check whether we can intiate the request.
        if (!canRequestInitiate(requester, target, input)) return true;

        // Create the request.
        statMan.createTpaRequest(requester, target);

        // Send the appropriate messages and sounds to the players.
        sendMessagesAndSounds(requester, target, "sent", "received", "sent", "received");
        
        // Send a debug message
        printDebug(plrU.getName(requester), "sent a TPA request to", plrU.getName(target));
        
        return true;
    }

    private boolean handleTPAHere(Player requester, Player subject, String input, boolean byCommand) {
        boolean tpaHereEnabled = conU.getBoolean("tpahere.enabled", true);
        // Allow permission node bypass in addition to op status
        boolean bypassRestrictions = requester.isOp();

        // Check if the TPAHere is disabled
        if (byCommand && !tpaHereEnabled) {
            if (bypassRestrictions) {
                msgU.chatMessage(requester, "disabled_cmd_bypassed", 
                                             "%COMMAND%", "tpahere");
                // Don't return, as we will continue on...
            } else {
                msgU.userMessage(requester, "cmd_disabled", "%COMMAND%", "tpahere");
                plrU.playPresetSound(requester, "error");
                return true;
            }
        }

        // Null-check subject early and notify requester if offline
        if (subject == null || !plrU.isValid(subject)) {
            msgU.userMessage(requester, "not_online", "%OTHER%", input);
            plrU.playPresetSound(requester, "error");
            return true;
        }

        // Finally, check whether we can request and send the message/s
        if (!canRequestInitiate(requester, subject, input)) return true;

        // Create the request.
        statMan.createTpaHereRequest(requester, subject);

        // Send the appropriate messages and sounds to the players.
        sendMessagesAndSounds(requester, subject, 
                    "here_sent", "here_received", 
                              "sent", "received");

        // Send a debug message
        printDebug(plrU.getName(requester), 
               "sent a TPA Here request to", 
                     plrU.getName(subject));
        
        return true;
    }

    private boolean strictCheck(Player req, String cmd, String[] args) {
        Player receiver = Bukkit.getPlayer(args[0]);
        // Use equalsIgnoreCase and null-check
        if ("tpahere".equalsIgnoreCase(cmd)) {
            if (receiver == null || !plrU.isValid(receiver)) {
                msgU.userMessage(req, "not_online", "%OTHER%", args[0]);
                plrU.playPresetSound(req, "error");
                return true;
            }
            return handleTPAHere(req, receiver, args[0], true);
        } else {
            if (receiver == null || !plrU.isValid(receiver)) {
                msgU.userMessage(req, "not_online", "%OTHER%", args[0]);
                plrU.playPresetSound(req, "error");
                return true;
            }
            return handleTPA(req, receiver, args[0]);
        }
    }

    /* ----------------------- APIs ----------------------- */
    
    /** 
     * The starting point of all the checks related to whether a request can be sent and
     * to displaying all appropriate notifications and similar messages.
     *
     * @param requester The requesting Player
     * @param cmd The string containing the invoked command's name
     * @param args Should represent at least one, or at max two players.
     */
    public boolean handleRequestChecks(Player requester, String cmd, String[] args) {
        // Check the amount of arguments being passed
        if (args.length < 1 || args.length > 2) {
            msgU.userMessage(requester, "usage_help_player_arg", "%COMMAND%", cmd);
            return true;
        }

        Player firstPlr = Bukkit.getPlayer(args[0]);
        // Null-check early and respond to the requester immediately
        if (firstPlr == null || !plrU.isValid(firstPlr)) {
            msgU.userMessage(requester, "not_online", "%OTHER%", args[0]);
            plrU.playPresetSound(requester, "error");
            return true;
        }

        if ("tpahere".equalsIgnoreCase(cmd)) return handleTPAHere(requester, firstPlr, args[0], true);
        
        // Get all the needed variables.
        boolean contextEnabled = conU.getBoolean("tpahere.tpa_context_recognition", true);
        
        if (contextEnabled && args.length >= 2) {
            Player secondPlr = Bukkit.getPlayer(args[1]);

            // Null-check second player and report if offline (caller probably meant them)
            if (secondPlr == null || !plrU.isValid(secondPlr)) {
                msgU.userMessage(requester, "not_online", "%OTHER%", args[1]);
                plrU.playPresetSound(requester, "error");
                return true;
            }
            
            if (requester.equals(firstPlr)) {
                return handleTPA(requester, secondPlr, args[1]);

            } else if (requester.equals(secondPlr)) {
                return handleTPAHere(requester, firstPlr, args[0], false);

            } else { // Attempted to request for somebody else.
                warnDebug(plrU.getName(requester), "attempted to send a TPA request for somebody else.");
                
                msgU.userMessage(requester, "request_for_somebody_else");
                plrU.playPresetSound(requester, "error");
                return true;
            }
        } else {
            return strictCheck(requester, cmd, args);
        }
    }
}
