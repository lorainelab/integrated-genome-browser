package org.lorainelab.igb.logging.api;

/**
 *
 * @author dcnorris
 */
public interface LogLevelConfigService {

    void setLogLevel(String logLevel);

    String getLogLevel();
}
