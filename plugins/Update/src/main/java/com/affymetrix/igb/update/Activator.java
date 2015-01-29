package com.affymetrix.igb.update;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.util.StatusAlert;
import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    
    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {
        ResourceBundle BUNDLE = ResourceBundle.getBundle("updates");
        try {
            URL url = new URL(BUNDLE.getString("updates"));
            String updateXml = Resources.toString(url, Charsets.UTF_8);
            //for testing only
           // String updateXml = Resources.toString(Activator.class.getClassLoader().getResource("updates.xml"), Charsets.UTF_8);
            if (StringUtils.isNotBlank(updateXml)) {
                final Version CURRENT_VERSION = new Version(CommonUtils.getInstance().getAppVersion());
                final List<Update> updates = UpdateParser.parse(updateXml);
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
            logger.info("No update notifications found");
        }

        return null;
    }

}
