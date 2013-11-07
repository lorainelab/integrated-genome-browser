package com.affymetrix.sequenceviewer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;

/**
 * @author hiralv
 */
public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

	public Activator(){
		super(IGBService.class);
	}
	
	@Override
	protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception{
		JRPMenuItem genomicSequenceMenuItem = new JRPMenuItem("SequenceViewer_viewGenomicSequenceInSeqViewer", new ViewGenomicSequenceInSeqViewerAction(igbService));
		JRPMenuItem readSequenceMenuItem = new JRPMenuItem("SequenceViewer_viewAlignmentSequenceInSeqViewer", new ViewReadSequenceInSeqViewerAction(igbService));
		
		return new ServiceRegistration[]{
			bundleContext.registerService(AMenuItem.class, new AMenuItem(genomicSequenceMenuItem, "view" , 0), null),
			bundleContext.registerService(AMenuItem.class, new AMenuItem(readSequenceMenuItem, "view" , 0), null),
			bundleContext.registerService(ContextualPopupListener.class, new PopupListener(genomicSequenceMenuItem, readSequenceMenuItem), null),
		};
	}
}