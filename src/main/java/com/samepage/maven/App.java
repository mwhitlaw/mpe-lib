package com.samepage.maven;

import lombok.experimental.var;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class App {
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        options.addOption("a", "add", true, "Adds a dependency to a pom");

        try (InputStream inputStream = getInput()){
            var cmdLine = parser.parse(options, args);
            var pomManager = new PomManager(inputStream);
            if (cmdLine.hasOption("add")) {
                String depToAdd = cmdLine.getOptionValue("add");
                if (depToAdd != null && !depToAdd.isEmpty()) {
                    pomManager.addDependency(depToAdd);
                    pomManager.printWorking();
                }
            }
        } catch (ParseException | IOException | XmlPullParserException e) {
            System.err.println(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private static InputStream getInput() throws FileNotFoundException {
        File pomFile = new File("pom.xml");
        if (pomFile.exists()) {
            return new FileInputStream(pomFile);
        } else {
            return System.in;
        }
    }


}
