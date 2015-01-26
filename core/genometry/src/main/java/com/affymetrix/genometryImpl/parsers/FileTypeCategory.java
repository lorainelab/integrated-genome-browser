package com.affymetrix.genometry.parsers;

public enum FileTypeCategory {

    Axis("Axis"),
    Annotation("Annotation"),
    Alignment("Alignment"),
    Graph("Graph"),
    Sequence("Sequence"),
    Mismatch("Mismatch"),
    ProbeSet("ProbeSet"),
    ScoredContainer("ScoredContainer"),
    PairedRead("PairedRead");

    private final String name;

    private FileTypeCategory(String name) {
        this.name = name;
    }

}
