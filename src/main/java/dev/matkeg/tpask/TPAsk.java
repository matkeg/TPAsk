package dev.matkeg.tpask;

import org.bukkit.plugin.java.JavaPlugin;

import dev.matkeg.tpask.Managers.*;
import dev.matkeg.tpask.Utils.*;

/* --------------------------- MAIN --------------------------- */
public class TPAsk extends JavaPlugin {
    // Modules
    private LanguageManager langMan;
    private RequestManager reqMan;
    private CommandManager comMan;
    private StateManager statMan;
    private PlayerUtils plrU;
    private ColorUtils colU;
    private Output output;

    // Fetchers: Call these in other files to get the initialized modules
    public LanguageManager getLanguageManager() {return langMan;}
    public RequestManager getRequestManager() {return reqMan;}
    public CommandManager getCommandManager() {return comMan;}
    public StateManager getStateManager() {return statMan;}
    public PlayerUtils getPlayerUtils() {return plrU;}
    public ColorUtils getColorUtils() {return colU;}
    public Output getOutput() {return output;}
    
    /* -------------------- OVERRIDES --------------------- */
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // INITIALIZE THE MODULES (ORDERED)
        output = new Output(this);
        colU = new ColorUtils(this);
        
        langMan = new LanguageManager(this);
        langMan.saveDefaultPacks();
        
        plrU = new PlayerUtils(this);
        statMan = new StateManager(this);
        reqMan = new RequestManager(this);
        comMan = new CommandManager(this);

        // Setup the commands;
        langMan.localizeCommands();
        getCommand("tpa").setExecutor(comMan);
        getCommand("tpdeny").setExecutor(comMan);
        getCommand("tpcancel").setExecutor(comMan);
        getCommand("tpaccept").setExecutor(comMan);
        
        // Inform the server
        output.print("Successfully Enabled!");
    }
    
    @Override
    public void onDisable() {
        statMan.cancelAll();
        output.print("Successfully Disabled!");
    }
        
}
