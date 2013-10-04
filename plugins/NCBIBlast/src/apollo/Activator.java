package apollo;

import apollo.analysis.BlastRunOptsPanel;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
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
		BlastRunOptsPanel blastRunOptsPanel = new BlastRunOptsPanel();
		
		return new ServiceRegistration[] {
			bundleContext.registerService(ContextualPopupListener.class, new NCBIBlastPopupListener(igbService.getSeqMapView(), blastRunOptsPanel), null),
//			bundleContext.registerService(IPrefEditorComponent.class, blastRunOptsPanel, null)
		};
	}
	
}
