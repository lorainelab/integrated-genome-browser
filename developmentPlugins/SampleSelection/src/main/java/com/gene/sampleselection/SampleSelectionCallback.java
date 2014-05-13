package com.gene.sampleselection;

import java.util.List;
import java.util.Map;

public interface SampleSelectionCallback {

    public void select(String name, boolean separateTracks, Map<String, List<String>> selections);
}
