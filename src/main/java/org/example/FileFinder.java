package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class FileFinder {
    static List<String> filenames = new ArrayList();

    public static void listFilesFromFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesFromFolder(fileEntry);
            } else {
                filenames.add(fileEntry.getName());
            }
        }
    }

    public static List<String> finder(String patternStr) {
        List<String> compatible = new ArrayList();

        for (int i = 0; i < filenames.size(); i++) {
            String file = filenames.get(i).toLowerCase();
            if (file.contains(patternStr))
                compatible.add(file);
        }
        return compatible;
    }
}
