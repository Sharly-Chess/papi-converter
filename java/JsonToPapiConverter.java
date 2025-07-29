package org.sharlychess.papiconverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import com.healthmarketscience.jackcess.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handles conversion from JSON format to PAPI database format.
 */
public class JsonToPapiConverter {
    
    /**
     * Converts a JSON file to PAPI (.mdb) format.
     * @param jsonFile Path to the input JSON file
     * @param mdbFile Path to the output PAPI file
     * @throws Exception if conversion fails
     */
    public static void convert(String jsonFile, String mdbFile) throws Exception {
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
        
        // Create parent directories if they don't exist
        File outputFile = new File(mdbFile);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new Exception("Failed to create directory: " + parentDir.getAbsolutePath());
            }
            System.out.println("Created directory: " + parentDir.getAbsolutePath());
        }
        
        Files.copy(Paths.get(templateFile), Paths.get(mdbFile), StandardCopyOption.REPLACE_EXISTING);
        
        // Open the copied MDB file
        Database db = DatabaseBuilder.open(new File(mdbFile));
        
        try {
            // Handle tournament variables
            processVariables(db, rootNode);
            
            // Handle players data
            processPlayers(db, rootNode);
            
        } finally {
            db.close();
        }
        
        System.out.println("Output MDB file: " + mdbFile);
        System.out.println("MDB conversion completed successfully!");
    }
    
    /**
     * Processes tournament variables from JSON and updates the INFO table.
     */
    private static void processVariables(Database db, JsonNode rootNode) throws Exception {
        // Get the INFO table
        Table infoTable = db.getTable("INFO");
        
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
                String englishVariable = field.getKey();
                String value = field.getValue().asText();
                
                // Map English variable name to French
                String frenchVariable = VariableMapping.englishToFrench(englishVariable);
                if (frenchVariable != null && VariableMapping.isValidFrenchVariable(frenchVariable)) {
                    Row existingRow = existingRows.get(frenchVariable);
                    if (existingRow != null) {
                        // Overwrite existing row
                        existingRow.put("Value", value);
                        infoTable.updateRow(existingRow);
                        System.out.println("  Updated: " + englishVariable + " (" + frenchVariable + ") = " + value);
                    } else {
                        // Add new row
                        infoTable.addRow(frenchVariable, value);
                        System.out.println("  Added: " + englishVariable + " (" + frenchVariable + ") = " + value);
                    }
                } else {
                    System.out.println("  Warning: Skipping invalid variable: " + englishVariable);
                }
            }
        } else {
            System.out.println("No 'variables' object found in JSON");
        }
    }
    
    /**
     * Processes players data from JSON and updates the JOUEUR table.
     */
    private static void processPlayers(Database db, JsonNode rootNode) throws Exception {
        JsonNode playersNode = rootNode.get("players");
        if (playersNode != null && playersNode.isArray()) {
            System.out.println("\nProcessing players data...");
            Table playerTable = db.getTable("JOUEUR");
            
            // Clear existing players (except EXEMPT which is Ref=1)
            Iterator<Row> existingRows = playerTable.iterator();
            while (existingRows.hasNext()) {
                Row row = existingRows.next();
                Object refObj = row.get("Ref");
                if (refObj != null && ((Number)refObj).intValue() > 1) {
                    existingRows.remove();
                }
            }
            
            // Add new players
            int playerRef = 2; // Start from 2, as 1 is reserved for EXEMPT
            for (JsonNode playerNode : playersNode) {
                PlayerConverter.addPlayerToTable(playerTable, playerNode, playerRef++);
            }
            
            System.out.println("Added " + (playerRef - 2) + " players to JOUEUR table");
        } else {
            System.out.println("No 'players' array found in JSON");
        }
    }
}
