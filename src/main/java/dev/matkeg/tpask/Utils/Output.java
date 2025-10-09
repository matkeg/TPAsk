package dev.matkeg.tpask.Utils;

import dev.matkeg.tpask.TPAsk;
import java.util.logging.Logger;

/* --------------------------- MAIN --------------------------- */
public class Output {
    // Modules
    private final TPAsk plugin;
    private final Logger logger;

    // Class Constructor
    public Output(TPAsk plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /* ----------------------- APIs ----------------------- */

    /**
     * Prints out an info message to the output.
     * Accepts any number of arguments; non-strings will be converted using toString().
     * @param args Text or objects to be outputted.
     */
    public void print(Object... args) {
        logger.info(joinArgs(args));
    }

    /**
     * Prints out a warning message.
     * Accepts any number of arguments; non-strings will be converted using toString().
     * @param args Text or objects to be outputted.
     */
    public void warn(Object... args) {
        logger.warning(joinArgs(args));
    }

    /* ----------------------- Helper ----------------------- */
    
    // Converts varargs into a single string
    private String joinArgs(Object... args) {
        if (args == null || args.length == 0) return "";
        
        // Initialize the string builder and
        // itterate through the arguments
        StringBuilder sb = new StringBuilder();
        for (Object obj : args) {
            if (obj != null) sb.append(obj.toString());
            else sb.append("null");
        }
        
        // Return the string
        return sb.toString();
    }
}
