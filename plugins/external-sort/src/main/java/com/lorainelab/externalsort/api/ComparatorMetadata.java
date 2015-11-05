/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.externalsort.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author jeckstei
 */
public class ComparatorMetadata {
    private Comparator<ComparatorInstance> comparator;
    private final List<Function<String, ? extends Comparable>> preparers;

    public ComparatorMetadata() {
        preparers = new ArrayList<>();
    }

    public Comparator<ComparatorInstance> getComparator() {
        return comparator;
    }

    public void setComparator(Comparator<ComparatorInstance> comparator) {
        this.comparator = comparator;
    }

    public List<Function<String, ? extends Comparable>> getPreparers() {
        return preparers;
    }


}
