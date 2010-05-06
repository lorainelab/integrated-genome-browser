package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.TwoBitParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 */
public class TwoBit extends SymLoader {
	private File f = null;
	
	public TwoBit(URI uri) {
		super(uri);
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();
		f = LocalUrlCacher.convertURIToFile(uri);
	}

	@Override
	public LoadStrategy[] getLoadChoices() {
		LoadStrategy[] choices = {LoadStrategy.NO_LOAD, LoadStrategy.VISIBLE, LoadStrategy.CHROMOSOME};
		return choices;
	}

	@Override
	public List<BioSeq> getChromosomeList(){
		//init();
		return Collections.<BioSeq>emptyList();
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		init();

		ByteArrayOutputStream outStream = null;
		try {
			outStream = new ByteArrayOutputStream();
			TwoBitParser.parse(f, span.getStart(), span.getEnd(), outStream);
			byte[] bytes = outStream.toByteArray();
			return new String(bytes);
		} catch (Exception ex) {
			Logger.getLogger(TwoBit.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		} finally {
			GeneralUtils.safeClose(outStream);
		}
	}
}
