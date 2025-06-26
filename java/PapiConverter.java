package org.sharlychess.papiconverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

import com.healthmarketscience.jackcess.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class PapiConverter {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputFile = args.length > 1 ? args[1] : null;
        
        if (inputFile.toLowerCase().endsWith(".json")) {
            convertJsonToMdb(inputFile, outputFile);
        } else if (inputFile.toLowerCase().endsWith(".mdb")) {
            convertMdbToJson(inputFile, outputFile);
        } else {
            System.err.println("Error: Input file must be either .json or .mdb");
            printUsage();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.err.println("Usage: java PapiConverter <input-file> [output-file]");
        System.err.println("");
        System.err.println("Conversions:");
        System.err.println("  JSON to MDB: PapiConverter input.json [output.mdb]");
        System.err.println("  MDB to JSON: PapiConverter input.mdb [output.json]");
        System.err.println("");
        System.err.println("If output file is not specified, it will be generated automatically.");
    }
    
    private static void convertJsonToMdb(String jsonFile, String mdbFile) throws Exception {
        System.out.println("Converting JSON to MDB...");
        
        // Generate output filename if not provided
        if (mdbFile == null) {
            mdbFile = jsonFile.replaceAll("\\.json$", ".papi");
        }
        
        // Read and parse JSON content
        String jsonContent = Files.readString(Paths.get(jsonFile));
        System.out.println("Reading JSON from: " + jsonFile);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonContent);
        
        // Check for template file
        String templateFile = "static/template-3.3.8.papi";
        if (!Files.exists(Paths.get(templateFile))) {
            throw new Exception("Template file not found: " + templateFile);
        }
        
        // Copy template to output location
        System.out.println("Copying template file: " + templateFile);
        Files.copy(Paths.get(templateFile), Paths.get(mdbFile), StandardCopyOption.REPLACE_EXISTING);
        
        // Open the copied MDB file
        Database db = DatabaseBuilder.open(new File(mdbFile));
        
        try {
            // Get the INFO table
            Table infoTable = db.getTable("INFO");
            
            // Define valid variable names
            List<String> validVariables = Arrays.asList(
                "Nom", "Genre", "NbrRondes", "Pairing", "Cadence", "ClassElo", 
                "EloBase1", "EloBase2", "Dep1", "Dep2", "Dep3", "DecomptePoints", 
                "Lieu", "DateDebut", "DateFin", "Arbitre", "Homologation"
            );
            
            // Get variables from JSON
            JsonNode variablesNode = rootNode.get("variables");
            if (variablesNode != null && variablesNode.isObject()) {
                System.out.println("Updating INFO table with variables...");
                
                // Create a map of existing rows for quick lookup
                Map<String, Row> existingRows = new HashMap<>();
                for (Row row : infoTable) {
                    Object variableObj = row.get("Variable");
                    if (variableObj != null) {
                        existingRows.put(variableObj.toString(), row);
                    }
                }
                
                // Update or insert data
                Iterator<Map.Entry<String, JsonNode>> fields = variablesNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String variable = field.getKey();
                    String value = field.getValue().asText();
                    
                    if (validVariables.contains(variable)) {
                        Row existingRow = existingRows.get(variable);
                        if (existingRow != null) {
                            // Overwrite existing row
                            existingRow.put("Value", value);
                            infoTable.updateRow(existingRow);
                            System.out.println("  Updated: " + variable + " = " + value);
                        } else {
                            // Add new row
                            infoTable.addRow(variable, value);
                            System.out.println("  Added: " + variable + " = " + value);
                        }
                    } else {
                        System.out.println("  Warning: Skipping invalid variable: " + variable);
                    }
                }
            } else {
                System.out.println("No 'variables' object found in JSON");
            }
            
        } finally {
            db.close();
        }
        
        System.out.println("Output MDB file: " + mdbFile);
        System.out.println("MDB conversion completed successfully!");
    }
    
    private static void convertMdbToJson(String mdbFile, String jsonFile) throws Exception {
        System.out.println("Converting MDB to JSON...");
        
        // Generate output filename if not provided
        if (jsonFile == null) {
            jsonFile = mdbFile.replaceAll("\\.mdb$", ".json");
        }
        
        // Check if MDB file exists
        if (!Files.exists(Paths.get(mdbFile))) {
            throw new Exception("MDB file not found: " + mdbFile);
        }
        
        System.out.println("Reading MDB from: " + mdbFile);
        
        // TODO: Implement MDB to JSON conversion logic
        // This would involve:
        // 1. Opening the MDB database
        // 2. Reading table schemas and data
        // 3. Converting to JSON format
        
        long mdbSize = Files.size(Paths.get(mdbFile));
        System.out.println("MDB file size: " + mdbSize + " bytes");
        
        // Placeholder: Create sample JSON
        String jsonOutput = "{\n  \"database\": \"" + new File(mdbFile).getName() + "\",\n  \"tables\": [],\n  \"converted_at\": \"" + java.time.Instant.now() + "\"\n}";
        
        Files.write(Paths.get(jsonFile), jsonOutput.getBytes());
        System.out.println("Output JSON file: " + jsonFile);
        System.out.println("JSON conversion completed successfully!");
    }
}
