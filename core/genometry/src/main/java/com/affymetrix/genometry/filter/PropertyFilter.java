package com.affymetrix.genometry.filter;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.general.BoundedParameter;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.LinkedList;
import java.util.List;

/**
 * IGBF-3640: This class is similar to {@link com.affymetrix.genometry.filter.ScoreFilter} with additional properties.
 * This class is designed to expand additional properties for filter-by.
 */
public class PropertyFilter extends SymmetryFilter{
    private final static String PROPERTY = "property";
    private final static List<String> PROPERTY_VALUES = new LinkedList<>();
    private final static String PROPERTY_VALUE = "value";
    public final static String DEFAULT_PROPERTY_VALUE = "";
    private final static String COMPARATOR = "comparator";
    private final static List<MathComparisonOperator> COMPARATOR_VALUES = new LinkedList<>();
    static {
        COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.GreaterThanEqualMathComparisonOperator());
        COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.GreaterThanMathComparisonOperator());
        COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.EqualMathComparisonOperator());
        COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.LessThanMathComparisonOperator());
        COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.LessThanEqualMathComparisonOperator());
        COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.NotEqualMathComparisonOperator());
    }
    static {
        PROPERTY_VALUES.add("");
    }
    private Object given_property_value;
    private Parameter<MathComparisonOperator> comparator = new BoundedParameter<>(COMPARATOR_VALUES);
    protected Parameter<String> property = new BoundedParameter<>(PROPERTY_VALUES);
    protected Parameter<String> property_value = new Parameter<String>(DEFAULT_PROPERTY_VALUE) {
        @Override
        public boolean set(Object e) {
            if (e != null) {
                try {
                    given_property_value = e;
                } catch (Exception ex) {
                }
                return super.set(e.toString().toLowerCase());
            }
            return super.set(e);
        }
    };
    public PropertyFilter(){
        super();
        parameters.addParameter(PROPERTY, String.class, property);
        parameters.addParameter(PROPERTY_VALUE, String.class, property_value);
        parameters.addParameter(COMPARATOR, MathComparisonOperator.class, comparator);
    }
    @Override
    public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym) {
        if(sym instanceof SymWithProps symWithProps){
            Object value = symWithProps.getProperty(property.get());
            if(value==null){
                return false;
            } else if (value instanceof String str) {
                return comparator.get().operate(str, (String)given_property_value);
            } else if (value instanceof Double val) {
                return comparator.get().operate(val, Double.parseDouble((String) given_property_value));
            }
        }
        return false;
    }
    @Override
    public String getName() {
        return "property";
    }
}