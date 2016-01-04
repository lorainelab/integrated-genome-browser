/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.externalsort.api;

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
        comparator = getDefaultComparator();
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

    private Comparator<ComparatorInstance> getDefaultComparator() {
        return (ComparatorInstance o1, ComparatorInstance o2) -> {
            int current = 0;
            try {

                for (Function<String, ? extends Comparable> preparer : o1.getComparatorMetadata().getPreparers()) {
                    Comparable input1 = preparer.apply(o1.getLine());
                    Comparable input2 = preparer.apply(o2.getLine());
                    current = input1.compareTo(input2);
                    if (current != 0) {
                        return current;
                    }

                }
            } catch (Exception ex) {
                return 0;
            }
            return current;
        };
    }

}
