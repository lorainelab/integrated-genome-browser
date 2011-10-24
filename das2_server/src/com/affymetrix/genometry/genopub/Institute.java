package com.affymetrix.genometry.genopub;


import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import java.io.*;

public class Institute implements Serializable {

	private static final long serialVersionUID = 1L;
	private Integer idInstitute;
	private String  name;
	private String  description;
	private String  isActive;

	public Integer getIdInstitute() {
		return idInstitute;
	}
	public void setIdInstitute(Integer idInstitute) {
		this.idInstitute = idInstitute;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getIsActive() {
		return isActive;
	}
	public void setIsActive(String isActive) {
		this.isActive = isActive;
	}
	public Document getXML(GenoPubSecurity genoPubSecurity) {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Institute");

		root.addAttribute("label",       this.getName() != null ? this.getName() : "");    
		root.addAttribute("idInstitute",   this.getIdInstitute().toString());
		root.addAttribute("name",         this.getName() != null ? this.getName() : "");				
		root.addAttribute("isActive",     this.getIsActive() != null ? this.getIsActive() : "");				
		root.addAttribute("canWrite",     genoPubSecurity.canWrite(this) ? "Y" : "N");

		return doc;
	}


}
