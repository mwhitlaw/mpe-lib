package com.samepage.maven;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.ExceptionUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class PomMangerTest {

    @Test
    public void addDependencyToRootPomTest() {
        try {
            InputStream inputStream = PomMangerTest.class.getResourceAsStream("/rootpom.xml");
            assertNotNull(inputStream);
            PomManager pomManager = new PomManager(inputStream);
            assertNotNull(pomManager);
            pomManager.addDependency("blah.root:root-yada:1.7:provided");
            assertTrue(
                pomManager.getWorkingPom().getProperties().size() - pomManager.getOriginalPom().getProperties().size() == 1
            );
            assertTrue(
                pomManager.getWorkingPom().getDependencyManagement().getDependencies().size()
                    -
                    pomManager.getOriginalPom().getDependencyManagement().getDependencies().size()
                    == 1
            );
            assertTrue(
                pomManager.getWorkingPom().getDependencies().size()
                    -
                    pomManager.getOriginalPom().getDependencies().size()
                    == 1
            );
            pomManager.printWorking();
        } catch (IOException | XmlPullParserException e) {
            log.error(ExceptionUtils.getRootCause(e).getLocalizedMessage());
        }

    }

    @Test
    public void addDependencyToModulePomTest() {
        try {
            InputStream inputStream = PomMangerTest.class.getResourceAsStream("/modulepom.xml");
            assertNotNull(inputStream);
            PomManager pomManager = new PomManager(inputStream);
            assertNotNull(pomManager);
            pomManager.addDependency("blah.module:module-yada:1.14:runtime");
            assertTrue(
                pomManager.getWorkingPom().getDependencies().size()
                    -
                    pomManager.getOriginalPom().getDependencies().size()
                    == 1
            );
            pomManager.printWorking();
        } catch (IOException | XmlPullParserException e) {
            log.error(ExceptionUtils.getRootCause(e).getLocalizedMessage());
        }

    }
}
