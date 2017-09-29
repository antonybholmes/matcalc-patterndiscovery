package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.Box;

import org.jebtk.core.Properties;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.collections.CountMap;
import org.jebtk.core.tree.CheckTreeNode;
import org.jebtk.core.tree.TreeNode;
import org.jebtk.core.tree.TreeRootNode;
import org.jebtk.graphplot.figure.heatmap.legacy.CountGroup;
import org.jebtk.graphplot.figure.heatmap.legacy.CountGroups;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.graphplot.figure.series.XYSeriesModel;
import org.jebtk.math.matrix.AnnotatableMatrix;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.modern.ModernComponent;
import org.jebtk.modern.UI;
import org.jebtk.modern.button.CheckBox;
import org.jebtk.modern.button.ModernButton;
import org.jebtk.modern.button.ModernButtonGroup;
import org.jebtk.modern.button.ModernCheckBox;
import org.jebtk.modern.button.ModernCheckSwitch;
import org.jebtk.modern.button.ModernRadioButton;
import org.jebtk.modern.dialog.ModernDialogFlatButton;
import org.jebtk.modern.dialog.ModernMessageDialog;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.help.ModernDialogHelpButton;
import org.jebtk.modern.panel.VBox;
import org.jebtk.modern.scrollpane.ModernScrollPane;
import org.jebtk.modern.scrollpane.ScrollBarPolicy;
import org.jebtk.modern.text.ModernLabelBold;
import org.jebtk.modern.text.ModernSubHeadingLabel;
import org.jebtk.modern.tree.ModernCheckTree;
import org.jebtk.modern.tree.ModernTree;
import org.jebtk.modern.tree.ModernTreeCheckNodeRenderer;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;

public class PatternsPanel extends ModernComponent implements ModernClickListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	private List<Pattern> mPhenPatterns;
	private List<Pattern> mControlPatterns;

	private ModernRadioButton mCheckUnion =
			new ModernRadioButton("Union");

	private ModernRadioButton mCheckIntersect =
			new ModernRadioButton("Intersection", true);

	private CheckBox mCheckPlot =
			new ModernCheckSwitch("Plot", true);

	private ModernButton mButtonUpdate =
			new ModernDialogFlatButton("Update...");

	private AnnotationMatrix mM;


	private MainMatCalcWindow mWindow;


	private Map<Pattern, CheckBox> mPhenSelMap =
			new HashMap<Pattern, CheckBox>();

	private Map<Pattern, CheckBox> mConSelMap =
			new HashMap<Pattern, CheckBox>();


	private XYSeriesModel mGroups;


	private XYSeriesGroup mComparisonGroups;


	private Properties mProperties;

	public PatternsPanel(MainMatCalcWindow window,
			AnnotationMatrix m,
			XYSeries phenGroup, 
			XYSeries controlGroup,
			List<Pattern> phenPatterns,
			List<Pattern> controlPatterns,
			XYSeriesModel groups,
			XYSeriesGroup comparisonGroups,
			Properties properties) {
		mWindow = window;
		mM = m;

		mGroups = groups;
		mComparisonGroups = comparisonGroups;
		mProperties = properties;

		mPhenPatterns = phenPatterns;
		mControlPatterns = controlPatterns;
		
		Box box;
		
		box = VBox.create();
		
		
		box.add(mCheckPlot);
		
		//box.add(UI.createVGap(10));
		//box.add(new ModernSeparator());
		//box.add(UI.createVGap(10));
		
		box.add(UI.createVGap(10));
		
		box.add(mCheckIntersect);
		box.add(mCheckUnion);
		//box.add(box2);
		box.add(UI.createVGap(20));
		
		box.add(new ModernSubHeadingLabel("Patterns"));
		box.add(UI.createVGap(10));
		
		setHeader(box);
		
		
		box = VBox.create();
		loadPatterns(phenGroup, phenPatterns, mPhenSelMap, box);
		box.add(UI.createVGap(10));
		loadPatterns(controlGroup, controlPatterns, mConSelMap, box);
		ModernScrollPane scrollPane = new ModernScrollPane(box);
		scrollPane.setHorizontalScrollBarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollBarPolicy.AUTO_SHOW);
		
		/*
		loadPatterns(phenGroup, phenPatterns, mPhenSelMap);
		loadPatterns(controlGroup, controlPatterns, mConSelMap);
		
		
		ModernTree<Pattern> tree = createPatternsTree(phenGroup, 
				phenPatterns, 
				controlGroup, 
				controlPatterns);
		ModernScrollPane scrollPane = new ModernScrollPane(tree);
		scrollPane.setHorizontalScrollBarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollBarPolicy.AUTO_SHOW);
		*/
		
		setBody(scrollPane);
		
		
		box = VBox.create();
		box.add(UI.createVGap(20));
		box.add(mButtonUpdate);
		box.add(UI.createVGap(20));
		box.add(new ModernDialogHelpButton("patterndiscovery.help.url"));
		box.add(UI.createVGap(10));
		
		setFooter(box);
		
		setBorder(LARGE_BORDER);

		// Select the smallest patterns from each
		
		selectPattern(phenPatterns, mPhenSelMap);

		selectPattern(controlPatterns, mConSelMap);

		new ModernButtonGroup(mCheckUnion, mCheckIntersect);

		mButtonUpdate.addClickListener(this);

		//filter();
	}


	@Override
	public void clicked(ModernClickEvent e) {
		filter();
	}

	private void filter() {
		Collection<Integer> biggestCombGenes = new HashSet<Integer>();

		if (mCheckIntersect.isSelected()) {
			// Intersection of genes.
			
			int sn = countSelected(mPhenSelMap, mConSelMap);
			
			// See how many times each gene is included in a pattern
			// For the intersection, the sum of a gene must equal the
			// the of patterns it is supposed to be in.
			CountMap<Integer> countMap = CountMap.create();
			
			for (Pattern p : mPhenPatterns) {
				if (mPhenSelMap.get(p).isSelected()) {
					countMap.putAll(p);
				}
			}
			
			for (Pattern p : mControlPatterns) {
				if (mConSelMap.get(p).isSelected()) {
					countMap.putAll(p);
				}
			}

			for (int g : countMap.keySet()) {
				if (countMap.get(g) == sn) {
					biggestCombGenes.add(g);
				}
			}
		} else {
			// Union of all genes
			
			for (Pattern p : mPhenPatterns) {
				if (mPhenSelMap.get(p).isSelected()) {
					CollectionUtils.addAll(p, biggestCombGenes);
				}
			}

			for (Pattern p : mControlPatterns) {
				if (mConSelMap.get(p).isSelected()) {
					CollectionUtils.addAll(p, biggestCombGenes);
				}
			}
		}

		System.err.println("big comb " + biggestCombGenes.size());

		if (biggestCombGenes.size() == 0) {
			ModernMessageDialog.createWarningDialog(mWindow, 
					"No suitable patterns could be found.");
			return;
		}

		//
		// The rest of this method is boilerpoint code for sorting
		// and displaying the matrix and is not specific to pattern discovery


		// which indices occur in both groups
		List<Integer> biDirectionalIndices = 
				CollectionUtils.sort(biggestCombGenes); //Collections.emptyList();
		//CollectionUtils.sort(CollectionUtils.intersect(phenPatterns.keySet(), controlPatterns.keySet()));


		AnnotationMatrix patternM = 
				AnnotatableMatrix.copyRows(mM, biDirectionalIndices);




		if (mCheckPlot.isSelected()) {
			// Count how many are up or down

			double[] zscores = patternM.getRowAnnotationValues("Z-score");

			int c = -1;

			// Since the z-scores are ordered, simply find the inflection
			// point from positive to negative to know how many positive
			// and negative samples there are
			for (int i = 0; i < zscores.length; ++i) {
				if (zscores[i] < 0) {
					c = i;
					break;
				}
			}

			CountGroups countGroups = new CountGroups()
					.add(new CountGroup("up", 0, c - 1))
					.add(new CountGroup("down", c, zscores.length - 1));

			List<String> history = mWindow.getTransformationHistory();

			int index = mWindow.searchHistory("Show Patterns");

			// Replace history after control curves
			mWindow.addToHistory(index,
					new PatternDiscoveryPlotMatrixTransform(mWindow,
					patternM, 
					mGroups,
					mComparisonGroups, 
					XYSeriesModel.EMPTY_SERIES,
					countGroups,
					history,
					mProperties));
			
			mWindow.addToHistory("Results", patternM);
		}
	}

	/**
	 * Count how many patterns the user selected.
	 * 
	 * @param phenSelMap
	 * @param conSelMap
	 * @return
	 */
	private int countSelected(Map<Pattern, CheckBox> phenSelMap,
			Map<Pattern, CheckBox> conSelMap) {
		int ret = 0;
		
		for (Pattern p : phenSelMap.keySet()) {
			if (phenSelMap.get(p).isSelected()) {
				++ret;
			}
		}
		
		for (Pattern p : conSelMap.keySet()) {
			if (conSelMap.get(p).isSelected()) {
				++ret;
			}
		}
		
		return ret;
	}


	/**
	 * Select the patterns with the fewest genes and the greatest support.
	 * 
	 * @param patterns
	 * @param selMap
	 */
	private static void selectPattern(final List<Pattern> patterns,
			Map<Pattern, CheckBox> selMap) {
		// Select the smallest patterns from each

		Pattern maxPattern = maxPattern(patterns);

		selMap.get(maxPattern).setSelected(true);
	}
	
	private static Pattern maxPattern(final List<Pattern> patterns) {
		// Select the smallest patterns from each

		int mcs = 0;
		int mgs = Integer.MAX_VALUE;

		Pattern maxPattern = null;

		for (Pattern p : patterns) {
			int cs = p.getComb().size();
			int gs = p.size();

			if (cs >= mcs) {
				if (gs < mgs) {
					maxPattern = p;

					mgs = gs;
				}

				mcs = cs;
			}
		}

		return maxPattern;
	}
	
	private void loadPatterns(XYSeries group,
			List<Pattern> patterns,
			Map<Pattern, CheckBox> selMap,
			Box box) {
		box.add(new ModernLabelBold(group.getName())); //ModernSubHeadingLabel(group.getName()));
		box.add(UI.createVGap(5));

		for (Pattern p : patterns) {
			String label = "<" + p.getComb().size() + ", " + p.size() +"> (" + p.getComb() + ")";
			
			CheckBox c = new ModernCheckSwitch(label);

			selMap.put(p, c);
			box.add(c);
		}
	}
	
	private void loadPatterns(XYSeries group,
			List<Pattern> patterns,
			Map<Pattern, CheckBox> selMap) {
		for (Pattern p : patterns) {
			String label = "<" + p.getComb().size() + ", " + p.size() +"> (" + p.getComb() + ")";
			
			ModernCheckBox c = new ModernCheckBox(label);

			selMap.put(p, c);
		}
	}
	
	private ModernTree<Pattern> createPatternsTree(XYSeries phenGroup,
			List<Pattern> phenPatterns,
			XYSeries contGroup,
			List<Pattern> contPatterns) {
		
		
		ModernTree<Pattern> tree = new ModernCheckTree<Pattern>();
		tree.setNodeRenderer(new ModernTreeCheckNodeRenderer());
		
		TreeRootNode<Pattern> root = new TreeRootNode<Pattern>();
		
		TreeNode<Pattern> phenNode = 
				new CheckTreeNode<Pattern>(phenGroup.getName());
		
		root.addChild(phenNode);
		
		TreeNode<Pattern> contNode = 
				new CheckTreeNode<Pattern>(contGroup.getName());
		
		root.addChild(contNode);
		
		tree.setRoot(root);
		
		return tree;
	}
}
