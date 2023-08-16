/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.context.menu.service.impl;

import com.google.common.collect.Sets;
import org.lorainelab.igb.menu.api.AnnotationContextMenuProvider;
import org.lorainelab.igb.context.menu.service.AnnotationContextMenuRegistryI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.Set;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class AnnotationContextMenuRegistry implements AnnotationContextMenuRegistryI {

    private Set<AnnotationContextMenuProvider> annotationContextMenuItems;

    public AnnotationContextMenuRegistry() {
        annotationContextMenuItems = Sets.newConcurrentHashSet();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeAnnotationContextMenuItems")
    public void addAnnotationContextMenuItems(AnnotationContextMenuProvider annotationContextMenuProvider) {
        annotationContextMenuItems.add(annotationContextMenuProvider);
    }

    public void removeAnnotationContextMenuItems(AnnotationContextMenuProvider annotationContextMenuProvider) {
        annotationContextMenuItems.remove(annotationContextMenuProvider);
    }

    @Override
    public Set<AnnotationContextMenuProvider> getAnnotationContextMenuItems() {
        return annotationContextMenuItems;
    }

}
