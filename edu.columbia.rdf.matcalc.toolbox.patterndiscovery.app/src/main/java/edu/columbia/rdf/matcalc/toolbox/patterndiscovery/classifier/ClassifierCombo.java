package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.classifier;

import org.jebtk.modern.UI;
import org.jebtk.modern.combobox.ModernComboBox;
import org.jebtk.modern.widget.ModernWidget;

public class ClassifierCombo extends ModernComboBox {
	private static final long serialVersionUID = 1L;

	public ClassifierCombo() {
		for (String c : ClassifierService.getInstance()) {
			addScrollMenuItem(c);
		}
		
		UI.setSize(this, 200, ModernWidget.WIDGET_HEIGHT);
	}

	public void refresh() {
		clear();
		
		for (String c : ClassifierService.getInstance()) {
			addScrollMenuItem(c);
		}
		
		setSelectedIndex(0);
	}
}
