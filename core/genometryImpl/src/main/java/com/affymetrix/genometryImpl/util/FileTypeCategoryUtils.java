/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class FileTypeCategoryUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileTypeCategoryUtils.class);
    
    public static boolean isFileTypeCategoryContainer(FileTypeCategory category) {
        switch (category) {
            case Alignment:
                return true;
            case Annotation:
                return true;
            case Axis:
                return false;
            case Graph:
                return false;
            case Mismatch:
                return false;
            case PairedRead:
                return true;
            case ProbeSet:
                return true;
            case ScoredContainer:
                return false;
            case Sequence:
                return false;
            default:
                logger.error("Unrecognized FileTypeCategory {} should be added to this util method", category);
                throw new IllegalArgumentException();
        }
    }
}
