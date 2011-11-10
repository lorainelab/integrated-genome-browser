package com.affymetrix.genometryImpl.symmetry;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;

public class UcscBedDetailSym extends UcscBedSym {
	private final String id;
	private String description;

	public UcscBedDetailSym(String type, BioSeq seq, int txMin, int txMax,
			String name, float score, boolean forward, int cdsMin, int cdsMax,
			int[] blockMins, int[] blockMaxs, String id, String description) {
		super(type, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins,
				blockMaxs);
		this.id = id;
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getID() {
		return id;
	}

	public Map<String,Object> cloneProperties() {
		Map<String,Object> tprops = super.cloneProperties();
		tprops.put("id", id);
		tprops.put("description", description);
		return tprops;
	}

	public Object getProperty(String key) {
		if (key.equals("id")) { return id; }
		else if (key.equals("description")) { return description; }
		else return super.getProperty(key);
	}

	protected void outputAdditional(DataOutputStream out) throws IOException  {
		out.write('\t');
		out.write(id.getBytes());
		out.write('\t');
		out.write(description.getBytes());
	}
}
