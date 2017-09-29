package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.classifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;

import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.xml.XmlUtils;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.math.cluster.DistanceMetric;
import org.jebtk.modern.UI;
import org.jebtk.modern.UIService;
import org.jebtk.modern.button.ModernButton;
import org.jebtk.modern.dialog.ModernDialogStatus;
import org.jebtk.modern.dialog.ModernDialogTaskWindow;
import org.jebtk.modern.dialog.ModernMessageDialog;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.graphics.icons.FolderVectorIcon;
import org.jebtk.modern.graphics.icons.SaveVectorIcon;
import org.jebtk.modern.io.FileDialog;
import org.jebtk.modern.io.RecentFilesService;
import org.jebtk.modern.panel.HBox;
import org.jebtk.modern.panel.HSpacedBox;
import org.jebtk.modern.panel.VBox;
import org.jebtk.modern.scrollpane.ModernScrollPane;
import org.jebtk.modern.scrollpane.ScrollBarPolicy;
import org.jebtk.modern.spinner.ModernCompactSpinner;
import org.jebtk.modern.text.ModernLabel;
import org.jebtk.modern.window.WindowWidgetFocusEvents;
import edu.columbia.rdf.matcalc.GroupsCombo;
import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.plot.heatmap.cluster.ClusterDistanceMetricCombo;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;


public class ClassifierDialog extends ModernDialogTaskWindow implements ModernClickListener {
	private static final long serialVersionUID = 1L;

	private ModernButton mLoadButton = 
			new ModernButton(UI.BUTTON_IMPORT, UIService.getInstance().loadIcon(FolderVectorIcon.class, 16));
	
	private ModernButton mExportButton = 
			new ModernButton(UI.BUTTON_EXPORT, UIService.getInstance().loadIcon(SaveVectorIcon.class, 16));
	
	private ModernButton mClearButton = 
			new ModernButton(UI.MENU_CLEAR, UIService.getInstance().loadIcon("delete", 16));

	private ClusterDistanceMetricCombo mDistanceCombo = 
			new ClusterDistanceMetricCombo();

	private XYSeriesGroup mGroups;

	private GroupsCombo mGroupCombo;

	private ModernCompactSpinner mPermField = 
			new ModernCompactSpinner(1, 100000, 10000);

	private ClassifierTable mClassifierTable;


	public ClassifierDialog(MainMatCalcWindow parent) {
		super(parent);

		setTitle("Classify");
		
		mGroups = parent.getGroups();
		
		//mClassifierCombo = new ClassifierCombo();
		mClassifierTable = new ClassifierTable();
		
		mGroupCombo = new GroupsCombo(mGroups);
		
		setup();

		createUi();
	}

	private void setup() {
		addWindowListener(new WindowWidgetFocusEvents(mOkButton));

		mLoadButton.addClickListener(new ModernClickListener() {
			@Override
			public void clicked(ModernClickEvent e) {
				try {
					open();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (SAXException e1) {
					e1.printStackTrace();
				} catch (ParserConfigurationException e1) {
					e1.printStackTrace();
				}
			}});
		
		mExportButton.addClickListener(new ModernClickListener() {
			@Override
			public void clicked(ModernClickEvent e) {
				try {
					save();
				} catch (TransformerException | ParserConfigurationException e1) {
					e1.printStackTrace();
				}
			}});
		
		mClearButton.addClickListener(new ModernClickListener() {
			@Override
			public void clicked(ModernClickEvent e) {
				clear();
			}});

		setSize(600, 500);

		UI.centerWindowToScreen(this);
	}



	private final void createUi() {
		Box box = VBox.create();

		//Box box2 = HBox.create();
		//box2.add(new ModernLabel("Classifier", 100));
		//box2.add(mClassifierCombo);
		//box.add(box2);
		//box.add(Ui.createVGap(10));
		
		sectionHeader("Classifiers", box);
		
		
		Box box2 = new HSpacedBox();
		box2.add(mLoadButton);
		box2.add(mExportButton);
		box2.add(mClearButton);
		box.add(box2);
		box.add(UI.createVGap(10));
		

		ModernScrollPane scrollPane = new ModernScrollPane(mClassifierTable)
				.setScrollBarPolicy(ScrollBarPolicy.NEVER, ScrollBarPolicy.ALWAYS);
		
		UI.setSize(scrollPane, 500, 180);
		
		box.add(scrollPane);
		

		midSectionHeader("Statistics", box);
		
		box.add(new HBox(new ModernLabel("Permutations", 150), mPermField));
		
		box.add(UI.createVGap(5));
		box.add(new HBox(new ModernLabel("Distance metric", 150), mDistanceCombo));
		
		setDialogCardContent(box);
		
		mDistanceCombo.setSelectedIndex(3);
	}

	//public XYSeries getGroup1() {
	//	return mGroups.get(group1Combo.getSelectedIndex());
	//}

	public List<Classifier> getClassifiers() {
		List<Classifier> ret = new ArrayList<Classifier>();
		
		for (int i = 0; i < mClassifierTable.getRowCount(); ++i) {
			if (mClassifierTable.getIsSelected(i)) {
				ret.add(ClassifierService.getInstance().get(mClassifierTable.getValueAt(i, 1).toString()));
			}
		}
		
		return CollectionUtils.sort(ret);
	}
	
	public XYSeries getGroup() {
		return mGroups.get(mGroupCombo.getSelectedIndex());
	}
	
	public int getPermutations() {
		return mPermField.getIntValue();
	}
	
	public DistanceMetric getDistanceMetric() {
		return mDistanceCombo.getDistanceMetric();
	}
	
	private void open() throws IOException, SAXException, ParserConfigurationException {
		Path file = FileDialog.open(mParent)
				.filter(new ClassifierGuiFileFilter())
				.getFile(RecentFilesService.getInstance().getPwd());
		
		open(FileUtils.newBufferedInputStream(file));
		
		//mClassifierCombo.refresh();
		mClassifierTable.refresh();
	}
	
	private static void open(InputStream is) throws SAXException, IOException, ParserConfigurationException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		ClassifierXmlHandler handler = new ClassifierXmlHandler();

		saxParser.parse(is, handler);	
	}
	
	private void save() throws TransformerException, ParserConfigurationException {
		Path file = FileDialog.save(mParent)
				.filter(new ClassifierGuiFileFilter())
				.getFile(RecentFilesService.getInstance().getPwd());
		
		if (FileUtils.exists(file)) {
			ModernDialogStatus status = ModernMessageDialog.createFileReplaceDialog(mParent, file);
			
			if (status == ModernDialogStatus.CANCEL) {
				return;
			}
		}
		
		save(file);
		
		ModernMessageDialog.createFileSavedDialog(mParent, file);
	}
	
	private final void save(Path file) throws TransformerException, ParserConfigurationException {
		Document doc = XmlUtils.createDoc();

		doc.appendChild(ClassifierService.getInstance().toXml(doc));

		XmlUtils.writeXml(doc, file);

		//LOG.info("Wrote settings to {}", Path.getAbsoluteFile());
	}
	
	private void clear() {
		ClassifierService.getInstance().clear();
	}
}
