package org.sharlychess.papiconverter;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.healthmarketscience.jackcess.*;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handles conversion of player data between JSON and PAPI database format.
 */
public class PlayerConverter {
    
    /**
     * Converts JSON player reference (0-based) to PAPI database reference (2-based).
     * JSON: 0, 1, 2, 3... -> PAPI: 2, 3, 4, 5...
     */
    public static int jsonRefToPapiRef(int jsonRef) {
        return jsonRef + 2;
    }
    
    /**
     * Converts PAPI database reference (2-based) to JSON player reference (0-based).
     * PAPI: 2, 3, 4, 5... -> JSON: 0, 1, 2, 3...
     */
    public static int papiRefToJsonRef(int papiRef) {
        return papiRef - 2;
    }
    
    /**
     * Adds a player from JSON to the JOUEUR table.
     * @param playerTable The JOUEUR table
     * @param playerNode The JSON node containing player data
     * @param playerRef The player reference ID
     * @throws Exception if conversion fails
     */
    public static void addPlayerToTable(Table playerTable, JsonNode playerNode, int playerRef) throws Exception {
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
        setFieldIfExists(rowData, playerNode, "InscriptionRegle", "paid");
        setFieldIfExists(rowData, playerNode, "InscriptionDu", "owed");
        setFieldIfExists(rowData, playerNode, "Fixe", "fixedBoard");

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
        if (roundsNode != null) {
            var fields = roundsNode.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String roundNumStr = entry.getKey();
                JsonNode roundNode = entry.getValue();
                
                try {
                    int roundNum = Integer.parseInt(roundNumStr);
                    if (roundNum >= 1 && roundNum <= 24) {
                        processRoundData(rowData, roundNode, roundNum, playerRef, playerTable);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("  Warning: Invalid round number '" + roundNumStr + "' for player " + playerRef);
                }
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
    
    /**
     * Converts a database row to JSON player format with reference mapping.
     * @param row The database row
     * @param papiRefToJsonIndexMap Mapping from PAPI references to JSON array indices
     * @return A map representing the player in JSON format
     * @throws Exception if conversion fails
     */
    public static Map<String, Object> convertRowToJsonWithMapping(Row row, Map<Integer, Integer> papiRefToJsonIndexMap) throws Exception {
        Map<String, Object> player = new HashMap<>();
        
        // Basic player information
        addFieldIfNotNull(player, "refFFE", row.get("RefFFE"));
        addFieldIfNotNull(player, "nr", row.get("Nr"));
        addFieldIfNotNull(player, "nrFFE", row.get("NrFFE"));
        addFieldIfNotNull(player, "lastName", row.get("Nom"));
        addFieldIfNotNull(player, "firstName", row.get("Prenom"));
        addFieldIfNotNull(player, "gender", row.get("Sexe"));
        
        // Birth date - convert from Date to DD/MM/YYYY format
        Object birthDateObj = row.get("NeLe");
        if (birthDateObj instanceof java.util.Date) {
            java.util.Date date = (java.util.Date) birthDateObj;
            java.time.LocalDate localDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            player.put("birthDate", localDate.format(formatter));
        }
        
        addFieldIfNotNull(player, "category", row.get("Cat"));
        addFieldIfNotNull(player, "elo", row.get("Elo"));
        addFieldIfNotNull(player, "rapidElo", row.get("Rapide"));
        addFieldIfNotNull(player, "blitzElo", row.get("Blitz"));
        addFieldIfNotNull(player, "federation", row.get("Federation"));
        addFieldIfNotNull(player, "club", row.get("Club"));
        addFieldIfNotNull(player, "league", row.get("Ligue"));
        addFieldIfNotNull(player, "fideElo", row.get("Fide"));
        addFieldIfNotNull(player, "fideRapidElo", row.get("RapideFide"));
        addFieldIfNotNull(player, "fideBlitzElo", row.get("BlitzFide"));
        addFieldIfNotNull(player, "fideCode", row.get("FideCode"));
        addFieldIfNotNull(player, "fideTitle", row.get("FideTitre"));
        addFieldIfNotNull(player, "licenceType", row.get("AffType"));
        addFieldIfNotNull(player, "paid", row.get("InscriptionRegle"));
        addFieldIfNotNull(player, "owed", row.get("InscriptionDu"));
        addFieldIfNotNull(player, "fixedBoard", row.get("Fixe"));

        // Boolean fields
        Object pointeObj = row.get("Pointe");
        if (pointeObj instanceof Boolean) {
            player.put("checkedIn", pointeObj);
        }
        
        // Contact information
        addFieldIfNotNull(player, "address", row.get("Adresse"));
        addFieldIfNotNull(player, "postalCode", row.get("CP"));
        addFieldIfNotNull(player, "phone", row.get("Tel"));
        addFieldIfNotNull(player, "email", row.get("EMail"));
        addFieldIfNotNull(player, "comment", row.get("Commentaire"));
        
        // Round results - using dictionary with round number as key
        Map<String, Map<String, Object>> rounds = new HashMap<>();
        for (int roundNum = 1; roundNum <= 24; roundNum++) {
            String roundStr = String.format("%02d", roundNum);
            
            Object colorObj = row.get("Rd" + roundStr + "Cl");
            Object opponentObj = row.get("Rd" + roundStr + "Adv");
            Object resultObj = row.get("Rd" + roundStr + "Res");
            
            // Only include rounds that have non-default values
            boolean hasColor = colorObj != null && !"R".equals(colorObj.toString());
            boolean hasOpponent = opponentObj != null && ((Number)opponentObj).intValue() > 0;
            boolean hasResult = resultObj != null && ((Number)resultObj).intValue() > 0;
            
            if (hasColor || hasOpponent || hasResult) {
                
                Map<String, Object> round = new HashMap<>();
                
                if (colorObj != null && !"R".equals(colorObj.toString())) {
                    round.put("color", colorObj.toString());
                }
                
                if (opponentObj != null && !Integer.valueOf(0).equals(opponentObj)) {
                    int papiOpponent = ((Number)opponentObj).intValue();
                    if (papiOpponent > 1) {  // Exclude EXEMPT player (ref 1) from JSON output
                        // Use mapping from PAPI reference to JSON index
                        if (papiRefToJsonIndexMap.containsKey(papiOpponent)) {
                            int jsonOpponent = papiRefToJsonIndexMap.get(papiOpponent);
                            round.put("opponent", jsonOpponent);
                        }
                        else {
                            throw new Exception("Opponent reference " + papiOpponent + " not found in mapping");
                        }
                    }
                }
                
                if (resultObj != null && !Integer.valueOf(0).equals(resultObj)) {
                    round.put("result", resultObj);
                }
                
                // Add round to dictionary with round number as key
                rounds.put(String.valueOf(roundNum), round);
            }
        }
        
        if (!rounds.isEmpty()) {
            player.put("rounds", rounds);
        }
        
        return player;
    }
    
    /**
     * Helper method to process round data for both array and dictionary formats.
     */
    private static void processRoundData(Map<String, Object> rowData, JsonNode roundNode, int roundNum, int playerRef, Table playerTable) throws Exception {
        String roundStr = String.format("%02d", roundNum);
        
        // Color (Cl) - B/N/R/F
        if (roundNode.has("color")) {
            rowData.put("Rd" + roundStr + "Cl", roundNode.get("color").asText());
        }
        
        // Get result first to check for bye
        int result = 0;
        if (roundNode.has("result")) {
            result = roundNode.get("result").asInt();
            rowData.put("Rd" + roundStr + "Res", result);
        }
        
        // Opponent (Adv) - opponent player reference
        if (roundNode.has("opponent")) {
            int jsonOpponent = roundNode.get("opponent").asInt();
            if (jsonOpponent >= 0) {
                // Convert JSON opponent reference to PAPI reference
                int papiOpponent = jsonRefToPapiRef(jsonOpponent);
                rowData.put("Rd" + roundStr + "Adv", papiOpponent);
            }
        } else if (result == 6) {
            // Auto-detect bye: result 6 without opponent means bye against EXEMPT (player 1)
            rowData.put("Rd" + roundStr + "Adv", 1);
            System.out.println("    Auto-detected bye for player " + papiRefToJsonRef(playerRef) + " in round " + roundNum + " (vs EXEMPT)");

            // Find the EXEMPT player (Ref=1)
            Row exemptRow = null;
            for (Row row : playerTable) {
                Object refObj = row.get("Ref");
                if (refObj != null && ((Number)refObj).intValue() == 1) {
                    exemptRow = row;
                    break;
                }
            }

            if (exemptRow != null) {
                // Now set the new values
                exemptRow.put("Rd" + roundStr + "Cl", "N");
                exemptRow.put("Rd" + roundStr + "Adv", playerRef);
                exemptRow.put("Rd" + roundStr + "Res", 0);
                
                // Update the EXEMPT row in the table
                playerTable.updateRow(exemptRow);
            }
        }
    }
    
    /**
     * Helper method to set a field if it exists in the JSON node.
     */
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
    
    /**
     * Helper method to add a field if it's not null.
     */
    private static void addFieldIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null && !value.toString().trim().isEmpty()) {
            map.put(key, value);
        }
    }
}
