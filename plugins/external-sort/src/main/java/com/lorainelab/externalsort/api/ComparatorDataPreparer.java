/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.externalsort.api;

import java.util.function.Function;

/**
 *
 * @author jeckstei
 */
public class ComparatorDataPreparer {
    private Class type;
    private Function<String, String> processFunction;

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public Function<String, String> getProcessFunction() {
        return processFunction;
    }

    public void setProcessFunction(Function<String, String> processFunction) {
        this.processFunction = processFunction;
    }

}
