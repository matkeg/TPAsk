package dev.matkeg.tpask.Managers;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import dev.matkeg.tpask.Utils.*;
import dev.matkeg.tpask.TPAsk;

import java.util.UUID;
import java.util.Map;

/* --------------------------- MAIN --------------------------- */
public class RequestManager {
    // Modules
    private final TPAsk plugin;
    private final Output output;
    private final ColorUtils colU;
    private final PlayerUtils plrU;
    private final StateManager statMan;
    private final LanguageManager langMan;
    
    // Class Constructor
    public RequestManager(TPAsk plugin) {
        this.plugin = plugin;
        this.output = plugin.getOutput();
        this.colU = plugin.getColorUtils();
        this.plrU = plugin.getPlayerUtils();
        this.statMan = plugin.getStateManager();
        this.langMan = plugin.getLanguageManager();
    }
    
    /* ---------------------- APIs ------------------------ */
    
    
    /**
     * Performs checks on whether a request can be
     * done and displays all appropriate messages.
     * 
     * @param requester The requesting Player
     * @param args Should include a Player object 
     * on the 1st (args[0]) slot which represents
     * the receiving player.
     */
    public boolean handleRequestChecks(Player requester, String[] args) {
        // Check if no arguments are passed
        if (args.length != 1) { 
            plrU.sendPresetMessage(requester,
                "messages.usage_help_player_arg", 
                Map.of("%command%", "tpa")); 
            return true;
        }
        
        // Get the receiver and requester info
        Player receiver = Bukkit.getPlayer(args[0]);

        /* --------------------- START CHECKING ---------------------- */
        
        // RECEIVER IS VALID CHECK  
        if (!plrU.isValid(receiver)) { 
            plrU.sendPresetMessage(requester,
            "messages.receiver_not_online", Map.of("%receiver%", args[0])); 
        return true; }
        
        // Store some values we'll need later;
        String receiverName = plrU.getName(receiver, 
                langMan.getLocalizedString("player_noun", "Player"));
        String requesterName = plrU.getName(requester,
                langMan.getLocalizedString("player_noun", "Player"));
        
        UUID receiverId = receiver.getUniqueId();
        UUID requesterId = requester.getUniqueId();
        
        // TPA TO THEMSELVES CHECK
        if (receiver.equals(requester)) { 
            plrU.sendPresetMessage(requester, "messages.request_yourself", null);
        return true; }
        
        // REQUESTER COOLDOWN CHECK
        if (statMan.onCooldown(requesterId)) {
            plrU.sendPresetMessage(requester, 
                    "messages.request_cooldown", 
                    Map.of("%seconds%", String.valueOf(
                            statMan.cooldownRemaining(requesterId)
            )));
        return true; }
        
        // RECEIVER HAS AN ACTIVE REQUEST CHECK
        if (statMan.hasIncoming(receiverId)) {
            plrU.sendPresetMessage(requester, "messages.receiver_request_busy", Map.of("%receiver%", receiverName)); 
        return true; }
        
        // REQUESTER HAS AN ACTIVE REQUEST CHECK
        if (statMan.hasOutgoing(requesterId)) {
            plrU.sendPresetMessage(requester, "messages.requester_already_has_outgoing", null); 
        return true; }
        
        /* ------- CHECKS PASSED, REQUEST CREATION AND DISPLAY ------- */
        
        statMan.createRequest(requester, receiver);

        // Send the notification messages and play the sound effects
        plrU.sendPresetMessage(requester, "messages.request_sent", Map.of("%receiver%", receiver.getName()));
        plrU.sendPresetMessage(receiver, "messages.request_received", Map.of("%requester%", requester.getName()));

        plrU.playPresetSound(requester, "sent");
        plrU.playPresetSound(receiver, "received");
        
        // RECEIVER: CLICKABLE CONTENT
        Component receiver_clickable_reply = plrU.clickable(
                langMan.getLocalizedString("clickables.accept.text", "[✔]"),
                langMan.getLocalizedString("clickables.accept.hover", null)
                                     .replace("%requester%", requesterName),
                langMan.getLocalizedString("clickables.accept.command", "/tpaccept"),
                colU.parseColor(langMan.getLocalizedString("clickables.accept.color", "GREEN")))
                
                .append(Component.text("  ")) // SPACE IN BETWEEN THE TWO BUTTONS
                
                .append(plrU.clickable(     
                langMan.getLocalizedString("clickables.deny.text", "[✔]"),
                langMan.getLocalizedString("clickables.deny.hover", null)
                                   .replace("%requester%", requesterName),
                langMan.getLocalizedString("clickables.deny.command", "/tpdeny"),
                colU.parseColor(langMan.getLocalizedString("clickables.deny.color", "RED"))));

        receiver.sendMessage(receiver_clickable_reply);
        
        
        // REQUESTER: CLICKABLE CONTENT
        Component requester_clickable_reply = plrU.clickable(
                langMan.getLocalizedString("clickables.cancel.text", "[⬅]"),
                langMan.getLocalizedString("clickables.cancel.hover", null)
                                       .replace("%receiver%", receiverName),
                langMan.getLocalizedString("clickables.cancel.command", "/tpaccept"),
                colU.parseColor(langMan.getLocalizedString("clickables.cancel.color", "GRAY")));

        requester.sendMessage(requester_clickable_reply);
        
        // And, that's the end of it... Phew...
        return true;
    }
}
