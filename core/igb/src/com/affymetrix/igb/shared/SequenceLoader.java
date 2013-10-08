package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genometryImpl.thread.CThreadWorker;

public class SequenceLoader extends CThreadWorker<Object, Void> {
	private SeqSpan span;
	private GenericActionDoneCallback doneback;

	public SequenceLoader(String msg, SeqSpan span, GenericActionDoneCallback doneback) {
		super(msg);
		this.span = span;
		this.doneback = doneback;
	}

	@Override
	protected Object runInBackground() {
		LoadResidueAction loadResidue = new LoadResidueAction(span, true);
		loadResidue.addDoneCallback(doneback);
		loadResidue.actionPerformed(null);
		loadResidue.removeDoneCallback(doneback);
		return null;
	}

	@Override
	protected void finished() {
	}
}
