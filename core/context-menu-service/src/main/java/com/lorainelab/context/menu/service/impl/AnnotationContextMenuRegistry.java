/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.context.menu.service.impl;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.common.collect.Sets;
import org.lorainelab.igb.context.menu.AnnotationContextMenuProvider;
import org.lorainelab.igb.context.menu.service.AnnotationContextMenuRegistryI;
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

    @Reference(optional = true, multiple = true, dynamic = true, unbind = "removeAnnotationContextMenuItems")
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
