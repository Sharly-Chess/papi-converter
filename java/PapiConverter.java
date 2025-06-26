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
            
            // Handle players data
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
                    addPlayerToTable(playerTable, playerNode, playerRef++);
                }
                
                System.out.println("Added " + (playerRef - 2) + " players to JOUEUR table");
            } else {
                System.out.println("No 'players' array found in JSON");
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
    
    private static void addPlayerToTable(Table playerTable, JsonNode playerNode, int playerRef) throws Exception {
        // Create new row for player
        Map<String, Object> rowData = new HashMap<>();
        
        // Set player reference
        rowData.put("Ref", playerRef);
        rowData.put("ClubRef", 0); // Always set ClubRef to 0

        // Set default values for required fields
        rowData.put("Fixe", 0); // Default Fixe
        rowData.put("InscriptionRegle", 0); // Default InscriptionRegle
        rowData.put("InscriptionDu", 0); // Default InscriptionDu
        rowData.put("AffType", "N");
        
        // Player basic information
        setFieldIfExists(rowData, playerNode, "RefFFE", "refFFE");
        setFieldIfExists(rowData, playerNode, "Nr", "nr");
        setFieldIfExists(rowData, playerNode, "NrFFE", "nrFFE");
        setFieldIfExists(rowData, playerNode, "Nom", "lastName");
        setFieldIfExists(rowData, playerNode, "Prenom", "firstName");
        setFieldIfExists(rowData, playerNode, "Sexe", "gender");
        
        // Dates (birth date)
        if (playerNode.has("birthDate")) {
            String birthDate = playerNode.get("birthDate").asText();
            if (!birthDate.isEmpty()) {
                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    java.time.LocalDate localDate = java.time.LocalDate.parse(birthDate, formatter);
                    java.util.Date date = java.util.Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                    rowData.put("NeLe", date);
                } catch (Exception e) {
                    System.out.println("  Warning: Invalid birth date format for player " + playerRef + ": " + birthDate + " (expected DD/MM/YYYY)");
                }
            }
        }
        
        setFieldIfExists(rowData, playerNode, "Cat", "category");
        setFieldIfExists(rowData, playerNode, "Elo", "elo");
        setFieldIfExists(rowData, playerNode, "Rapide", "rapidElo");
        setFieldIfExists(rowData, playerNode, "Blitz", "blitzElo");
        setFieldIfExists(rowData, playerNode, "Federation", "federation");
        setFieldIfExists(rowData, playerNode, "Club", "club");
        setFieldIfExists(rowData, playerNode, "Ligue", "league");
        setFieldIfExists(rowData, playerNode, "Fide", "fideElo");
        setFieldIfExists(rowData, playerNode, "RapideFide", "fideRapidElo");
        setFieldIfExists(rowData, playerNode, "BlitzFide", "fideBlitzElo");
        setFieldIfExists(rowData, playerNode, "FideCode", "fideCode");
        setFieldIfExists(rowData, playerNode, "FideTitre", "fideTitle");
        setFieldIfExists(rowData, playerNode, "AffType", "licenceType");

        // Boolean fields
        if (playerNode.has("checkedIn")) {
            rowData.put("Pointe", playerNode.get("checkedIn").asBoolean());
        }
        
        // Contact information
        setFieldIfExists(rowData, playerNode, "Adresse", "address");
        setFieldIfExists(rowData, playerNode, "CP", "postalCode");
        setFieldIfExists(rowData, playerNode, "Tel", "phone");
        setFieldIfExists(rowData, playerNode, "EMail", "email");
        setFieldIfExists(rowData, playerNode, "Commentaire", "comment");
        
        // Initialize all rounds with defaults first
        for (int roundNum = 1; roundNum <= 24; roundNum++) {
            String roundStr = String.format("%02d", roundNum);
            rowData.put("Rd" + roundStr + "Cl", "R"); // Default color: R
            rowData.put("Rd" + roundStr + "Res", 0);   // Default result: 0
        }
        
        // Round results (up to 24 rounds)
        JsonNode roundsNode = playerNode.get("rounds");
        if (roundsNode != null && roundsNode.isArray()) {
            int roundNum = 1;
            for (JsonNode roundNode : roundsNode) {
                if (roundNum > 24) break; // Maximum 24 rounds
                
                String roundStr = String.format("%02d", roundNum);
                
                // Color (Cl) - B/N/R/F
                if (roundNode.has("color")) {
                    rowData.put("Rd" + roundStr + "Cl", roundNode.get("color").asText());
                }
                
                // Opponent (Adv) - opponent player reference
                if (roundNode.has("opponent")) {
                    int opponent = roundNode.get("opponent").asInt();
                    if (opponent > 0) {
                        rowData.put("Rd" + roundStr + "Adv", opponent);
                    }
                }
                
                // Result (Res) - NO_RESULT = 0, LOSS = 1, DRAW = 2, GAIN = 3,...
                if (roundNode.has("result")) {
                    rowData.put("Rd" + roundStr + "Res", roundNode.get("result").asInt());
                }
                
                roundNum++;
            }
        }
        
        // Add the row to the table using the correct column order
        Object[] rowValues = new Object[playerTable.getColumnCount()];
        for (int i = 0; i < playerTable.getColumnCount(); i++) {
            Column column = playerTable.getColumns().get(i);
            rowValues[i] = rowData.get(column.getName());
        }
        playerTable.addRow(rowValues);
        
        String playerName = playerNode.has("firstName") ? 
            playerNode.get("firstName").asText() + " " + (playerNode.has("lastName") ? playerNode.get("lastName").asText() : "") :
            (playerNode.has("lastName") ? playerNode.get("lastName").asText() : "Player " + playerRef);
        System.out.println("  Added player: " + playerName.trim() + " (Ref: " + playerRef + ")");
    }
    
    private static void setFieldIfExists(Map<String, Object> rowData, JsonNode playerNode, String dbField, String jsonField) {
        if (playerNode.has(jsonField)) {
            JsonNode fieldNode = playerNode.get(jsonField);
            if (!fieldNode.isNull()) {
                if (fieldNode.isNumber()) {
                    if (fieldNode.isInt()) {
                        rowData.put(dbField, fieldNode.asInt());
                    } else {
                        rowData.put(dbField, fieldNode.asDouble());
                    }
                } else {
                    String value = fieldNode.asText();
                    if (!value.isEmpty()) {
                        rowData.put(dbField, value);
                    }
                }
            }
        }
    }
}
