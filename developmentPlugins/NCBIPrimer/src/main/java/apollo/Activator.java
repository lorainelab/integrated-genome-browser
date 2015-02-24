package apollo;

import apollo.analysis.NCBIPrimerBlastPane;
import com.affymetrix.genometry.event.ContextualPopupListener;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.XServiceRegistrar;
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
        NCBIPrimerBlastPane ncbiPrimerBlastPane = new NCBIPrimerBlastPane();

        return new ServiceRegistration[]{
            bundleContext.registerService(ContextualPopupListener.class, new NCBIPrimerPopupListener(igbService.getSeqMapView(), ncbiPrimerBlastPane), null), //			bundleContext.registerService(IPrefEditorComponent.class, ncbiPrimerBlastPane, null)
        };
    }

}
