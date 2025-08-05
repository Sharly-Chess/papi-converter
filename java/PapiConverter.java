package org.sharlychess.papiconverter;

/**
 * Main entry point for the PAPI Converter application.
 * Provides bidirectional conversion between JSON and PAPI database formats.
 */
public class PapiConverter {
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        // Check for --playerdb option
        if (args.length >= 2 && "--playerdb".equals(args[0])) {
            String inputFile = args[1];
            String outputFile = args.length > 2 ? args[2] : null;
            PlayerDbConverter.convert(inputFile, outputFile);
            return;
        }
        
        String inputFile = args[0];
        String outputFile = args.length > 1 ? args[1] : null;
        
        if (inputFile.toLowerCase().endsWith(".json")) {
            JsonToPapiConverter.convert(inputFile, outputFile);
        } else if (inputFile.toLowerCase().endsWith(".mdb") || inputFile.toLowerCase().endsWith(".papi")) {
            PapiToJsonConverter.convert(inputFile, outputFile);
        } else {
            System.err.println("Error: Input file must be either .json, .mdb, or .papi");
            printUsage();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.err.println("Usage: java PapiConverter <input-file> [output-file]");
        System.err.println("       java PapiConverter --playerdb <input-mdb-file> [output-sql-file]");
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
