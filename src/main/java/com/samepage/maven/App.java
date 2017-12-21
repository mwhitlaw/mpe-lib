package com.samepage.maven;

import lombok.experimental.var;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        options.addOption("a", "add", true, "Adds a dependency to a pom");
        options.addOption("i", "inputFile", true, "The pom.xml to manipulate");
        options.addOption("o", "outputFile", true, "The name of the file to write to.");


        try {
            var cmdLine = parser.parse(options, args);
            String sourceFilename = cmdLine.getOptionValue("inputFile");
            if (sourceFilename == null) {
                sourceFilename = "pom.xml";
            }
            String targetFilename = cmdLine.getOptionValue("outputFile");
            if (targetFilename == null) {
                targetFilename = "new_pom.xml";
            }

            var pomManager = new PomManager(sourceFilename, targetFilename);

            if (cmdLine.hasOption("add")) {
                String depToAdd = cmdLine.getOptionValue("add");

                if (depToAdd != null && !depToAdd.isEmpty()) {
                    pomManager.addDependency(depToAdd);
                    System.out.println("Original START");
                    pomManager.printOriginal();
                    System.out.println("Original END");
                    System.out.println();
                    System.out.println("Working START");
                    pomManager.printWorking();
                    System.out.println("Working END");
                    pomManager.save();
                }
            }
        } catch (ParseException | IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }


}
