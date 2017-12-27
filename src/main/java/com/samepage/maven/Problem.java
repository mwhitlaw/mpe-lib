package com.samepage.maven;

import lombok.Data;
import org.apache.maven.model.Dependency;

@Data
public class Problem {
    private final Type type;
    private final Dependency dependency;
    private final String location;
    private String info;

    public enum Type {
        DEP_WITH_VERSION,
        DEP_WITH_EXCLUSION,
        DEP_WITHOUT_SCOPE,
        DEP_WITHOUT_DMDEP
    }


}
