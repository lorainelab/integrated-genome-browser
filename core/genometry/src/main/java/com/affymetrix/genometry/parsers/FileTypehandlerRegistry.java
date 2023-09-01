/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.parsers;


import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, service = {FileTypehandlerRegistry.class})
public class FileTypehandlerRegistry {

    private static final FileTypeHolder FILE_TYPE_HOLDER = new FileTypeHolder();

    public FileTypehandlerRegistry() {
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, unbind = "removeFileTypeHandler", policy = ReferencePolicy.DYNAMIC)
    public void addFileTypeHandler(FileTypeHandler fileTypeHandler) {
        FILE_TYPE_HOLDER.addFileTypeHandler(fileTypeHandler);
    }

    public void removeFileTypeHandler(FileTypeHandler fileTypeHandler) {
        FILE_TYPE_HOLDER.removeFileTypeHandler(fileTypeHandler);
    }

    //stop gap until singletons can be converted to components.
    public static FileTypeHolder getFileTypeHolder() {
        return FILE_TYPE_HOLDER;
    }

}
