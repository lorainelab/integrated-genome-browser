package com.affymetrix.genometryImpl.symmetry;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;

public class UcscBedDetailSym extends UcscBedSym {
	private final String geneName;
	private String description;

	public UcscBedDetailSym(String type, BioSeq seq, int txMin, int txMax,
			String name, float score, boolean forward, int cdsMin, int cdsMax,
			int[] blockMins, int[] blockMaxs, String geneName, String description) {
		super(type, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins,
				blockMaxs);
		this.geneName = geneName;
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGeneName() {
		return geneName;
	}

	public Map<String,Object> cloneProperties() {
		Map<String,Object> tprops = super.cloneProperties();
		tprops.put("gene name", geneName);
		tprops.put("description", description);
		return tprops;
	}

	public Object getProperty(String key) {
		if (key.equals("gene name")) { return geneName; }
		else if (key.equals("description")) { return description; }
		else return super.getProperty(key);
	}

	protected void outputAdditional(DataOutputStream out) throws IOException  {
		out.write('\t');
		out.write(geneName.getBytes());
		out.write('\t');
		out.write(description.getBytes());
	}
}
