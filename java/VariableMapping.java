package org.sharlychess.papiconverter;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;

/**
 * Handles mapping between English variable names (used in JSON) 
 * and French variable names (used in PAPI database).
 */
public class VariableMapping {
    
    private static final Map<String, String> ENGLISH_TO_FRENCH = createEnglishToFrenchMapping();
    private static final Map<String, String> FRENCH_TO_ENGLISH = createFrenchToEnglishMapping();
    
    private static final List<String> VALID_FRENCH_VARIABLES = Arrays.asList(
        "Nom", "Genre", "NbrRondes", "Pairing", "Cadence", "ClassElo", 
        "EloBase1", "EloBase2", "Dep1", "Dep2", "Dep3", "DecomptePoints", 
        "Lieu", "DateDebut", "DateFin", "Arbitre", "Homologation"
    );
    
    /**
     * Creates the English to French variable mapping.
     */
    private static Map<String, String> createEnglishToFrenchMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("name", "Nom");
        mapping.put("type", "Genre");
        mapping.put("rounds", "NbrRondes");
        mapping.put("pairing", "Pairing");
        mapping.put("timeControl", "Cadence");
        mapping.put("ratingClass", "ClassElo");
        mapping.put("minRating", "EloBase1");
        mapping.put("maxRating", "EloBase2");
        mapping.put("tiebreak1", "Dep1");
        mapping.put("tiebreak2", "Dep2");
        mapping.put("tiebreak3", "Dep3");
        mapping.put("pointSystem", "DecomptePoints");
        mapping.put("venue", "Lieu");
        mapping.put("startDate", "DateDebut");
        mapping.put("endDate", "DateFin");
        mapping.put("arbiter", "Arbitre");
        mapping.put("homologation", "Homologation");
        return mapping;
    }
    
    /**
     * Creates the French to English variable mapping.
     */
    private static Map<String, String> createFrenchToEnglishMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : ENGLISH_TO_FRENCH.entrySet()) {
            mapping.put(entry.getValue(), entry.getKey());
        }
        return mapping;
    }
    
    /**
     * Maps an English variable name to its French equivalent.
     * @param englishName The English variable name
     * @return The French variable name, or null if no mapping exists
     */
    public static String englishToFrench(String englishName) {
        return ENGLISH_TO_FRENCH.get(englishName);
    }
    
    /**
     * Maps a French variable name to its English equivalent.
     * @param frenchName The French variable name
     * @return The English variable name, or null if no mapping exists
     */
    public static String frenchToEnglish(String frenchName) {
        return FRENCH_TO_ENGLISH.get(frenchName);
    }
    
    /**
     * Checks if a French variable name is valid for PAPI database.
     * @param frenchName The French variable name to check
     * @return true if the variable is valid, false otherwise
     */
    public static boolean isValidFrenchVariable(String frenchName) {
        return VALID_FRENCH_VARIABLES.contains(frenchName);
    }
    
    /**
     * Gets all English variable names.
     * @return A set of all English variable names
     */
    public static java.util.Set<String> getAllEnglishNames() {
        return ENGLISH_TO_FRENCH.keySet();
    }
    
    /**
     * Gets all French variable names.
     * @return A set of all French variable names
     */
    public static java.util.Set<String> getAllFrenchNames() {
        return FRENCH_TO_ENGLISH.keySet();
    }
}
