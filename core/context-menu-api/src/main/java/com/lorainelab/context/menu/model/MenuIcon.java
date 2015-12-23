/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.context.menu.model;

import com.google.common.io.BaseEncoding;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class MenuIcon {

    private String encodedImage;
    private static final Logger logger = LoggerFactory.getLogger(MenuIcon.class);

    public MenuIcon(InputStream resourceAsStream) {
        try {
            encodedImage = BaseEncoding.base64().encode(IOUtils.toByteArray(resourceAsStream));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public byte[] getEncodedImage() {
        return BaseEncoding.base64().decode(encodedImage);
    }

}
