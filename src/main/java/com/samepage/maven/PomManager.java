package com.samepage.maven;

import lombok.Getter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;


public class PomManager {
    @Getter
    private File dir;
    private Class loadingClass;
    @Getter
    private Model originalPom;
    @Getter
    private Model workingPom;
    @Getter
    private PomManager parent;
    @Getter
    private List<PomManager> children;

    public PomManager() throws XmlPullParserException, IOException, URISyntaxException {
        this("./", null, null);
    }

    public PomManager(String pomDir) throws XmlPullParserException, IOException, URISyntaxException {
        this(pomDir, null, null);
    }

    public PomManager(String pomDir, Class loadingClass) throws XmlPullParserException, IOException, URISyntaxException {
        this(pomDir, null, loadingClass);
    }

    public PomManager(String pomDir, PomManager parent) throws XmlPullParserException, IOException, URISyntaxException {
        this(pomDir, parent, parent.loadingClass);
    }


    public PomManager(String pomDir, PomManager parent, Class loadingClass) throws IOException, XmlPullParserException, URISyntaxException {
        this.parent = parent;
        this.loadingClass = loadingClass;

        if (this.loadingClass != null) {
            URL url = this.loadingClass.getResource(pomDir);
            if (url != null) {
                this.dir = new File(url.toURI());
            } else {
                StringBuilder pomDirAdj = new StringBuilder();
                String[] dirParts = pomDir.split("/");
                int level = getLevel() + 2;
                while (dirParts.length > 0 && level > 0) {
                    pomDirAdj.append("/").append(dirParts[dirParts.length - level]);
                    level--;
                }
                url = this.loadingClass.getResource(pomDirAdj.toString());
                if (url != null) {
                    this.dir = new File(url.toURI());
                }
            }
        } else {
            this.dir = new File(pomDir);
        }
        this.load();
    }

    private void load() throws IOException, XmlPullParserException, URISyntaxException {
        InputStream inputStream = new FileInputStream(new File(this.dir, "pom.xml"));
        this.originalPom = new MavenXpp3Reader().read(inputStream);
        this.workingPom = this.originalPom.clone();
        this.children = new LinkedList<>();
        if (this.workingPom.getModules() != null) {
            for (String moduleName : this.workingPom.getModules()) {
                File moduleDir = new File(this.dir, moduleName);
                PomManager modulePomManager = new PomManager(moduleDir.getPath(), this.loadingClass);
                this.children.add(modulePomManager);
            }
        }
    }

    public List<Problem> getProblems() {
        List<Problem> ret = new LinkedList<>();
        for (Dependency dependency : this.getWorkingPom().getDependencies()) {
            if (dependency.getVersion() != null && !dependency.getVersion().isEmpty()) {
                ret.add(new Problem(Problem.Type.DEP_WITH_VERSION, dependency, this.dir.toString()));
            }
            if (dependency.getExclusions() != null && dependency.getExclusions().size() > 0) {
                ret.add(new Problem(Problem.Type.DEP_WITH_EXCLUSION, dependency, this.dir.toString()));
            }
            if (dependency.getScope() == null || dependency.getScope().isEmpty()) {
                ret.add(new Problem(Problem.Type.DEP_WITHOUT_SCOPE, dependency, this.dir.toString()));
            }
            Dependency dmDep = getDmDep(dependency);
            if (dmDep == null) {
                ret.add(new Problem(Problem.Type.DEP_WITHOUT_DMDEP, dependency, this.dir.toString()));
            }
        }
        return ret;
    }

    public Dependency getDmDep(Dependency dep) {
        if (dep == null) return null;
        PomManager rootPm = this.getRoot();
        DependencyManagement dm = rootPm.getWorkingPom().getDependencyManagement();
        if (dm == null) {
            return null;
        }
        List<Dependency> rootDmDeps = dm.getDependencies();
        if (rootDmDeps == null || rootDmDeps.size() == 0) {
            return null;
        }
        Dependency ret = null;
        for (Dependency dmDep : rootDmDeps) {
            if (compareDeps(dmDep, dep) == 0) {
                ret = dmDep;
            }
        }
        return ret;
    }

    public int compareDeps(Dependency dep1, Dependency dep2) {
        if (dep1 == null && dep2 == null) return 0;
        if (dep1 == null) return -1;
        if (dep2 == null) return 1;

        int ret = dep1.getGroupId().compareTo(dep2.getGroupId());
        if (ret == 0) {
            return dep1.getArtifactId().compareTo(dep2.getArtifactId());
        } else {
            return ret;
        }
    }


    public boolean isRoot() {
        return this.parent == null;
    }

    public PomManager getRoot() {
        PomManager ret = this;
        while (!ret.isRoot()) {
            ret = ret.parent;
        }
        return ret;
    }

    public int getLevel() {
        int ret = 0;
        PomManager c = this;
        while (!c.isRoot()) {
            ret++;
            c = this.parent;
        }
        return ret;
    }

    public void printOriginal() throws IOException {
        printOriginal(System.out);
    }

    public void printOriginal(OutputStream outputStream) throws IOException {
        this.outputPom(outputStream, this.originalPom, false);
        for (PomManager child : this.children) {
            child.printWorking(outputStream);
        }
    }

    public void printWorking() throws IOException {
        printWorking(System.out);
    }

    public void printWorking(OutputStream outputStream) throws IOException {
        this.outputPom(outputStream, this.workingPom, true);
        for (PomManager child : this.children) {
            child.printWorking(outputStream);
        }
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
