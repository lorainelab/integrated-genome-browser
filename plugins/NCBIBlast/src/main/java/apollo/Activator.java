package apollo;

import com.affymetrix.genometry.event.AxisPopupListener;
import com.affymetrix.genometry.event.ContextualPopupListener;
import com.lorainelab.igb.service.api.IgbService;
import com.lorainelab.igb.service.api.XServiceRegistrar;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author hiralv
 */
public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(ContextualPopupListener.class, new NCBIBlastPopupListener(igbService.getSeqMapView()), null),
            bundleContext.registerService(AxisPopupListener.class, new NCBIBlastPopupListener(igbService.getSeqMapView()), null)};
    }

}
