package org.sharlychess.papiconverter;

/**
 * Utility class to manage verbose output throughout the application.
 * This allows progress messages to be controlled by a --verbose flag.
 */
public class VerboseOutput {
    
    private static boolean verboseMode = false;
    
    /**
     * Sets the verbose mode for the application.
     * @param verbose true to enable verbose output, false to disable
     */
    public static void setVerbose(boolean verbose) {
        verboseMode = verbose;
    }
    
    /**
     * Prints a message only if verbose mode is enabled.
     * @param message the message to print
     */
    public static void println(String message) {
        if (verboseMode) {
            System.out.println(message);
        }
    }
    
    /**
     * Prints a formatted message only if verbose mode is enabled.
     * @param format the format string
     * @param args the arguments for formatting
     */
    public static void printf(String format, Object... args) {
        if (verboseMode) {
            System.out.printf(format, args);
        }
    }
    
    /**
     * Always prints a message regardless of verbose mode (for important output).
     * @param message the message to print
     */
    public static void alwaysPrintln(String message) {
        System.out.println(message);
    }
    
    /**
     * Always prints an error message regardless of verbose mode.
     * @param message the error message to print
     */
    public static void errorPrintln(String message) {
        System.err.println(message);
    }
    
    /**
     * Returns whether verbose mode is enabled.
     * @return true if verbose mode is enabled
     */
    public static boolean isVerbose() {
        return verboseMode;
    }
}
