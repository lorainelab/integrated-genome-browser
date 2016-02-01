/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.context.menu.service;

import org.lorainelab.igb.menu.api.AnnotationContextMenuProvider;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface AnnotationContextMenuRegistryI {

    public Set<AnnotationContextMenuProvider> getAnnotationContextMenuItems();

}
