package dev.matkeg.tpask.utilities;

import dev.matkeg.tpask.PluginMain;

import java.util.logging.Logger;
import java.util.logging.Level;

/* ---------------------- MAIN CLASS ---------------------- */
public class OutputUtils {
    // Modules
    private final PluginMain plugin;
    private final Logger logger;

    // Variables
    private boolean autoSpacingEnabled = true;
    
    // Constructor
    public OutputUtils(PluginMain plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /* ----------------------- APIs ----------------------- */

    /**
     * Prints out an info message to the output / console.<br> 
     * Accepts any number of arguments; non-strings will be converted using toString().
     * @param args Text or objects to be outputted.
     */
    public void print(Object... args) {
        logger.info(joinArgs(args));
    }

    /**
     * Prints out a warning message to the output / console.
     * Accepts any number of arguments; non-strings will be converted using toString().
     * @param args Text or objects to be outputted.
     */
    public void warn(Object... args) {
        logger.warning(joinArgs(args));
    }
    
    /**
     * Prints out a critical ("error") message to the output / console.
     * Accepts any number of arguments; non-strings will be converted using toString().
     * @param args Text or objects to be outputted.
     */
    public void error(Object... args) {
        logger.severe(joinArgs(args));
    }
    
    /**
     * Prints out a message to the output / console at the specified level.
     * Accepts any number of arguments; non-strings will be converted using toString().
     * @param args Text or objects to be outputted.
     * 
     * @deprecated Should be used only in cases where {@link #print(Object...)}, 
     * {@link #warn(Object...)}, or {@link #error(Object...)} methods do not
     * achieve the desired results.
     */
    public void log(Level lvl, Object... args) {
        logger.log(lvl, joinArgs(args));
    }
    
    /**
     * Changes the default behavior of joining multiple arguments in an output message.
     * By default, different arguments are joined together with a space in between them.
     * 
     * @param state An optional state which sets the autoSpacingEnabled variable to the
     * passed boolean value. If this isn't provided, the automaticSpacingEnabled will
     * just be inverted from its previous state.
     */
    public void toggleAutoSpacing(Boolean state) {
        if (state != null) {
            autoSpacingEnabled = state;
        } else {
            autoSpacingEnabled = !autoSpacingEnabled;
        }
    }
    
    /* --------------------- FUNCTIONS -------------------- */
    
    // Converts varargs into a single string
    private String joinArgs(Object... args) {
        if (args == null || args.length == 0) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            Object obj = args[i];
            sb.append(obj != null ? obj.toString() : "null");

            // Add spacing if enabled and not at the last argument
            if (autoSpacingEnabled && i < args.length - 1) {
                sb.append(" ");
            }
        }

        // Return the string
        return sb.toString();
    }
}
