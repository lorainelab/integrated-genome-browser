/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class JSLogger {

    private static final Logger logger = LoggerFactory.getLogger(JSLogger.class);

    public void log(String message) {
        Platform.runLater(() -> {
            logger.info(message);
        });
    }
}
