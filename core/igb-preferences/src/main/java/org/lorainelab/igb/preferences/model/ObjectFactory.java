/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.lorainelab.igb.preferences.model;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import javax.xml.namespace.QName;

/**
 *
 * @author dcnorris
 */
public class ObjectFactory {
       public ObjectFactory() {
    }

    @XmlElementDecl(name = "AnnotationUrl")
    public JAXBElement<AnnotationUrl> createAnnotationUrl(AnnotationUrl value) {
        return new JAXBElement<>(new QName("uri", "AnnotationUrl"), AnnotationUrl.class, value);
    }

    @XmlElementDecl(name = "DataProviderConfig")
    public JAXBElement<DataProviderConfig> createDataProviderConfig(DataProviderConfig value) {
        return new JAXBElement<>(new QName("uri", "DataProviderConfig"), DataProviderConfig.class, value);
    }

    @XmlElementDecl(name = "IgbPreferences")
    public JAXBElement<IgbPreferences> createIgbPreferences(IgbPreferences value) {
        return new JAXBElement<>(new QName("uri", "IgbPreferences"), IgbPreferences.class, value);
    }

    @XmlElementDecl(name = "PluginRepository")
    public JAXBElement<PluginRepository> createPluginRepository(PluginRepository value) {
        return new JAXBElement<>(new QName("uri", "PluginRepository"), PluginRepository.class, value);
    }
}
