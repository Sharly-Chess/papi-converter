package org.sharlychess.papiconverter;

/**
 * Main entry point for the PAPI Converter application.
 * Provides bidirectional conversion between JSON and PAPI database formats.
 */
public class PapiConverter {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        // Parse arguments for flags
        boolean verbose = false;
        int argIndex = 0;
        
        // Check for --verbose flag
        while (argIndex < args.length && args[argIndex].startsWith("--")) {
            if ("--verbose".equals(args[argIndex])) {
                verbose = true;
                argIndex++;
            } else if ("--playerdb".equals(args[argIndex])) {
                break; // Handle --playerdb in the existing logic below
            } else {
                System.err.println("Unknown flag: " + args[argIndex]);
                printUsage();
                System.exit(1);
            }
        }
        
        // Set verbose mode
        VerboseOutput.setVerbose(verbose);
        
        // Check for --playerdb option (adjust for consumed flags)
        if (argIndex < args.length && "--playerdb".equals(args[argIndex])) {
            if (argIndex + 1 >= args.length) {
                System.err.println("Error: --playerdb requires an input file");
                printUsage();
                System.exit(1);
            }
            String inputFile = args[argIndex + 1];
            String outputFile = (argIndex + 2 < args.length) ? args[argIndex + 2] : null;
            try {
                PlayerDbConverter.convert(inputFile, outputFile);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                if (VerboseOutput.isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
            return;
        }
        
        // Ensure we have at least one remaining argument (input file)
        if (argIndex >= args.length) {
            System.err.println("Error: Input file required");
            printUsage();
            System.exit(1);
        }
        
        String inputFile = args[argIndex];
        String outputFile = (argIndex + 1 < args.length) ? args[argIndex + 1] : null;
        
        try {
            if (inputFile.toLowerCase().endsWith(".json")) {
                JsonToPapiConverter.convert(inputFile, outputFile);
            } else if (inputFile.toLowerCase().endsWith(".mdb") || inputFile.toLowerCase().endsWith(".papi")) {
                PapiToJsonConverter.convert(inputFile, outputFile);
            } else {
                System.err.println("Error: Input file must be either .json, .mdb, or .papi");
                printUsage();
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (VerboseOutput.isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.err.println("Usage: java PapiConverter [--verbose] <input-file> [output-file]");
        System.err.println("       java PapiConverter [--verbose] --playerdb <input-mdb-file> [output-sql-file]");
        System.err.println("");
        System.err.println("Options:");
        System.err.println("  --verbose         Show detailed progress information");
        System.err.println("");
        System.err.println("Conversions:");
        System.err.println("  JSON to PAPI:     PapiConverter input.json [output.papi]");
        System.err.println("  PAPI to JSON:     PapiConverter input.papi [output.json]");
        System.err.println("  MDB to JSON:      PapiConverter input.mdb [output.json]");
        System.err.println("  PlayerDB Convert: PapiConverter --playerdb Data.mdb [players.sql]");
        System.err.println("");
        System.err.println("If output file is not specified, it will be generated automatically.");
    }
}
