package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import org.jebtk.math.matrix.DataFrame;
import org.jebtk.math.ui.matrix.transform.MatrixTransform;
import org.jebtk.modern.tabs.CloseableTab;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;

public class PatternsPanelTransform extends MatrixTransform {

	private PatternsPanel mPanel;

	public PatternsPanelTransform(MainMatCalcWindow parent,
			PatternsPanel panel,
			DataFrame inputMatrix) {
		super(parent, "Patterns", inputMatrix);
		
		mPanel = panel;
	}
	
	@Override
	public void apply() {
		//mParent.getTabsPane().getModel().getLeftTabs().clear();
		//mParent.getTabsPane().getModel().getLeftTabs().addTab(new SizableContentPane("Pattern Discovery", 
		//		new CloseableHTab("Pattern Discovery", mPanel, mParent.getTabsPane()), 250, 200, 500));
		
		
		((MainMatCalcWindow)mParent).getLeftTabsModel().removeTab("Pattern Discovery");
		((MainMatCalcWindow)mParent).addLeftTab("Pattern Discovery", 'P', 
				new CloseableTab("Pattern Discovery", 
						mPanel, 
						((MainMatCalcWindow)mParent).getLeftTabsModel()));
	}

}
