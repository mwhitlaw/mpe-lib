package com.samepage.maven;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class SortedProperties extends Properties {

    public SortedProperties() {
        super();
    }

    public SortedProperties(Properties properties) {
        super();
        this.putAll(properties);
    }

    @Override
    public Set<Object> keySet() {
        return new TreeSet<>(super.keySet());
    }
}
