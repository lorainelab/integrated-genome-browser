package com.affymetrix.genometry;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author hiralv
 */
public class Das2AnnotatedSeqGroup extends AnnotatedSeqGroup {
	private Map<String, SymLoader> type_id2symloader = null;
	
	public Das2AnnotatedSeqGroup(String id){
		super(id);
	}
	
	public final void addSymLoader(String type, SymLoader value){
		if(type_id2symloader == null){
			type_id2symloader = new HashMap<String, SymLoader>();
		}
		type_id2symloader.put(type, value);
	}

	public final Set<String> getSymloaderList(){
		if(type_id2symloader == null){
			return Collections.<String>emptySet();
		}
		return type_id2symloader.keySet();
	}

	public final SymLoader getSymLoader(String type){
		if(type_id2symloader == null) {
			return null;
		}		
		return type_id2symloader.get(type);
	}

	public boolean  removeSymLoader(String type) {
		if(type_id2symloader == null || !type_id2symloader.containsKey(type)) {
			return false;
		}
		type_id2symloader.remove(type);
		return true;
	}
}
