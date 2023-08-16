/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.parsers;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, provide = {FileTypehandlerRegistry.class})
public class FileTypehandlerRegistry {

    private static final FileTypeHolder FILE_TYPE_HOLDER = new FileTypeHolder();

    public FileTypehandlerRegistry() {
    }

    @Reference(optional = false, multiple = true, unbind = "removeFileTypeHandler", dynamic = true)
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
