package com.affymetrix.genometry.event;

import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.Map;

public interface PropertyHandler {

    public Map<String, Object> getPropertiesRow(SeqSymmetry sym, PropertyHolder propertyHolder);

    public Map<String, Object> getGraphPropertiesRowColumn(GraphSym sym, int x, PropertyHolder propertyHolder);

    public void showGraphProperties(GraphSym sym, int x, PropertyHolder propertyHolder);

    public static String[] graph_tooltip_order = new String[]{
        "id",
        "strand",
        "x coord",
        "y coord",
        "y total",
        "min score",
        "max score",
        "A",
        "T",
        "G",
        "C",
        "N"
    };

}
