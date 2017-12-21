package com.samepage.maven;

import lombok.Builder;
import lombok.Data;
import org.apache.maven.model.Dependency;

import java.util.Optional;

@Data
@Builder
public class SimpleDependency {
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private String type;

    public String toString() {
        return String.format(
            "<depedency>\n" +
            "\t<groupId>%s</groupId>\n" +
            "\t<artifactId>%s</artifactId>\n" +
            "\t<version>%s</version>\n" +
            "\t<type>%s</type>\n" +
            "\t<scope>%s</scope>\n" +
            "</dependency>",
            this.getGroupId(),
            this.getArtifactId(),
            this.getVersion(),
            this.getType(),
            this.getScope()
        );
    }

    public String getVersionPropertyName() {
        return String.format("version.%s", this.getVersion());
    }

    public Dependency asMavenDependency(boolean useVersionProperty) {
        Dependency ret = new Dependency();
        ret.setGroupId(this.getGroupId());
        ret.setArtifactId(this.getArtifactId());
        ret.setVersion(useVersionProperty ? String.format("${%s}", this.getVersionPropertyName()) : this.getVersion());
        ret.setType(this.getType());
        ret.setScope(this.getScope());

        return ret;
    }

    public Dependency asShortMavenDependency() {
        Dependency ret = new Dependency();
        ret.setGroupId(this.getGroupId());
        ret.setArtifactId(this.getArtifactId());
        return ret;
    }

    public static Optional<SimpleDependency> from(String depStr) {
        if (depStr == null || depStr.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = depStr.split(":");
        if (parts.length < 3) {
            return Optional.empty();
        }
        String g = parts[0];
        String a = parts[1];
        String v = parts[2];
        String t = "jar";
        String s = "compile";

        // g:a:v:s or g:a:v:t:s
        if (parts.length == 4) {
            s = parts[3];
        } else if (parts.length == 5) {
            t = parts[3];
            s = parts[4];
        }
        return Optional.of(new SimpleDependencyBuilder()
            .groupId(g)
            .artifactId(a)
            .version(v)
            .type(t)
            .scope(s).build());
    }


}
