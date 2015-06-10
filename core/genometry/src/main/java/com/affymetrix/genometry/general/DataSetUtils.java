/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.general;

import java.io.File;

/**
 *
 * @author Tarun
 */
public class DataSetUtils {

    public static String extractNameFromPath(String path) {
        if (path.contains(File.separator)) {
            path = path.substring(path.lastIndexOf(File.separator) + 1);
        } else {
            path = path.substring(path.lastIndexOf("/") + 1);
        }
        return path;
    }
}
