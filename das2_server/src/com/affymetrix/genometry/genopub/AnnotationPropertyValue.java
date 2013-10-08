package com.affymetrix.genometry.genopub;


public class AnnotationPropertyValue {
    
    private Integer      idAnnotationPropertyValue;
    private String       value;
    private Integer      idAnnotationProperty;
    
    
    public Integer getIdAnnotationPropertyValue() {
      return idAnnotationPropertyValue;
    }
    public String getValue() {
      return value;
    }
    public Integer getIdAnnotationProperty() {
      return idAnnotationProperty;
    }
    public void setIdAnnotationPropertyValue(Integer idAnnotationPropertyValue) {
      this.idAnnotationPropertyValue = idAnnotationPropertyValue;
    }
    public void setValue(String value) {
      this.value = value;
    }
    public void setIdAnnotationProperty(Integer idAnnotationProperty) {
      this.idAnnotationProperty = idAnnotationProperty;
    }
   

}
