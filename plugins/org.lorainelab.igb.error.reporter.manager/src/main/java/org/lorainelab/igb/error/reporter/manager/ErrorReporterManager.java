package org.lorainelab.igb.error.reporter.manager;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.lorainelab.igb.error.reporter.api.ErrorReporter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class ErrorReporterManager {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorReporterManager.class);
    private final LoggerContext loggerContext;
    private ErrorReportingAppender errorReportingAppender;
    private OutputStreamAppender<ILoggingEvent> outputStreamAppender;
    private final ch.qos.logback.classic.Logger rootLogger;
    private ErrorReporter errorReporter;
    private BundleContext bundleContext;

    public ErrorReporterManager() {
        rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        loggerContext = rootLogger.getLoggerContext();
    }

    @Activate
    void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        setupErrorReporterLogging();
    }

    @Deactivate
    void deactivate() {
        tearDownErrorReporterLogging();
    }

    @Reference
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    private void setupErrorReporterLogging() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(stream);
        errorReportingAppender = new ErrorReportingAppender(bundleContext, errorReporter);
        outputStreamAppender = new OutputStreamAppender<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent eventObject) {
                super.append(eventObject);
                if (eventObject.getLevel().equals(Level.ERROR)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(eventObject.getFormattedMessage());
                    sb.append("\n");
                    for (StackTraceElement element : eventObject.getCallerData()) {
                        sb.append(element.toString());
                        sb.append("\n");
                    }
                    errorReportingAppender.recordErrorLogMessage(sb.toString());
                }
            }

        };
        outputStreamAppender.setName("Remote Error Appender");
        outputStreamAppender.setContext(loggerContext);
        LevelFilter errorLevelFilter = new LevelFilter();
        errorLevelFilter.setLevel(Level.ERROR);
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n");
        encoder.start();
        outputStreamAppender.addFilter(errorLevelFilter);
        outputStreamAppender.setEncoder(encoder);
        outputStreamAppender.setOutputStream(printStream);
        outputStreamAppender.start();
        rootLogger.addAppender(outputStreamAppender);

    }

    private void tearDownErrorReporterLogging() {
        rootLogger.detachAppender(outputStreamAppender);
        outputStreamAppender.stop();
    }

}
