package com.affymetrix.genometryImpl.thread;

import java.util.EventListener;

/**
 *
 * @author hiralv
 */
public interface CThreadListener extends EventListener{
	public void heardThreadEvent(CThreadEvent cte);
}
