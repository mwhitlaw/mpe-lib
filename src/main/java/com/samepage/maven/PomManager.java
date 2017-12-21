package com.samepage.maven;

import lombok.Getter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class PomManager {
    @Getter
    private Model originalPom;
    @Getter
    private Model workingPom;

    public PomManager(InputStream inputStream)
        throws IOException, XmlPullParserException {
        this.load(inputStream);
    }

    private void load(InputStream inputStream)
        throws IOException, XmlPullParserException {
        this.originalPom = new MavenXpp3Reader().read(inputStream);
        this.workingPom = this.originalPom.clone();
    }

    public void printOriginal() throws IOException {
        printOriginal(System.out);
    }

    public void printOriginal(OutputStream outputStream) throws IOException {
        this.outputPom(outputStream, this.originalPom, false);
    }

    public void printWorking() throws IOException {
        printWorking(System.out);
    }

    public void printWorking(OutputStream outputStream) throws IOException {
        this.outputPom(outputStream, this.workingPom, true);
    }

    private void outputPom(final OutputStream out, final Model model, final boolean sort) throws IOException {
        if (sort) PomManager.sort(model);
        new MavenXpp3Writer().write(out, model);
    }

    public void addDependency(String depStr) {
        SimpleDependency.from(depStr).ifPresent(this::addDependency);
    }

    public void addDependency(SimpleDependency source) {
        if (source != null) {

            if (this.workingPom.getDependencyManagement() != null) {
                this.workingPom.addProperty(source.getVersionPropertyName(), source.getVersion());
                this.workingPom.getDependencyManagement().addDependency(source.asDepManDependency());
            }

            this.workingPom.addDependency(source.asDepDependency());
        }

    }

    private static void sort(final Model pomModel) {
        // sort the properties by replacing the Properties object with
        // a SortedProperties object tha is a clone of the original
        if (pomModel.getProperties() != null) {
            pomModel.setProperties(new SortedProperties(pomModel.getProperties()));
        }

        // sort the dependencyManagement dependencies
        if (pomModel.getDependencyManagement() != null) {
            pomModel.getDependencyManagement().getDependencies().sort((o1, o2) -> sortDependencies(o1, o2, 0));
        }

        // sort the dependencies
        if (pomModel.getDependencies() != null) {
            pomModel.getDependencies().sort((o1, o2) -> {
                String effScope1 = getEffectiveScope(o1, pomModel);
                String effScope2 = getEffectiveScope(o2, pomModel);
                int compareResult = effScope1.compareTo(effScope2);
                return sortDependencies(o1, o2, compareResult);
            });
        }

    }

    private static String getEffectiveScope(final Dependency dep, final Model pomModel) {
        if (dep == null) return "";
        if (dep.getScope() != null) return dep.getScope();
        if (pomModel.getDependencyManagement() == null) return "compile";

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

    private static int sortDependencies(final Dependency o1, final Dependency o2, int initialCompare) {
        if (initialCompare == 0) {
            int groupCompare = o1.getGroupId().compareTo(o2.getGroupId());
            if (groupCompare == 0) {
                return o1.getArtifactId().compareTo(o2.getArtifactId());
            } else {
                return groupCompare;
            }
        } else {
            return initialCompare;
        }
    }

}
