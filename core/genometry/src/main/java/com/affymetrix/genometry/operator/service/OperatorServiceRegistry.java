/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.operator.service;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.operator.Operator;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dcnorris
 */
@Component(name = OperatorServiceRegistry.COMPONENT_NAME, immediate = true)
public class OperatorServiceRegistry {

    public static final String COMPONENT_NAME = "OperatorServiceRegistry";

    private static final List<Operator> operators = new ArrayList<>();

    public OperatorServiceRegistry() {
    }

    @Reference(multiple = true, optional = true, dynamic = true, unbind = "removeOperator")
    public void addOperator(Operator operator) {
        checkNotNull(operator);
        operators.add(operator);
    }

    public void removeOperator(Operator operator) {
        checkNotNull(operator);
        operators.remove(operator);
    }

    public static List<Operator> getOperators() {
        return operators;
    }

}
