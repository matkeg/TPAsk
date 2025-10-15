package dev.matkeg.tpask;

import org.bukkit.plugin.java.JavaPlugin;

import dev.matkeg.tpask.utilities.*;
import dev.matkeg.tpask.managers.*;

/* ---------------------- MAIN CLASS ---------------------- */
public class PluginMain extends JavaPlugin {
    // Modules
    private ColorUtils colU;
    private ConfigUtils conU;
    private PlayerUtils plrU;
    private VersionUtils verU;
    private MessageUtils msgU;
    private OutputUtils output;
    private StateManager statMan;
    private RequestManager reqMan;
    private CommandManager cmdMan;
    private LanguageManager langMan;
    
    // Fetchers: Call these in other files to get the initialized modules
    public OutputUtils getOutput() { return output; }
    public ColorUtils getColorUtils() { return colU; }
    public ConfigUtils getConfigUtils() { return conU; }
    public PlayerUtils getPlayerUtils() { return plrU; }
    public VersionUtils getVersionUtils() { return verU; }
    public MessageUtils getMessageUtils() { return msgU; }
    public StateManager getStateManager() {return statMan;}
    public CommandManager getCommandManager() { return cmdMan; }
    public RequestManager getRequestManager() { return reqMan; }
    public LanguageManager getLanguageManager() { return langMan; }
    
    /* -------------------- OVERRIDES --------------------- */
    
    @Override
    public void onLoad() {
        saveDefaultConfig();
        
        // INITIALIZE THE MODULES (ORDERED)
        output = new OutputUtils(this);
        
        verU = new VersionUtils(this);
        colU = new ColorUtils(this);
        conU = new ConfigUtils(this);
        
        langMan = new LanguageManager(this);
        langMan.saveDefaultPacks();
        
        msgU = new MessageUtils(this);
        plrU = new PlayerUtils(this);
        msgU.setPlayerUtils(plrU);
        
        statMan = new StateManager(this);
        reqMan = new RequestManager(this);
        cmdMan = new CommandManager(this);
    }
    
    @Override
    public void onEnable() {
        try {
            // Check if the config is compatible;
            conU.checkConfigCompatibility();
            // Setup the commands;
            langMan.localizeCommands();
        } catch (Exception e) {
            output.error("Plugin failed to initialize: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
        
        // Initialize command   
        cmdMan.initializeCommand("tpa");
        cmdMan.initializeCommand("back");
        cmdMan.initializeCommand("tpdeny");
        cmdMan.initializeCommand("tpahere");
        cmdMan.initializeCommand("tpcancel");
        cmdMan.initializeCommand("tpaccept");
        cmdMan.initializeCommand("tpa-reload");
    }
    
    @Override
    public void onDisable() { 
        statMan.cancelAll(); 
        msgU.cancelAllActionBars();
    }
}
