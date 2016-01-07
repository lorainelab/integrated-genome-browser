package org.lorainelab.igb.error.reporter.api;

import java.util.Set;
import org.lorainelab.igb.error.reporter.model.ClientInfo;
import org.lorainelab.igb.error.reporter.model.ErrorInfo;

/**
 *
 * @author dcnorris
 */
public interface ErrorReporter {

    public void report(Set<ErrorInfo> errorInfo, ClientInfo clientInfo);

}
