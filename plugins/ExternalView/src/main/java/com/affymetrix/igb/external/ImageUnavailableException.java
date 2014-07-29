/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.external;

import java.text.MessageFormat;

/**
 *
 * @author dcnorris
 */
public class ImageUnavailableException extends Exception {

    ImageUnavailableException() {
        super(MessageFormat.format(ExternalViewer.BUNDLE.getString("findImageURLError"), ""));
    }
    ImageUnavailableException(String message){
        super(message);
    }
}
