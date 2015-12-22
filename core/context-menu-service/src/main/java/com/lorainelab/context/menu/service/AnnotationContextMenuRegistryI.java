/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.context.menu.service;

import com.lorainelab.context.menu.AnnotationContextMenuProvider;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface AnnotationContextMenuRegistryI {

    public Set<AnnotationContextMenuProvider> getAnnotationContextMenuItems();

}
