package org.lorainelab.igb.igb.plugins.model;

import java.util.Base64;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

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

    public PluginListItemMetadata(Bundle bundle, String version, String repository, Boolean isInstalled, Boolean isUpdatable) {
        this.bundle = bundle;
        String bundleName = getpluginName(bundle);
        this.pluginName = new SimpleStringProperty(bundleName);
        this.version = new SimpleStringProperty(version);
        this.repository = new SimpleStringProperty(repository);
        this.isUpdatable = new SimpleBooleanProperty(isUpdatable);
        this.isInstalled = new SimpleBooleanProperty(isInstalled);
        this.description = new SimpleStringProperty(getBundleDescription(bundle));
        this.weight = new SimpleIntegerProperty(0);
        this.isBusy = new SimpleBooleanProperty(Boolean.FALSE);
    }

    private String getpluginName(Bundle bundle) {
        try {
            return bundle.getHeaders().get(Constants.BUNDLE_NAME);
        } catch (Exception ex) {
            return bundle.getSymbolicName();
        }
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

    public StringProperty getPluginName() {
        return pluginName;
    }

    public StringProperty getRepository() {
        return repository;
    }

    public StringProperty getVersion() {
        return version;
    }

    public StringProperty getDescription() {
        return description;
    }

    public BooleanProperty getIsUpdatable() {
        return isUpdatable;
    }

    public BooleanProperty getIsInstalled() {
        return isInstalled;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public IntegerProperty getWeight() {
        return weight;
    }

    public BooleanProperty getIsBusy() {
        return isBusy;
    }

    public void setWeight(int weight) {
        Platform.runLater(() -> {
            this.weight.setValue(weight);
        });
    }

    public void setIsUpdatable(Boolean isUpdatable) {
        Platform.runLater(() -> {
            this.isUpdatable.setValue(isUpdatable);
        });
    }

    public void setIsBusy(Boolean isBusy) {
        Platform.runLater(() -> {
            this.isBusy.setValue(isBusy);
        });
    }

    public void setVersion(String version) {
        Platform.runLater(() -> {
            this.version.setValue(version);
        });
    }

    public static String getBundleDescription(Bundle bundle) {
        String bundleDescription;
        try {
            bundleDescription = bundle.getHeaders().get("Bundle-Description");
            byte[] decode = Base64.getDecoder().decode(bundleDescription);
            bundleDescription = new String(decode);
        } catch (Exception ex) {
            bundleDescription = bundle.getSymbolicName();
        }
        return bundleDescription;
    }

    public void setIsInstalled(Boolean isInstalled) {
        Platform.runLater(() -> {
            this.isInstalled.setValue(isInstalled);
        });
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
        return o.getWeight().get() - this.getWeight().get();
    }

}
