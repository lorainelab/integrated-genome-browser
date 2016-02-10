package org.lorainelab.igb.menu.api.model;

import com.google.common.io.BaseEncoding;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ## MenuIcon
 * A class to allow the association of an icon with a
 * {@see ContextMenuItem}
 * @author dcnorris
 * @module.info context-menu-api
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
