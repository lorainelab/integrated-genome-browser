package apollo.datamodel;

import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

/**
 *
 * @author hiralv
 */
public class Sequence implements SequenceI {

	private String id;
	private String residues;
	public Sequence(String id, String residues) {
		this.id = id;
		this.residues = residues;
	}
	
	public int getLength() {
		return residues == null ? 0: residues.length();
	}

	public void setLength(int length) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getResidues() {
		return residues;
	}

	public String getResidues(int start, int end) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setResidues(String residues) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void clearResidues() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasResidues() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getResidueType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setResidueType(String res_type) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasResidueType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isAA() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SequenceI getSubSequence(int start, int end) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getName() {
		return id;
	}

	public void setName(String id) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasName() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getAccessionNo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setAccessionNo(String id) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void addDbXref(String db, String id, int isCurrent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void addDbXref(String db, String id) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Vector getDbXrefs() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getChecksum() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setChecksum(String checksum) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public char getBaseAt(int loc) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getDescription() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private String desc;
	public void setDescription(String desc) {
		this.desc = desc;
	}

	public String getReverseComplement() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isSequenceAvailable(long position) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getFrame(long position, boolean forward) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean usesGenomicCoords() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setRange(RangeI loc) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public RangeI getRange() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isLazy() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public HashMap getGenomicErrors() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isSequencingErrorPosition(int base_position) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SequenceEditI getSequencingErrorAtPosition(int base_position) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean addSequencingErrorPosition(String operation, int pos, String residue) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean addSequenceEdit(SequenceEditI seq_edit) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean removeSequenceEdit(SequenceEditI seqEdit) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getOrganism() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setOrganism(String organism) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setDate(Date update_date) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
