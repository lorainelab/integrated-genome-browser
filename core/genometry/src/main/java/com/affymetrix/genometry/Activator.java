package com.affymetrix.genometry;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.util.ServerTypeI;

/**
 * OSGi Activator for genometry bundle
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        initFileTypeHandlers(bundleContext);
        initGenericActions(bundleContext);
        initServerTypes(bundleContext);
    }

    @Override
    public void stop(BundleContext _bundleContext) throws Exception {
    }

    private void initFileTypeHandlers(BundleContext bundleContext) {
        // add all FileTypeHandler implementations to FileTypeHolder
        ExtensionPointHandler<FileTypeHandler> extensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, FileTypeHandler.class);
        extensionPoint.addListener(new ExtensionPointListener<FileTypeHandler>() {
            @Override
            public void removeService(FileTypeHandler fileTypeHandler) {
                FileTypeHolder.getInstance().removeFileTypeHandler(fileTypeHandler);
            }

            @Override
            public void addService(FileTypeHandler fileTypeHandler) {
                FileTypeHolder.getInstance().addFileTypeHandler(fileTypeHandler);
            }
        });
    }

    private void initGenericActions(BundleContext bundleContext) {
        // add all GenericAction implementations to GenericActionHolder
        ExtensionPointHandler<GenericAction> extensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, GenericAction.class);
        extensionPoint.addListener(new ExtensionPointListener<GenericAction>() {
            @Override
            public void addService(GenericAction genericAction) {
                GenericActionHolder.getInstance().addGenericAction(genericAction);
            }

            @Override
            public void removeService(GenericAction genericAction) {
                GenericActionHolder.getInstance().removeGenericAction(genericAction);
            }
        });
    }

    private void initServerTypes(BundleContext bundleContext) {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
        bundleContext.registerService(ServerTypeI.class, ServerTypeI.LocalFiles, null);
        bundleContext.registerService(ServerTypeI.class, ServerTypeI.QuickLoad, null);
        bundleContext.registerService(ServerTypeI.class, ServerTypeI.DAS, null);
        bundleContext.registerService(ServerTypeI.class, ServerTypeI.DAS2, null);
    }

}
