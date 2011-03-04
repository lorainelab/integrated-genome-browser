package com.affymetrix.genometry.genopub;

import java.util.Set;

import com.affymetrix.genometry.genopub.Owned;

public class AnnotationProperty {
    
    private Integer      idAnnotationProperty;
    private String       name;
    private String       value;
    private Set          options;
    private Integer      idAnnotation;
    private Integer      idProperty;
    private Property     property;
    
    public Integer getIdAnnotationProperty() {
      return idAnnotationProperty;
    }
    public void setIdAnnotationProperty(Integer idAnnotationProperty) {
      this.idAnnotationProperty = idAnnotationProperty;
    }
    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }
    public String getValue() {
      return value;
    }
    public void setValue(String value) {
      this.value = value;
    }
    public Set getOptions() {
      return options;
    }
    public void setOptions(Set options) {
      this.options = options;
    }
    public Integer getIdAnnotation() {
      return idAnnotation;
    }
    public void setIdAnnotation(Integer idAnnotation) {
      this.idAnnotation = idAnnotation;
    }
    public Integer getIdProperty() {
      return idProperty;
    }
    public void setIdProperty(Integer idProperty) {
      this.idProperty = idProperty;
    }
    public Property getProperty() {
      return property;
    }
    public void setProperty(Property property) {
      this.property = property;
    }
    


}
