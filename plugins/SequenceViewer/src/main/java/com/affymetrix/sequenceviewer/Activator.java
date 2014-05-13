package com.affymetrix.sequenceviewer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;

/**
 * @author hiralv
 */
public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        GenericAction genomicSequenceAction = new ViewGenomicSequenceInSeqViewerAction(igbService);
        GenericAction readSequencAction = new ViewReadSequenceInSeqViewerAction(igbService);
        JRPMenuItem genomicSequenceMenuItem = new JRPMenuItem("SequenceViewer_viewGenomicSequenceInSeqViewer", genomicSequenceAction);
        JRPMenuItem readSequenceMenuItem = new JRPMenuItem("SequenceViewer_viewAlignmentSequenceInSeqViewer", readSequencAction);

        return new ServiceRegistration[]{
            bundleContext.registerService(AMenuItem.class, new AMenuItem(genomicSequenceMenuItem, "view", 0), null),
            bundleContext.registerService(AMenuItem.class, new AMenuItem(readSequenceMenuItem, "view", 0), null),
            bundleContext.registerService(ContextualPopupListener.class, new PopupListener(genomicSequenceAction, readSequencAction), null),};
    }
}
