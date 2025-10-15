package dev.matkeg.tpask.managers;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import dev.matkeg.tpask.PluginMain;
import dev.matkeg.tpask.utilities.*;

/* --------------------------- MAIN --------------------------- */
public class CommandManager implements CommandExecutor {
    // Modules
    private final PluginMain plugin;
    private final ConfigUtils conU;
    private final OutputUtils output;
    private final StateManager statMan;
    private final RequestManager reqMan;
    private final LanguageManager langMan;

    // Constructor
    public CommandManager(PluginMain plugin) {
        this.plugin = plugin;
        this.output = plugin.getOutput();
        this.conU = plugin.getConfigUtils();
        this.reqMan = plugin.getRequestManager();
        this.statMan = plugin.getStateManager();
        this.langMan = plugin.getLanguageManager();
    }
    
    /* -------------------- OVERRIDES --------------------- */
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!"); 
        return true; }
        
        // Convert the CommandSender to a Player object
        // and get the command that was just invoked
        String invokedCmd = cmd.getName().toLowerCase();
        Player plr = (Player) sender;
        
        // Switch based on the invoked cmd
        switch (invokedCmd) {
            case "tpa": case "tpahere":  
                // We cannot use statMan here direcly, we need to check the all
                // sorts of things, and thus we use reqMan's handleRequestChecks.
                return reqMan.handleRequestChecks(plr, invokedCmd, args);

            case "tpaccept": 
                statMan.accept(plr.getUniqueId());
                return true;
                
            case "tpdeny": 
                statMan.deny(plr.getUniqueId());
                return true;
                
            case "tpcancel": 
                statMan.cancel(plr.getUniqueId());
                return true;
                
            case "back":
                statMan.back(plr);
                return true;
                
            case "tpa-reload":
                conU.reloadConfig(plr);
                langMan.reloadManager();
                return true;
                
            default: return false;
        }
    }
    
    /* ---------------------- APIs ------------------------ */
    
    /**
     * Initializes the given command.
     * @param cmd The command to initialize.
     */
    public void initializeCommand(String cmd) {
        PluginCommand command = plugin.getCommand(cmd);
        if (command != null) command.setExecutor(this);
        else output.error("Command '"+cmd+"' missing from plugin.yml");
    }
}
