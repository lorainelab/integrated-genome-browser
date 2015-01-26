package apollo;

import apollo.analysis.NCBIPrimerBlastPane;
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
        NCBIPrimerBlastPane ncbiPrimerBlastPane = new NCBIPrimerBlastPane();

        return new ServiceRegistration[]{
            bundleContext.registerService(ContextualPopupListener.class, new NCBIPrimerPopupListener(igbService.getSeqMapView(), ncbiPrimerBlastPane), null), //			bundleContext.registerService(IPrefEditorComponent.class, ncbiPrimerBlastPane, null)
        };
    }

}
