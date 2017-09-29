package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.math.ui.matrix.transform.MatrixTransform;
import org.jebtk.modern.contentpane.CloseableHTab;
import org.jebtk.modern.contentpane.SizableContentPane;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;

public class PatternPanelTransform extends MatrixTransform {

	private PatternsPanel mPanel;

	public PatternPanelTransform(MainMatCalcWindow parent,
			PatternsPanel panel,
			AnnotationMatrix inputMatrix) {
		super(parent, "Show Patterns", inputMatrix);
		
		mPanel = panel;
	}
	
	@Override
	public void apply() {
		mParent.getTabsPane().getModel().getLeftTabs().clear();
		mParent.getTabsPane().getModel().getLeftTabs().addTab(new SizableContentPane("Pattern Discovery", 
				new CloseableHTab("Pattern Discovery", mPanel, mParent.getTabsPane()), 300, 200, 500));
	}

}
