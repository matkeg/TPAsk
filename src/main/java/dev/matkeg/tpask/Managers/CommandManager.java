package dev.matkeg.tpask.Managers;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import dev.matkeg.tpask.Utils.*;
import dev.matkeg.tpask.TPAsk;

/* --------------------------- MAIN --------------------------- */
public class CommandManager implements CommandExecutor {
    // Modules
    private final TPAsk plugin;
    private final Output output;
    private final RequestManager reqMan;
    private final StateManager statMan;

    // Class Constructor
    public CommandManager(TPAsk plugin) {
        this.plugin = plugin;
        this.output = plugin.getOutput();
        this.reqMan = plugin.getRequestManager();
        this.statMan = plugin.getStateManager();
    }
    
    /* -------------------- OVERRIDES --------------------- */
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!"); 
            return true; 
        }
        
        // Convert the CommandSender to a Player object
        // and get the command that was just invoked
        String invokedCmd = cmd.getName().toLowerCase();
        Player plr = (Player) sender;
        
        // Switch based on the invoked cmd
        switch (invokedCmd) {
            case "tpa":
                // We cannot use statMan here direcly, we need to check the all
                // sorts of things, and thus we use reqMan's handleRequestChecks.
                return reqMan.handleRequestChecks(plr, args);
                
            case "tpaccept": 
                statMan.accept(plr.getUniqueId());
                return true;
                
            case "tpdeny": 
                statMan.deny(plr.getUniqueId());
                return true;
                
            case "tpcancel": 
                statMan.cancel(plr.getUniqueId());
                return true;
                
            default: return false;
        }
    }
    
    /* -------------------- FUNCTIONS --------------------- */

    
}
