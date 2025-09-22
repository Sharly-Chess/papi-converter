package org.sharlychess.papiconverter;

import java.io.File;
import java.net.URI;
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
        VerboseOutput.println("Converting JSON to MDB...");
        
        // Generate output filename if not provided
        if (mdbFile == null) {
            mdbFile = jsonFile.replaceAll("\\.json$", ".papi");
        }
        
        // Read and parse JSON content
        String jsonContent = Files.readString(Paths.get(jsonFile));
        VerboseOutput.println("Reading JSON from: " + jsonFile);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonContent);
        
        // Check for template file - make path relative to executable location
        String executableDir = getExecutableDirectory();
        String templateFile = findTemplateFile(executableDir);
        if (templateFile == null) {
            throw new Exception("Template file not found. Searched in: " + executableDir + "/static/ and parent directories");
        }
        
        // Copy template to output location
        VerboseOutput.println("Copying template file: " + templateFile);
        
        // Create parent directories if they don't exist
        File outputFile = new File(mdbFile);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new Exception("Failed to create directory: " + parentDir.getAbsolutePath());
            }
            VerboseOutput.println("Created directory: " + parentDir.getAbsolutePath());
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
        
        VerboseOutput.alwaysPrintln("Output MDB file: " + mdbFile);
        VerboseOutput.alwaysPrintln("JSON to MDB conversion completed successfully!");
    }
    
    /**
     * Gets the directory where the JAR/executable is located.
     * @return The directory path of the JAR/executable
     */
    private static String getExecutableDirectory() {
        try {
            // Get the location of the current class
            URI jarUri = JsonToPapiConverter.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI();
            
            // Normalize the path to handle different platforms and URI schemes
            String jarPath;
            if ("file".equals(jarUri.getScheme())) {
                jarPath = Paths.get(jarUri).toString();
            } else {
                jarPath = jarUri.getPath();
            }
            
            File jarFile = new File(jarPath);
            if (jarFile.isFile()) {
                // This is a JAR file - return the directory containing the JAR
                return jarFile.getParent();
            } else {
                // This might be a directory (development scenario)
                return jarPath;
            }
        } catch (Exception e) {
            // Fallback to current working directory if we can't determine executable location
            VerboseOutput.errorPrintln("Warning: Could not determine executable directory, using current directory");
            return System.getProperty("user.dir");
        }
    }
    
    /**
     * Finds the template file by searching in different possible locations.
     * This handles different distribution structures:
     * - Development/Mac: static folder next to JAR/executable
     * - Windows: JAR in dist/java/, static folder at distribution root
     * 
     * @param executableDir The directory where the JAR/executable is located
     * @return The full path to the template file, or null if not found
     */
    private static String findTemplateFile(String executableDir) {
        String[] searchPaths = {
            // First try: static folder next to JAR/executable (development, Mac native)
            Paths.get(executableDir, "static", "template-3.3.8.papi").toString(),
            
            // Second try: go up one level from JAR location (Windows: dist/java -> dist)
            Paths.get(executableDir).getParent() != null ?
                Paths.get(executableDir).getParent().resolve("static").resolve("template-3.3.8.papi").toString() : null,
            
            // Third try: go up two levels from JAR location (dist/java -> dist -> root)
            Paths.get(executableDir).getParent() != null && Paths.get(executableDir).getParent().getParent() != null ?
                Paths.get(executableDir).getParent().getParent().resolve("static").resolve("template-3.3.8.papi").toString() : null,
            
            // Fallback: current working directory
            Paths.get(System.getProperty("user.dir"), "static", "template-3.3.8.papi").toString()
        };
        
        for (String path : searchPaths) {
            if (path != null && Files.exists(Paths.get(path))) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * Maximum length for INFO table Value column (based on MDB schema)
     */
    private static final int MAX_INFO_VALUE_LENGTH = 50;
    
    /**
     * Trims a string to the maximum allowed length for INFO table values.
     * @param value The string to trim
     * @return The trimmed string, or null if input is null
     */
    private static String trimToMaxLength(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= MAX_INFO_VALUE_LENGTH) {
            return value;
        }
        String trimmed = value.substring(0, MAX_INFO_VALUE_LENGTH);
        VerboseOutput.alwaysPrintln("  Warning: Trimmed value from " + value.length() + " to " + MAX_INFO_VALUE_LENGTH + " characters: '" + value + "' -> '" + trimmed + "'");
        return trimmed;
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
            VerboseOutput.println("Updating INFO table with variables...");
            
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
                    // Trim the value to ensure it fits in the database field
                    String trimmedValue = trimToMaxLength(value);
                    
                    Row existingRow = existingRows.get(frenchVariable);
                    if (existingRow != null) {
                        // Overwrite existing row
                        existingRow.put("Value", trimmedValue);
                        infoTable.updateRow(existingRow);
                        VerboseOutput.println("  Updated: " + englishVariable + " (" + frenchVariable + ") = " + trimmedValue);
                    } else {
                        // Add new row
                        infoTable.addRow(frenchVariable, trimmedValue);
                        VerboseOutput.println("  Added: " + englishVariable + " (" + frenchVariable + ") = " + trimmedValue);
                    }
                } else {
                    VerboseOutput.alwaysPrintln("  Warning: Skipping invalid variable: " + englishVariable);
                }
            }
        } else {
            VerboseOutput.println("No 'variables' object found in JSON");
        }
    }
    
    /**
     * Processes players data from JSON and updates the JOUEUR table.
     */
    private static void processPlayers(Database db, JsonNode rootNode) throws Exception {
        JsonNode playersNode = rootNode.get("players");
        if (playersNode != null && playersNode.isArray()) {
            VerboseOutput.println("\nProcessing players data...");
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
            
            VerboseOutput.println("Added " + (playerRef - 2) + " players to JOUEUR table");
        } else {
            VerboseOutput.println("No 'players' array found in JSON");
        }
    }
}
