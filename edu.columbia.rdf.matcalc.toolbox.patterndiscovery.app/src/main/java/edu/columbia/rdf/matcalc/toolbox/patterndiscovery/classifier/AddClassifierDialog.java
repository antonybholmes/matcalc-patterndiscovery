package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.classifier;

import javax.swing.Box;

import org.jebtk.core.text.TextUtils;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.modern.ModernComponent;
import org.jebtk.modern.UI;
import org.jebtk.modern.dialog.ModernDialogTaskWindow;
import org.jebtk.modern.dialog.ModernMessageDialog;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.panel.HBox;
import org.jebtk.modern.panel.VBoxAutoWidth;
import org.jebtk.modern.text.ModernAutoSizeLabel;
import org.jebtk.modern.text.ModernClipboardTextField;
import org.jebtk.modern.text.ModernTextBorderPanel;
import org.jebtk.modern.text.ModernTextField;
import org.jebtk.modern.window.WindowWidgetFocusEvents;
import edu.columbia.rdf.matcalc.GroupPanel;
import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.MatrixRowAnnotationCombo;


public class AddClassifierDialog extends ModernDialogTaskWindow {
	private static final long serialVersionUID = 1L;

	private ModernTextField mTextName = new ModernClipboardTextField();


	private XYSeriesGroup mGroups;

	private GroupPanel mGroupPanel;

	private MatrixRowAnnotationCombo mHeaderCombo;

	private AnnotationMatrix mM;


	public AddClassifierDialog(MainMatCalcWindow parent, AnnotationMatrix m) {
		super(parent);

		mM = m;
		
		setTitle("Classifier");
		
		mGroups = parent.getGroups();

		setup();

		createUi();
	}

	private void setup() {
		addWindowListener(new WindowWidgetFocusEvents(mOkButton));

		setSize(600, 340);

		UI.centerWindowToScreen(this);
	}

	private final void createUi() {
		ModernComponent box = VBoxAutoWidth.create();

		box.add(new HBox(new ModernAutoSizeLabel("Name", 100), 
				new ModernTextBorderPanel(mTextName, 300)));
		
		
		box.add(UI.createVGap(20));
		mGroupPanel = new GroupPanel(mGroups);
		box.add(new HBox(new ModernAutoSizeLabel("Groups", 100), mGroupPanel));
	
		box.add(UI.createVGap(20));
		mHeaderCombo = new MatrixRowAnnotationCombo(mM);
		box.add(new HBox(new ModernAutoSizeLabel("Annotation", 100), mHeaderCombo));
	
		setDialogCardContent(box);
	}

	@Override
	public final void clicked(ModernClickEvent e) {
		if (e.getMessage().equals(UI.BUTTON_OK)) {
			if (TextUtils.isNullOrEmpty(mTextName.getText())) {
				ModernMessageDialog.createWarningDialog(mParent, "The classifier must have a name.");
				
				return;
			}
		}
		
		super.clicked(e);
	}

	public XYSeries getGroup1() {
		return mGroupPanel.getGroup1();
	}
	
	public XYSeries getGroup2() {
		return mGroupPanel.getGroup2();
	}

	public String getClassifierName() {
		return mTextName.getText();
	}
	
	public String getAnnotation() {
		return mHeaderCombo.getText();
	}
}
