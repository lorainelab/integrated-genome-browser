package org.lorainelab.igb.error.reporter.manager;

import static com.affymetrix.common.CommonUtils.APP_VERSION;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.function.Predicate;
import org.lorainelab.igb.error.reporter.api.ErrorReporter;
import org.lorainelab.igb.error.reporter.api.model.BundleInfo;
import org.lorainelab.igb.error.reporter.api.model.ClientInfo;
import org.lorainelab.igb.error.reporter.api.model.EnvironmentInfo;
import org.lorainelab.igb.error.reporter.api.model.ErrorInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public final class ErrorReportingAppender {

    private static final double TPS = 1.0 / 5;
    private final ErrorReporter errorReporter;
    private final Set<BundleInfo> runtimeBundleInfo;
    private EnvironmentInfo environmentInfo;
    private final Set<ErrorInfo> errorInfo;
    private final RateLimiter rateLimiter;
    private static final Predicate<? super Bundle> IS_PLUGIN = bundle -> bundle.getLocation().startsWith("obr:");
    private ClientInfo clientInfo;

    private final ScheduledExecutorService scheduler;

    public void initializeScheduledErrorReports() {
        final Runnable beeper = () -> {
            if (!errorInfo.isEmpty()) {
                if (rateLimiter.tryAcquire()) {
                    errorReporter.report(errorInfo, clientInfo);
                    errorInfo.clear();
                }
            }
        };
        final ScheduledFuture<?> errorReportScheduler = scheduler.scheduleAtFixedRate(beeper, 15, 15, SECONDS);
        scheduler.schedule(() -> {
            errorReportScheduler.cancel(true);
        }, Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    public ErrorReportingAppender(BundleContext bundleContext, ErrorReporter errorReporter) {
        checkNotNull(errorReporter);
        this.errorReporter = errorReporter;
        errorInfo = Sets.newLinkedHashSet();
        rateLimiter = RateLimiter.create(TPS, 1, TimeUnit.SECONDS);
        runtimeBundleInfo = Sets.newLinkedHashSet();
        initializeBundleInfo(bundleContext);
        initializeEnvironmentInfo();
        initializeClientInfo();
        scheduler = Executors.newScheduledThreadPool(1);
        initializeScheduledErrorReports();
    }

    private void initializeClientInfo() {
        clientInfo = new ClientInfo(environmentInfo, runtimeBundleInfo);
    }

    private void initializeEnvironmentInfo() {
        String memoryInfo = humanReadableByteCount(Runtime.getRuntime().maxMemory(), true);
        environmentInfo = new EnvironmentInfo(APP_VERSION, System.getProperty("os.name"), memoryInfo);
    }

    //credit http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void initializeBundleInfo(BundleContext bundleContext) {
        final List<Bundle> pluginBundleInfo = Arrays.asList(bundleContext.getBundles());
        pluginBundleInfo.stream().filter(IS_PLUGIN).forEach(runtimeBundle -> {
            runtimeBundleInfo.add(new BundleInfo(runtimeBundle.getVersion().toString(), runtimeBundle.getSymbolicName()));
        });
    }

    public void recordErrorLogMessage(String logMessage) {
        errorInfo.add(new ErrorInfo(logMessage));
    }

}
