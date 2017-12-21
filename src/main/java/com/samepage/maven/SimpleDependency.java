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
        return String.format("version.%s", this.getArtifactId());
    }


    public Dependency asDepManDependency() {
        Dependency ret = new Dependency();
        ret.setGroupId(this.getGroupId());
        ret.setArtifactId(this.getArtifactId());
        if ("war".equalsIgnoreCase(this.getType())) {
            ret.setType(this.getType());
        }
        ret.setVersion(String.format("${version.%s}", this.getArtifactId()));
        return ret;
    }

    public Dependency asDepDependency() {
        Dependency ret = new Dependency();
        ret.setGroupId(this.getGroupId());
        ret.setArtifactId(this.getArtifactId());
        if ("war".equalsIgnoreCase(this.getType())) {
            ret.setType(this.getType());
        }
        ret.setVersion(String.format("${version.%s}", this.getArtifactId()));
        if (this.getScope() != null) {
            ret.setScope(this.getScope());
        } else {
            ret.setScope("compile");
        }
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
