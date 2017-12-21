package com.samepage.maven;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;


@Slf4j
public class PomManager {
    private Model originalPom;
    private Model workingPom;
    @Getter
    private String sourceFilename;
    @Getter
    private String targetFilename;


    public PomManager(String sourceFilename, String targetFilename)
        throws IOException, XmlPullParserException {
        this.load(new FileInputStream(new File(sourceFilename)));
        this.sourceFilename = sourceFilename;
        this.targetFilename = targetFilename;
    }

    public PomManager(InputStream inputStream)
        throws IOException, XmlPullParserException {
        this.load(inputStream);
    }

    private void load(InputStream inputStream)
        throws IOException, XmlPullParserException {
        this.originalPom = new MavenXpp3Reader().read(inputStream);
        this.workingPom = this.originalPom.clone();
    }

    public void save() throws IOException {
        save(true);
    }

    public void save(boolean createBackupOnReplace) throws IOException {

        if (this.targetFilename != null) {

            if (createBackupOnReplace &&
                this.sourceFilename != null &&
                getSourceFilename().equalsIgnoreCase(getTargetFilename())) {

                PomManager.outputPom(
                    new FileWriter(
                        new File(this.getBackupFilename())
                    ),
                    this.originalPom,
                    false
                );
            }

            PomManager.outputPom(
                new FileWriter(
                    new File(this.getTargetFilename())
                ),
                this.workingPom,
                true
            );
        } else {
            log.warn("Call to save without target filename specified");
        }
    }

    public void printOriginal() throws IOException {
        printOriginal(System.out);
    }

    public void printOriginal(OutputStream outputStream) throws IOException {
        PomManager.outputPom(outputStream, this.originalPom, false);
    }

    public void printWorking() throws IOException {
        printWorking(System.out);
    }

    public void printWorking(OutputStream outputStream) throws IOException {
        PomManager.outputPom(outputStream, this.workingPom, true);
    }

    private static void outputPom(final Writer writer, final Model model, final boolean sort) throws IOException {
        if (sort) PomManager.sort(model);
        new MavenXpp3Writer().write(writer, model);
    }

    private static void outputPom(final OutputStream out, final Model model, final boolean sort) throws IOException {
        if (sort) PomManager.sort(model);

        new MavenXpp3Writer().write(out, model);
    }


    public void addDependency(String depStr) {
        SimpleDependency.from(depStr).ifPresent(this::addDependency);
    }

    public void addDependency(SimpleDependency source) {
        if (source != null) {
            this.workingPom.addProperty(source.getVersionPropertyName(), source.getVersion());
            this.workingPom.getDependencyManagement().addDependency(source.asMavenDependency(true));
            this.workingPom.addDependency(source.asShortMavenDependency());
        }

    }

    private String getBackupFilename() {
        return String.format("%s_prev.xml",
            this.sourceFilename.substring(0, this.sourceFilename.lastIndexOf("."))
        );
    }

    private static void sort(final Model targetPom) {
        // sort the properties
        targetPom.setProperties(new SortedProperties(targetPom.getProperties()));

        // sort the dependencyManagement dependencies
        targetPom.getDependencyManagement().getDependencies().sort((o1, o2) -> {
            int compareResult = o1.getScope().compareTo(o2.getScope());
            return sortDependencies(o1, o2, compareResult);
        });

        // sort the dependencies
        targetPom.getDependencies().sort((o1, o2) -> {
            String effScope1 = getEffectiveScope(o1, targetPom);
            String effScope2 = getEffectiveScope(o2, targetPom);
            int compareResult = effScope1.compareTo(effScope2);
            return sortDependencies(o1, o2, compareResult);
        });

    }

    private static String getEffectiveScope(final Dependency dep, final Model pomModel) {
        if (dep == null) return "";
        if (dep.getScope() != null) return dep.getScope();

        Dependency dmDep = pomModel.getDependencyManagement().getDependencies()
            .stream()
            .filter(d ->
                dep.getGroupId().equalsIgnoreCase(d.getGroupId()) &&
                    dep.getArtifactId().equalsIgnoreCase(d.getArtifactId())
            )
            .findAny()
            .orElse(null);
        if (dmDep != null) {
            return dmDep.getScope();
        } else {
            return "";
        }
    }

    private static int sortDependencies(final Dependency o1, final Dependency o2, int compareResult) {
        if (compareResult == 0) {
            compareResult = o1.getGroupId().compareTo(o2.getGroupId());
            if (compareResult == 0) {
                return o1.getArtifactId().compareTo(o2.getArtifactId());
            } else {
                return compareResult;
            }
        } else {
            return compareResult;
        }
    }

}
