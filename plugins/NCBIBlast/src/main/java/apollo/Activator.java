package apollo;

import com.affymetrix.genometryImpl.event.AxisPopupListener;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author hiralv
 */
public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(ContextualPopupListener.class, new NCBIBlastPopupListener(igbService.getSeqMapView()), null),
            bundleContext.registerService(AxisPopupListener.class, new NCBIBlastPopupListener(igbService.getSeqMapView()), null)};
    }

}
