/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.lorainelab.igb.logging;

import ch.qos.logback.classic.Level;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.lorainelab.igb.logging.api.LogLevelConfigService;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class LogLevelActivator implements LogLevelConfigService {

    private String currentLogLevel = Level.INFO.toString();

    @Activate
    public void activate() {
        String envDevMode = System.getenv("IGB_DEV_MODE");
        if (envDevMode != null && !envDevMode.isBlank()) {
            setLogLevel(Level.DEBUG.toString());
        }
    }

    public void setLogLevel(String logLevel) {
        Level newLogLevel = Level.toLevel(logLevel, Level.INFO);
        currentLogLevel = newLogLevel.toString();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(newLogLevel);
    }

    public String getLogLevel() {
        return currentLogLevel;
    }
}
