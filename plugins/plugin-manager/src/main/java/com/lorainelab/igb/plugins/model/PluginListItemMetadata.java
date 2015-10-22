package com.lorainelab.igb.plugins.model;

import com.lorainelab.igb.plugins.BundleInfoManager;
import java.util.Base64;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.osgi.framework.Bundle;

/**
 *
 * @author jeckstei
 */
public class PluginListItemMetadata extends AbstractObservableModel<PluginListItemMetadata> {

    private final StringProperty pluginName;
    private final StringProperty repository;
    private StringProperty version;
    private final StringProperty description;
    private BooleanProperty isUpdatable;
    private BooleanProperty isInstalled;
    private Bundle bundle;
    private IntegerProperty weight;
    private BooleanProperty isBusy;


    public PluginListItemMetadata(Bundle bundle, String repository, Boolean isUpdatable) {
        this.bundle = bundle;
        this.pluginName = new SimpleStringProperty(bundle.getSymbolicName());
        this.version = new SimpleStringProperty(bundle.getVersion().toString());
        this.repository = new SimpleStringProperty(repository);
        this.isUpdatable = new SimpleBooleanProperty(isUpdatable);
        this.isInstalled = new SimpleBooleanProperty(BundleInfoManager.isInstalled(bundle) || isUpdatable);
        this.description = new SimpleStringProperty(getBundleDescription(bundle));
        this.weight = new SimpleIntegerProperty(0);
        this.isBusy = new SimpleBooleanProperty(Boolean.FALSE);
    }

    //for unit testing...
    public PluginListItemMetadata(String pluginName, String repository, String version, String description, Boolean isUpdatable, Boolean isInstalled) {
        this.pluginName = new SimpleStringProperty(pluginName);
        this.repository = new SimpleStringProperty(repository);
        this.version = new SimpleStringProperty(version);
        this.description = new SimpleStringProperty(description);
        this.isUpdatable = new SimpleBooleanProperty(isUpdatable);
        this.isInstalled = new SimpleBooleanProperty(isInstalled || isUpdatable);
    }

    public String getPluginName() {
        return pluginName.getValue();
    }

    public String getRepository() {
        return repository.getValue();
    }

    public String getVersion() {
        return version.getValue();
    }

    public String getDescription() {
        return description.getValue();
    }

    public Boolean isUpdatable() {
        return isUpdatable.getValue();
    }

    public Boolean isInstalled() {
        return isInstalled.getValue();
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setVersion(String version) {
        this.version.set(version);
    }

    public int getWeight() {
        return weight.getValue();
    }

    public void setWeight(int weight) {
        this.weight.setValue(weight);
    }

    public void setIsUpdatable(Boolean isUpdatable) {
        this.isUpdatable.setValue(isUpdatable);
    }

    public Boolean isBusy() {
        return isBusy.getValue();
    }

    public void setIsBusy(Boolean isBusy) {
        this.isBusy.setValue(isBusy);
    }

    public static String getBundleDescription(Bundle bundle) {
        String bundleDescription = bundle.getSymbolicName();
        try {
            bundleDescription = bundle.getHeaders().get("Bundle-Description");
            byte[] decode = Base64.getDecoder().decode(bundleDescription);
            bundleDescription = new String(decode);
        } catch (Exception ex) {
        }
        return bundleDescription;
    }

    public void setIsInstalled(Boolean isInstalled) {
        this.isInstalled.setValue(isInstalled);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.pluginName);
        hash = 67 * hash + Objects.hashCode(this.repository);
        hash = 67 * hash + Objects.hashCode(this.version);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PluginListItemMetadata other = (PluginListItemMetadata) obj;
        if (!Objects.equals(this.pluginName, other.pluginName)) {
            return false;
        }
        if (!Objects.equals(this.repository, other.repository)) {
            return false;
        }
        if (!Objects.equals(this.version, other.version)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(PluginListItemMetadata o) {
        return o.getWeight() - this.getWeight();
    }

}
