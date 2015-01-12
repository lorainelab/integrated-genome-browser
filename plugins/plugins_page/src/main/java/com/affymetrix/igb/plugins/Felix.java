package com.affymetrix.igb.plugins;

import com.affymetrix.common.CommonUtils;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.impl.PropertyImpl;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.bundlerepository.impl.wrapper.CapabilityWrapper;
import org.apache.felix.bundlerepository.impl.wrapper.RepositoryAdminWrapper;
import org.apache.felix.utils.log.Logger;

public class Felix implements OSGIImpl {

    private static final Capability COMMON_CAPABILITY = new Capability() {
        @Override
        public String getName() {
            return Capability.PACKAGE;
        }

        @Override
        public Property[] getProperties() {
            return new Property[]{new PropertyImpl("package", null, "com.affymetrix.common"), new PropertyImpl("version", Property.VERSION, CommonUtils.getInstance().getAppVersion())};
        }

        @Override
        public java.util.Map<String, Object> getPropertiesAsMap() {
            java.util.Map<String, Object> propertiesMap = new java.util.HashMap<>();
            propertiesMap.put("package", "com.affymetrix.common");
            propertiesMap.put("version", new org.osgi.framework.Version(CommonUtils.getInstance().getAppVersion()));
            return propertiesMap;
        }
    };

    private final org.osgi.service.obr.Capability COMMON_CAPABILITY_WRAPPER = new CapabilityWrapper(COMMON_CAPABILITY);

    @Override
    public org.osgi.service.obr.Capability getCapability() {
        return COMMON_CAPABILITY_WRAPPER;
    }

    @Override
    public org.osgi.service.obr.RepositoryAdmin getRepositoryAdmin(org.osgi.framework.BundleContext bundleContext) {
        return new RepositoryAdminWrapper(new RepositoryAdminImpl(bundleContext, new Logger(bundleContext)));
    }
}
