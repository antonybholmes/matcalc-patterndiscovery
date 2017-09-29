package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.classifier;

import java.util.ArrayList;
import java.util.List;

import org.jebtk.core.event.ChangeEvent;
import org.jebtk.core.event.ChangeListener;
import org.jebtk.modern.table.ModernSelectionTable;
import org.jebtk.modern.table.ModernSelectionTableModel;

public class ClassifierTable extends ModernSelectionTable implements ChangeListener {

	private static final long serialVersionUID = 1L;
	
	
	public ClassifierTable() {
		setShowHeader(false);
		
		ClassifierService.getInstance().addChangeListener(this);
		
		refresh();
	}


	@Override
	public void changed(ChangeEvent e) {
		refresh();
	}

	public void refresh() {
		List<String> names = new ArrayList<String>(ClassifierService.getInstance().getCount());
		
		for (String c : ClassifierService.getInstance()) {
			names.add(c);
		}
		
		setModel(new ModernSelectionTableModel("Classifier", names));
	}
	
	public boolean getIsSelected(int row) {
		return ((ModernSelectionTableModel)mModel).getIsSelected(row);
	}
}
