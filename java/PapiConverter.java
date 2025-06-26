package org.sharlychess.papiconverter;

import java.nio.file.Files;
import java.nio.file.Paths;

public class PapiConverter {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java MyTool <json-file>");
            System.exit(1);
        }
        String jsonContent = Files.readString(Paths.get(args[0]));
        System.out.println("Processing JSON: " + jsonContent);
        // Your MDB access logic here
    }
}
