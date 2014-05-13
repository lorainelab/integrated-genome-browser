package com.affymetrix.igb.update;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.StatusAlert;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

/**
 *
 * @author hiralv
 */
public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(final BundleContext bundleContext, final IGBService igbService) throws Exception {
        ResourceBundle BUNDLE = ResourceBundle.getBundle("updates");
        InputStream inputStream = null;
        try {
            inputStream = Activator.class.getResourceAsStream("/updates.xml");
            //inputStream = LocalUrlCacher.getInputStream(BUNDLE.getString("updates"));
            if (inputStream != null) {
                final Version CURRENT_VERSION = new Version(CommonUtils.getInstance().getAppVersion());
                final List<Update> updates = UpdateParser.parse(inputStream);
                Collections.sort(updates, new Comparator<Update>() {
                    public int compare(Update o1, Update o2) {
                        return o2.getVersion().compareTo(o1.getVersion());
                    }
                });
                if (updates.size() > 0) {
                    if (updates.get(0).getVersion().compareTo(CURRENT_VERSION) > 0 /*	&& !PreferenceUtils.getBooleanParam(Update.UPDATE_PREFIX+updates.get(0).getVersion().toString(), false) */) {
                        return new ServiceRegistration[]{bundleContext.registerService(StatusAlert.class, new UpdateStatusAlert(updates.get(0)), null)};
                    }
                }
            }
        } catch (Exception ex) {

        } finally {
            GeneralUtils.safeClose(inputStream);
        }

        return null;
    }
}
