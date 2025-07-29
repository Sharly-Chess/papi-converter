package org.sharlychess.papiconverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import com.healthmarketscience.jackcess.*;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles conversion from PAPI database format to JSON format.
 */
public class PapiToJsonConverter {
    
    /**
     * Converts a PAPI (.mdb) file to JSON format.
     * @param mdbFile Path to the input PAPI file
     * @param jsonFile Path to the output JSON file
     * @throws Exception if conversion fails
     */
    public static void convert(String mdbFile, String jsonFile) throws Exception {
        System.out.println("Converting MDB to JSON...");
        
        // Generate output filename if not provided
        if (jsonFile == null) {
            jsonFile = mdbFile.replaceAll("\\.mdb$|\\.papi$", ".json");
        }
        
        // Check if MDB file exists
        if (!Files.exists(Paths.get(mdbFile))) {
            throw new Exception("MDB file not found: " + mdbFile);
        }
        
        System.out.println("Reading MDB from: " + mdbFile);
        
        // Open the MDB database
        Database db = DatabaseBuilder.open(new File(mdbFile));
        
        Map<String, Object> jsonData = new LinkedHashMap<>();
        
        try {
            // Read tournament variables
            Map<String, String> variables = processVariables(db);
            jsonData.put("variables", variables);
            
            // Read players data
            List<Map<String, Object>> players = processPlayers(db);
            jsonData.put("players", players);
            
        } finally {
            db.close();
        }
        
        // Convert to JSON and write to file
        ObjectMapper mapper = new ObjectMapper();

        // Create parent directories if they don't exist
        File outputFile = new File(jsonFile);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new Exception("Failed to create directory: " + parentDir.getAbsolutePath());
            }
            System.out.println("Created directory: " + parentDir.getAbsolutePath());
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, jsonData);
        
        System.out.println("Output JSON file: " + jsonFile);
        System.out.println("JSON conversion completed successfully!");
    }
    
    /**
     * Processes tournament variables from the INFO table.
     */
    private static Map<String, String> processVariables(Database db) throws Exception {
        System.out.println("Reading tournament variables...");
        Table infoTable = db.getTable("INFO");
        Map<String, String> variables = new LinkedHashMap<>();
        
        for (Row row : infoTable) {
            Object variableObj = row.get("Variable");
            Object valueObj = row.get("Value");
            
            if (variableObj != null && valueObj != null) {
                String frenchVarName = variableObj.toString();
                if (VariableMapping.isValidFrenchVariable(frenchVarName)) {
                    // Map French variable name to English
                    String englishVarName = VariableMapping.frenchToEnglish(frenchVarName);
                    if (englishVarName != null) {
                        variables.put(englishVarName, valueObj.toString());
                    } else {
                        // Fallback to French name if no mapping exists
                        variables.put(frenchVarName, valueObj.toString());
                    }
                }
            }
        }
        
        System.out.println("  Found " + variables.size() + " tournament variables");
        return variables;
    }
    
    /**
     * Processes players data from the JOUEUR table.
     */
    private static List<Map<String, Object>> processPlayers(Database db) throws Exception {
        System.out.println("Reading players data...");
        Table joueurTable = db.getTable("JOUEUR");
        
        // Step 1: Collect all player rows and sort them by Ref field
        List<Row> playerRows = new ArrayList<>();
        for (Row row : joueurTable) {
            Object refObj = row.get("Ref");
            if (refObj != null && ((Number)refObj).intValue() > 1) { // Skip EXEMPT player (Ref=1)
                playerRows.add(row);
            }
        }
        
        // Sort players by their Ref field to ensure consistent ordering
        playerRows.sort((row1, row2) -> {
            int ref1 = ((Number)row1.get("Ref")).intValue();
            int ref2 = ((Number)row2.get("Ref")).intValue();
            return Integer.compare(ref1, ref2);
        });
        
        // Step 2: Create mapping from PAPI Ref to JSON index (0-based)
        Map<Integer, Integer> papiRefToJsonIndex = new HashMap<>();
        for (int i = 0; i < playerRows.size(); i++) {
            Object refObj = playerRows.get(i).get("Ref");
            if (refObj != null) {
                int papiRef = ((Number)refObj).intValue();
                papiRefToJsonIndex.put(papiRef, i);
            }
        }
        
        // Step 3: Convert sorted rows to JSON with proper opponent mapping
        List<Map<String, Object>> players = new ArrayList<>();
        for (Row row : playerRows) {
            Map<String, Object> player = PlayerConverter.convertRowToJsonWithMapping(row, papiRefToJsonIndex);
            if (player != null) {
                players.add(player);
            }
        }
        
        System.out.println("  Found " + players.size() + " players");
        return players;
    }
}
