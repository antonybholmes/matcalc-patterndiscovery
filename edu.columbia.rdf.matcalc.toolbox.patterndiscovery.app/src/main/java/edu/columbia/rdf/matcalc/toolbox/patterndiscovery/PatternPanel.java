package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.awt.Color;

import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.modern.ModernComponent;
import org.jebtk.modern.button.CheckBox;
import org.jebtk.modern.button.ModernCheckSwitch;

public class PatternPanel extends ModernComponent {
	private static final long serialVersionUID = 1L;
	
	private Pattern mPattern;
	
	private CheckBox mC;

	private String mTitle;

	public PatternPanel(XYSeries g, Pattern p, Color color) {
		mPattern = p;
		
		mTitle = g.getName() + 
				" <" + 
				p.getComb().size() + 
				", " + p.size() +
				"> (" + p.getComb() + ")";
		
		mC = new ModernCheckSwitch(mTitle, true);
		
		add(mC);
	}
	
	public Pattern getPattern() {
		return mPattern;
	}
	
	public boolean isSelected() {
		return mC.isSelected();
	}

	public String getTitle() {
		return mTitle;
	}

	public void setSelected(boolean selected) {
		mC.setSelected(selected);
	}
}
