package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.general.IParameters;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComboChainOperator implements Operator, IParameters {

    private static final String BASE_NAME = "chain";
    private final List<Operator> operators;

    public ComboChainOperator(Operator... operators_) {
        super();
        this.operators = (operators_ == null) ? new ArrayList<>() : Arrays.asList(operators_);
        checkAllCompatible();
    }

    private boolean checkAllCompatible() {
        boolean isCompatible = true;
        for (int i = 1; i < operators.size(); i++) {
            isCompatible &= checkCompatible(operators.get(i - 1), operators.get(i - 1));
        }
        return isCompatible;
    }

    private boolean checkCompatible(Operator before, Operator after) {
        FileTypeCategory category = before.getOutputCategory();
        boolean isCompatible = true;
        for (FileTypeCategory checkCategory : FileTypeCategory.values()) {
            int categoryCount = (checkCategory == category) ? 1 : 0;
            boolean ok = after.getOperandCountMin(checkCategory) <= categoryCount;
            if (!ok) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "incompatible operands, {0} cannot pass output to {1}", new Object[]{before.getDisplay(), after.getDisplay()});
            }
            isCompatible &= ok;
        }
        return isCompatible;
    }

    @Override
    public String getName() {
        StringBuilder name = new StringBuilder(BASE_NAME);
        for (Operator operator : operators) {
            name.append("_");
            name.append(operator.getName());
        }
        return name.toString();
    }

    @Override
    public String getDisplay() {
        StringBuilder name = new StringBuilder(GenometryConstants.BUNDLE.getString("operator_" + BASE_NAME));
        for (Operator operator : operators) {
            if (BASE_NAME.equals(name.toString())) {
                name.append(" ");
            } else {
                name.append(",");
            }
            name.append(GenometryConstants.BUNDLE.getString("operator_" + operator.getName()));
        }
        return name.toString();
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        if (!checkAllCompatible()) {
            return null;
        }
        SeqSymmetry resultSym = null;
        for (Operator operator : operators) {
            if (resultSym == null) {
                resultSym = operator.operate(aseq, symList);
            } else {
                List<SeqSymmetry> inputSymList = new ArrayList<>();
                inputSymList.add(resultSym);
                resultSym = operator.operate(aseq, inputSymList);
            }
        }
        return resultSym;
    }

    @Override
    public int getOperandCountMin(FileTypeCategory category) {
        return operators.isEmpty() ? 0 : operators.get(0).getOperandCountMin(category);
    }

    @Override
    public int getOperandCountMax(FileTypeCategory category) {
        return operators.isEmpty() ? 0 : operators.get(0).getOperandCountMax(category);
    }

    @Override
    public Map<String, Class<?>> getParametersType() {
        Map<String, Class<?>> parameters = new HashMap<>();
        operators.stream().filter(operator -> operator instanceof IParameters).forEach(operator -> {
            parameters.putAll(((IParameters) operator).getParametersType());
        });
        return parameters;
    }

    @Override
    public boolean setParametersValue(Map<String, Object> parms) {
        boolean ret = true;
        for (Operator operator : operators) {
            if (operator instanceof IParameters) {
                ret &= ((IParameters) operator).setParametersValue(parms);
            }
        }
        return ret;
    }

    @Override
    public Object getParameterValue(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Object> getParametersPossibleValues(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean setParameterValue(String key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrintableString() {
        return "";
    }

    @Override
    public FileTypeCategory getOutputCategory() {
        return operators.isEmpty() ? null : operators.get(operators.size() - 1).getOutputCategory();
    }

    @Override
    public boolean supportsTwoTrack() {
        boolean support = true;
        for (Operator operator : operators) {
            support &= operator.supportsTwoTrack();
        }
        return support;
    }

    @Override
    public Operator newInstance() {
        try {
            return getClass().getConstructor(Operator[].class).newInstance(operators.toArray());
        } catch (Exception ex) {

        }
        return null;
    }
}
