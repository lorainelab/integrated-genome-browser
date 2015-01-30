/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.track.operations;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.igb.service.api.IgbService;
import com.lorainelab.igb.track.operations.api.OperationsPanel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dcnorris
 */
@Component(name = AnnotationOperationsImpl.COMPONENT_NAME, immediate = true, provide = AnnotationOperationsImpl.class)
public class AnnotationOperationsImpl extends OperationsPanel {

    public static final String COMPONENT_NAME = "AnnotationOperationsImpl";
    private volatile List<Operator> initializationHolder;
    private IgbService igbService;

    public AnnotationOperationsImpl() {
        categories = new FileTypeCategory[]{FileTypeCategory.Annotation, FileTypeCategory.Alignment, FileTypeCategory.ProbeSet};
        initializationHolder = new ArrayList<>();
    }

    @Activate
    public void activate() {
        init(igbService);
        initializationHolder.forEach(this::addOperator);
        initializationHolder.clear();
    }

    @Reference(multiple = true, unbind = "removeTrackOperator", optional = true, dynamic = true)
    public void addTrackOperator(Operator operator) {
        if (igbService == null) {
            initializationHolder.add(operator);
        } else {
            addOperator(operator);
        }
    }

    public void removeTrackOperator(Operator operator) {
        if (igbService == null) {
            initializationHolder.remove(operator);
        } else {
            removeOperator(operator);
        }
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }
}
