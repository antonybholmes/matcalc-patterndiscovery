package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.math.ui.matrix.transform.MatrixTransform;
import org.jebtk.modern.contentpane.CloseableHTab;
import org.jebtk.modern.contentpane.SizableContentPane;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;

public class PatternsPanelTransform extends MatrixTransform {

	private PatternsPanel mPanel;

	public PatternsPanelTransform(MainMatCalcWindow parent,
			PatternsPanel panel,
			AnnotationMatrix inputMatrix) {
		super(parent, "Sort by z-score", inputMatrix);
		
		mPanel = panel;
	}
	
	@Override
	public void apply() {
		mParent.getTabsPane().getModel().getLeftTabs().clear();
		mParent.getTabsPane().getModel().getLeftTabs().addTab(new SizableContentPane("Patterns", 
				new CloseableHTab("Patterns", mPanel, mParent.getTabsPane()), 250, 200, 500));
	}

}
