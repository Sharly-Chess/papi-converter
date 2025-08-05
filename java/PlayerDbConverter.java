package org.sharlychess.papiconverter;

import java.sql.*;
import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import com.healthmarketscience.jackcess.*;

/**
 * Converts Access player database to SQLite format.
 * Reads from Data.mdb and outputs to SQLite with the specified schema.
 */
public class PlayerDbConverter {
    
    /**
     * Helper class to store club information.
     */
    private static class ClubInfo {
        final String name;
        final String ligue;
        final String commune;
        
        ClubInfo(String name, String ligue, String commune) {
            this.name = name != null ? name : "";
            this.ligue = ligue != null ? ligue : "";
            this.commune = commune != null ? commune : "";
        }
    }
    
    private static final String SQLITE_SCHEMA = """
        CREATE TABLE player (
            id INTEGER NOT NULL AUTO_INCREMENT,
            ffe_id INTEGER NOT NULL,
            last_name VARCHAR(255) NOT NULL,
            first_name VARCHAR(255),
            gender INTEGER NOT NULL,
            ffe_licence_number VARCHAR(255),
            ffe_licence INTEGER NOT NULL,
            federation VARCHAR(10) NOT NULL,
            league VARCHAR(255),
            city VARCHAR(255),
            club VARCHAR(255),
            fide_id INTEGER,
            fide_title INTEGER NOT NULL,
            standard_rating INTEGER NOT NULL,
            rapid_rating INTEGER NOT NULL,
            blitz_rating INTEGER NOT NULL,
            standard_rating_type INTEGER NOT NULL,
            rapid_rating_type INTEGER NOT NULL,
            blitz_rating_type INTEGER NOT NULL,
            date_of_birth VARCHAR(10),
            PRIMARY KEY(id)
        );
        """;
    
    public static void convert(String inputFile, String outputFile) throws Exception {
        if (outputFile == null) {
            outputFile = inputFile.replaceFirst("\\.[^.]+$", ".sqlite");
        }
        
        System.out.println("Converting Access player database to SQLite...");
        System.out.println("Input: " + inputFile);
        System.out.println("Output: " + outputFile);
        
        // Load H2 JDBC driver for SQLite compatibility
        Class.forName("org.h2.Driver");
        
        // Create parent directories if they don't exist and delete existing output file
        File outFile = new File(outputFile);
        File parentDir = outFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new Exception("Failed to create directory: " + parentDir.getAbsolutePath());
            }
            System.out.println("Created directory: " + parentDir.getAbsolutePath());
        }
        
        if (outFile.exists()) {
            outFile.delete();
        }
        
        // Open Access database
        Database accessDb = DatabaseBuilder.open(new File(inputFile));
        
        // Create H2 connection in SQLite mode with optimizations
        String h2Url = "jdbc:h2:" + outputFile.replaceFirst("\\.sqlite$", "");
        Connection sqliteConn = DriverManager.getConnection(h2Url);
        
        // Note: Using H2 database in embedded mode for better compatibility
        
        // Disable auto-commit for batching
        sqliteConn.setAutoCommit(false);
        
        try {
            // Create SQLite table
            Statement stmt = sqliteConn.createStatement();
            stmt.execute(SQLITE_SCHEMA);
            
            // Get the JOUEUR and CLUB tables
            Table playerTable = accessDb.getTable("JOUEUR");
            Table clubTable = accessDb.getTable("CLUB");
            
            if (playerTable == null) {
                throw new Exception("JOUEUR table not found in database.");
            }
            if (clubTable == null) {
                throw new Exception("CLUB table not found in database.");
            }
            
            // Print column names for debugging
            System.out.println("\nColumns in JOUEUR table:");
            for (Column col : playerTable.getColumns()) {
                System.out.println("  " + col.getName() + " (" + col.getType() + ")");
            }
            
            System.out.println("\nColumns in CLUB table:");
            for (Column col : clubTable.getColumns()) {
                System.out.println("  " + col.getName() + " (" + col.getType() + ")");
            }
            
            // Build club lookup map with all club data
            System.out.println("\nBuilding club lookup map...");
            Map<Long, ClubInfo> clubMap = new HashMap<>();
            for (Row clubRow : clubTable) {
                Object refObj = clubRow.get("Ref");
                Object nomObj = clubRow.get("Nom");
                Object ligueObj = clubRow.get("Ligue");
                Object communeObj = clubRow.get("Commune");
                if (refObj != null && nomObj != null) {
                    long clubRef = ((Number) refObj).longValue();
                    String clubName = nomObj != null ? nomObj.toString().trim() : "";
                    String clubLigue = ligueObj != null ? ligueObj.toString().trim() : "";
                    String clubCommune = communeObj != null ? communeObj.toString().trim() : "";
                    clubMap.put(clubRef, new ClubInfo(clubName, clubLigue, clubCommune));
                }
            }
            System.out.println("Loaded " + clubMap.size() + " clubs.");
            
            // Prepare SQLite insert statement
            String insertSql = """
                INSERT INTO player (
                    ffe_id, last_name, first_name, gender, ffe_licence_number, ffe_licence,
                    federation, league, city, club, fide_id, fide_title,
                    standard_rating, rapid_rating, blitz_rating,
                    standard_rating_type, rapid_rating_type, blitz_rating_type, date_of_birth
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            PreparedStatement insertStmt = sqliteConn.prepareStatement(insertSql);
            
            int playerCount = 0;
            int batchSize = 1000; // Process in batches of 1000
            System.out.println("\nConverting players with batch processing (batch size: " + batchSize + ")...");
            
            // Process each player row with batch processing
            for (Row row : playerTable) {
                try {
                    // Get club information from ClubRef
                    ClubInfo clubInfo = null;
                    Object clubRefObj = row.get("ClubRef");
                    if (clubRefObj != null) {
                        long clubRef = ((Number) clubRefObj).longValue();
                        clubInfo = clubMap.get(clubRef);
                    }
                    
                    String clubName = clubInfo != null ? clubInfo.name : "";
                    String clubLigue = clubInfo != null ? clubInfo.ligue : "";
                    String clubCommune = clubInfo != null ? clubInfo.commune : "";
                    
                    // Map Access fields to SQLite fields using actual column names
                    insertStmt.setInt(1, getIntValue(row, "Ref")); // ffe_id (from Ref)
                    insertStmt.setString(2, getStringValue(row, "Nom")); // last_name
                    insertStmt.setString(3, getStringValue(row, "Prenom")); // first_name
                    insertStmt.setInt(4, getGenderAsInt(row.get("Sexe"))); // gender (M=1, F=2, other=0)
                    insertStmt.setString(5, getStringValue(row, "NrFFE")); // ffe_licence_number
                    insertStmt.setInt(6, getLicenceType(row.get("AffType"))); // ffe_licence (from AffType)
                    insertStmt.setString(7, getStringValue(row, "Federation")); // federation
                    insertStmt.setString(8, clubLigue); // league (from club lookup)
                    insertStmt.setString(9, clubCommune); // city (commune from club lookup)
                    insertStmt.setString(10, clubName); // club (from club lookup)
                    insertStmt.setObject(11, getFideIdFromCode(row.get("FideCode"))); // fide_id
                    insertStmt.setInt(12, getFideTitleAsInt(row.get("FideTitre"))); // fide_title
                    insertStmt.setInt(13, getIntValue(row, "Elo")); // standard_rating
                    insertStmt.setInt(14, getIntValue(row, "Rapide")); // rapid_rating
                    insertStmt.setInt(15, getIntValue(row, "Blitz")); // blitz_rating
                    insertStmt.setInt(16, getRatingType(row.get("Fide"))); // standard_rating_type
                    insertStmt.setInt(17, getRatingType(row.get("RapideFide"))); // rapid_rating_type
                    insertStmt.setInt(18, getRatingType(row.get("BlitzFide"))); // blitz_rating_type
                    
                    // Handle birth date
                    String birthDate = getDateAsString(row, "NeLe");
                    insertStmt.setObject(19, birthDate); // date_of_birth
                    
                    // Add to batch instead of executing immediately
                    insertStmt.addBatch();
                    playerCount++;
                    
                    // Execute batch when it reaches the batch size
                    if (playerCount % batchSize == 0) {
                        insertStmt.executeBatch();
                        sqliteConn.commit(); // Commit the batch
                        System.out.println("  Converted " + playerCount + " players...");
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error converting player row " + (playerCount + 1) + ": " + e.getMessage());
                    e.printStackTrace();
                    // Continue with next player
                }
            }
            
            // Execute any remaining batch items
            if (playerCount % batchSize != 0) {
                insertStmt.executeBatch();
                sqliteConn.commit();
            }
            
            System.out.println("\\nConversion completed successfully!");
            System.out.println("Total players converted: " + playerCount);
            
        } finally {
            accessDb.close();
            sqliteConn.close();
        }
    }
    
    /**
     * Gets string value from row for a specific column.
     */
    private static String getStringValue(Row row, String columnName) {
        try {
            Object value = row.get(columnName);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        } catch (Exception e) {
            // Column doesn't exist or error reading
        }
        return ""; // Return empty string for required TEXT fields
    }
    
    /**
     * Gets integer value from row for a specific column.
     */
    private static int getIntValue(Row row, String columnName) {
        try {
            Object value = row.get(columnName);
            if (value != null) {
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                } else if (value instanceof String) {
                    String str = value.toString().trim();
                    if (!str.isEmpty()) {
                        return Integer.parseInt(str);
                    }
                } else if (value instanceof Boolean) {
                    return ((Boolean) value) ? 1 : 0;
                }
            }
        } catch (Exception e) {
            // Column doesn't exist or invalid value
        }
        return 0; // Return 0 for required INTEGER fields
    }
    
    /**
     * Converts gender string to integer
     */
    private static int getGenderAsInt(Object sexeObj) {
        if (sexeObj == null) return 0;
        String sexe = sexeObj.toString().trim();
        
        switch (sexe.toUpperCase()) {
            case "F": return 1;
            case "M": return 2;
            default: return 0;
        }
    }
    
    /**
     * Converts licence type to integer
     */
    private static int getLicenceType(Object affTypeObj) {
        if (affTypeObj == null) return 0; // NONE
        String affType = affTypeObj.toString().trim().toUpperCase();
        if (affType.isEmpty()) return 0; // NONE
        
        switch (affType) {
            case "N": return 1;
            case "A": return 2;
            case "B": return 3;
            default: return 0;
        }
    }
    
    /**
     * Extracts FIDE ID from FideCode, handling quotes and spaces.
     */
    private static Integer getFideIdFromCode(Object fideCodeObj) {
        if (fideCodeObj == null) return null;
        String fideCode = fideCodeObj.toString().trim();
        if (fideCode.isEmpty() || "0".equals(fideCode)) return null;
        
        // Remove surrounding quotes and extra spaces
        fideCode = fideCode.replaceAll("^['\"]+|['\"]+$", "").trim();
        if (fideCode.isEmpty() || "0".equals(fideCode)) return null;
        
        try {
            int fideId = Integer.parseInt(fideCode);
            return fideId == 0 ? null : fideId;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Converts FIDE title to integer using Access database codes.
     */
    private static int getFideTitleAsInt(Object fideTitreObj) {
        if (fideTitreObj == null) return 0; // NONE
        String titre = fideTitreObj.toString().trim().toLowerCase();
        if (titre.isEmpty()) return 0; // NONE
        
        switch (titre) {
            case "ff": return 3;  // WOMAN_FIDE_MASTER
            case "f": return 4;   // FIDE_MASTER
            case "mf": return 5;  // WOMAN_INTERNATIONAL_MASTER
            case "m": return 6;   // INTERNATIONAL_MASTER
            case "gf": return 7;  // WOMAN_GRANDMASTER
            case "g": return 8;   // GRANDMASTER
            default: return 0;    // NONE (unknown titles)
        }
    }
    
    /**
     * Converts rating type
     */
    private static int getRatingType(Object ratingTypeObj) {
        if (ratingTypeObj == null) return 1; // Empty = 1
        String type = ratingTypeObj.toString().trim().toUpperCase();
        
        switch (type) {
            case "E": return 1; // Estimated rating
            case "N": return 2; // National rating
            case "F": return 3; // FIDE rating
            default: return 1; // Default to estimated
        }
    }
    
    /**
     * Gets date value as YYYY-MM-DD string from row to match ffe.db format.
     * Handles LocalDateTime objects from Access database.
     */
    private static String getDateAsString(Row row, String columnName) {
        try {
            Object value = row.get(columnName);
            if (value != null) {
                if (value instanceof LocalDateTime) {
                    LocalDateTime localDateTime = (LocalDateTime) value;
                    // Convert to YYYY-MM-DD string format (like Python's date() does)
                    return localDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);

                    // Convert to Unix timestamp (seconds since epoch)
                    // long epochSeconds = localDateTime.atZone(ZoneId.of("UTC")).toEpochSecond();
                    // return (int) epochSeconds;

                }
            }
        } catch (Exception e) {
            System.err.println("Error processing date for column " + columnName + ": " + e.getMessage());
        }
        return null;
    }
}
