package apollo;

import apollo.analysis.BlastOptionsI;
import apollo.analysis.RemoteBlastNCBI;
import apollo.analysis.RemoteBlastOptions;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
import com.affymetrix.igb.shared.IPrefEditorComponent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author hiralv
 */
public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

	public Activator(){
		super(IGBService.class);
	}
	
	@Override
	protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
		BlastOptionsI blastOptions = new RemoteBlastOptions(BlastOptionsI.DEFAULT_BLAST_TYPE);
		
		return new ServiceRegistration[] {
			bundleContext.registerService(ContextualPopupListener.class, new NCBIPopupListener(igbService.getSeqMapView(), blastOptions), null),
//			bundleContext.registerService(IPrefEditorComponent.class, (IPrefEditorComponent)blastOptions, null)
		};
	}
	
}
