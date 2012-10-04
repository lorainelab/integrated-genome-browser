package com.affymetrix.igb.trackOperations;

import org.osgi.framework.BundleActivator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.SelectAllAction;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	static FileTypeCategory[] categories = new FileTypeCategory[]{FileTypeCategory.Graph, FileTypeCategory.Mismatch};
	
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		
		GraphTrackPanel tabPanel = new GraphTrackPanel(igbService) {
			@Override
			protected void selectAllButtonActionPerformedA(java.awt.event.ActionEvent evt) {
				SelectAllAction.getAction().execute(FileTypeCategory.Graph, FileTypeCategory.Mismatch);
			}
		};
		
		final GraphOperationsImpl trackOperation = new GraphOperationsImpl(igbService){
			@Override
			protected boolean addThisOperator(Operator operator){
				for(FileTypeCategory category : categories){
					if(operator.getOperandCountMin(category) > 0){
						return true;
					}
				}
				return false;
			}
		};
		tabPanel.addPanel(trackOperation);
		
		ExtensionPointHandler<Operator> operatorExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
		operatorExtensionPoint.addListener(new ExtensionPointListener<Operator>() {

			@Override
			public void addService(Operator operator) {
				trackOperation.addOperator(operator);
			}

			@Override
			public void removeService(Operator operator) {
				trackOperation.removeOperator(operator);
			}
		});
		
		
		return tabPanel;
	}
}
