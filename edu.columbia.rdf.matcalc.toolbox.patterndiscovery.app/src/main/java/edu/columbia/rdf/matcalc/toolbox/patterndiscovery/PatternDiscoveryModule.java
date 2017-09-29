package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jebtk.core.Indexed;
import org.jebtk.core.IndexedInt;
import org.jebtk.core.Mathematics;
import org.jebtk.core.Properties;
import org.jebtk.core.collections.ArrayListCreator;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.collections.DefaultHashMap;
import org.jebtk.core.collections.DefaultHashMapCreator;
import org.jebtk.core.collections.DefaultTreeMap;
import org.jebtk.core.collections.HashMapCreator;
import org.jebtk.core.collections.TreeSetCreator;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.graphplot.figure.series.XYSeriesModel;
import org.jebtk.math.MathUtils;
import org.jebtk.math.matrix.AnnotatableMatrix;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.math.matrix.DoubleMatrix;
import org.jebtk.math.matrix.MatrixGroup;
import org.jebtk.math.matrix.utils.MatrixArithmetic;
import org.jebtk.math.matrix.utils.MatrixOperations;
import org.jebtk.math.statistics.KernelDensity;
import org.jebtk.math.statistics.NormKernelDensity;
import org.jebtk.math.statistics.Statistics;
import org.jebtk.modern.UIService;
import org.jebtk.modern.contentpane.CloseableHTab;
import org.jebtk.modern.contentpane.SizableContentPane;
import org.jebtk.modern.dialog.ModernDialogStatus;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.ribbon.RibbonLargeButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.CalcModule;
import edu.columbia.rdf.matcalc.toolbox.patterndiscovery.app.PatternDiscoveryIcon;
import edu.columbia.rdf.matcalc.toolbox.plot.heatmap.HeatMapProperties;

public class PatternDiscoveryModule extends CalcModule implements ModernClickListener {

	//private static final int DEFAULT_POINTS = 
	//		SettingsService.getInstance().getAsInt("pattern-discovery.cdf.points");

	//private static final List<Double> EVAL_POINTS =
	//		Linspace.generate(0, 1, DEFAULT_POINTS);

	private MainMatCalcWindow mWindow;

	private static final Logger LOG =
			LoggerFactory.getLogger(PatternDiscoveryModule.class);

	@Override
	public String getName() {
		return "Pattern Discovery";
	}

	@Override
	public void init(MainMatCalcWindow window) {
		mWindow = window;

		RibbonLargeButton button = new RibbonLargeButton("Pattern Discovery",
				UIService.getInstance().loadIcon(PatternDiscoveryIcon.class, 24),
				"Pattern Discovery",
				"Supervised differentially expressed genes.");
		button.addClickListener(this);
		mWindow.getRibbon().getToolbar("Statistics").getSection("Statistics").add(button);
	}

	@Override
	public void clicked(ModernClickEvent e) {
		try {
			patternDiscovery();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
	}

	private void patternDiscovery() throws IOException, ParseException {
		patternDiscovery(new HeatMapProperties());
	}

	/**
	 * Pattern discovery.
	 *
	 * @param properties the properties
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void patternDiscovery(Properties properties) throws IOException {
		XYSeriesModel groups = XYSeriesModel.create(mWindow.getGroups());

		if (groups.getCount() == 0) {
			MainMatCalcWindow.createGroupWarningDialog(mWindow);

			return;
		}

		XYSeriesModel rowGroups = XYSeriesModel.create(mWindow.getRowGroups());

		AnnotationMatrix m = mWindow.getCurrentMatrix();

		if (m == null) {
			return;
		}

		PatternDiscoveryDialog dialog = 
				new PatternDiscoveryDialog(mWindow, m, mWindow.getGroups());

		dialog.setVisible(true);

		if (dialog.getStatus() == ModernDialogStatus.CANCEL) {
			return;
		}

		// We are only interested in the opened matrix
		// without transformations.

		//resetHistory();

		if (dialog.getReset()) {
			mWindow.resetHistory();
		}

		XYSeries g1 = dialog.getPhenGroup();
		XYSeries g2 = dialog.getControlGroup();

		//double snr = dialog.getMinSNR();
		double delta = dialog.getDelta();
		int support1 = dialog.getPhenSupport();
		boolean support1Only = dialog.getPhenSupportMinOnly();
		int support2 = dialog.getControlSupport();
		boolean support2Only = dialog.getControlSupportMinOnly();
		int minGenes = dialog.getGenes();
		double minZ = dialog.getMinZScore();

		//int percentile = dialog.getPercentile();
		//boolean plot = dialog.getPlot();
		boolean isLogData = dialog.getIsLogData();
		//boolean bidirectional = dialog.getBidirectional();

		patternDiscovery(m, 
				delta, 
				g1, 
				g2, 
				groups,
				rowGroups,
				support1,
				support1Only,
				support2,
				support2Only,
				minGenes,
				minZ,
				isLogData,
				properties);
	}

	/**
	 * Discover differential expression patterns.
	 * 
	 * @param m						The matrix to analyze.
	 * @param delta					The max separation between experiments.
	 * @param phenGroup				The columns in the phenotype group.
	 * @param controlGroup			The columns in the control group.
	 * @param groups				All the groups in the matrix.
	 * @param rowGroups				Any row groups.
	 * @param phenSupport			Minimum support in the phenotype group.
	 * @param phenSupportOnly		Comb must contain exactly minimum support.
	 * @param controlSupport		Minimum support in the control group.
	 * @param controlSupportOnly	Comb must contain exactly minimum support.
	 * @param minGenes				Minimum number of genes in pattern.
	 * @param minZ					Minimum z-score.
	 * @param isLogData				Is data log transformed.
	 * @param properties			Heat map properties.
	 * @throws IOException
	 */
	public void patternDiscovery(AnnotationMatrix m,
			double delta,
			XYSeries phenGroup, 
			XYSeries controlGroup,
			XYSeriesModel groups,
			XYSeriesModel rowGroups,
			int phenSupport,
			boolean phenSupportOnly,
			int controlSupport,
			boolean controlSupportOnly,
			int minGenes,
			double minZ,
			boolean isLogData,
			Properties properties) throws IOException {

		XYSeriesGroup comparisonGroups = new XYSeriesGroup();
		comparisonGroups.add(phenGroup);
		comparisonGroups.add(controlGroup);

		// Order table by groups

		List<Integer> indices = 
				MatrixGroup.findAllColumnIndices(m, comparisonGroups);

		// Rule of thumb, lets look at genes where at least half the
		// samples are non zero
		//AnnotationMatrix filterM = m; //mWindow.addToHistory("Min Exp Filter", MatrixOperations.minExpFilter(m, 0.01, indices.size() / 2));


		//
		// Filter z scores to ensure min z scores
		//

		List<Double> zscores = DoubleMatrix.diffGroupZScores(m, 
				phenGroup, 
				controlGroup);

		AnnotationMatrix zscoresM = new AnnotatableMatrix(m);
		zscoresM.setNumRowAnnotations("Z-score", zscores);

		mWindow.addToHistory("Z-score", zscoresM);

		List<Indexed<Integer, Double>> zscoresIndexed = 
				CollectionUtils.index(zscores);

		indices = new ArrayList<Integer>();

		for (Indexed<Integer, Double> index : zscoresIndexed) {
			if (Math.abs(index.getValue()) > minZ) {
				indices.add(index.getIndex());
			}
		}

		// Filter the zscores and re-index
		zscores = CollectionUtils.subList(zscores, indices);
		zscoresIndexed = CollectionUtils.index(zscores);

		AnnotationMatrix zScoreFilteredM = mWindow.addToHistory("Filter z-score", 
				AnnotatableMatrix.copyRows(zscoresM, indices));


		//
		// Fold Changes
		//

		List<Double> foldChanges;

		if (isLogData) {
			foldChanges = DoubleMatrix.logFoldChange(zScoreFilteredM, phenGroup, controlGroup);
		} else {
			foldChanges = DoubleMatrix.foldChange(zScoreFilteredM, phenGroup, controlGroup);
		}

		// filter by fold changes
		// filter by fdr

		String name = isLogData ? "Log fold change" : "Fold change";

		AnnotationMatrix foldChangesM = new AnnotatableMatrix(zScoreFilteredM);
		foldChangesM.setNumRowAnnotations(name, foldChanges);

		mWindow.addToHistory(name, foldChangesM);




		//
		// Order by pos and negative z score
		//

		List<Indexed<Integer, Double>> posZScores = 
				CollectionUtils.reverseSort(CollectionUtils.subList(zscoresIndexed, MathUtils.gt(zscoresIndexed, 0)));


		List<Indexed<Integer, Double>> negZScores = 
				CollectionUtils.sort(CollectionUtils.subList(zscoresIndexed, MathUtils.lt(zscoresIndexed, 0)));


		// Now make a list of the new zscores in the correct order,
		// positive decreasing, negative, decreasing
		List<Indexed<Integer, Double>> sortedZscores = 
				CollectionUtils.append(posZScores, negZScores);

		// Put the zscores in order
		indices = IndexedInt.indices(sortedZscores);

		zscores = CollectionUtils.subList(zscores, indices);

		System.err.println("zscore " + zscores + " " + indices);

		AnnotationMatrix zScoreSortedM = mWindow.addToHistory("Sort by z-score", 
				AnnotatableMatrix.copyRows(foldChangesM, indices));


		//
		// Normalize control and phenotype by max in control group
		//

		AnnotationMatrix phenStandardNormM = zScoreSortedM; //mWindow.addToHistory("Normalize To Control", normalize(orderM, controlGroup));

		AnnotationMatrix phenNormM = mWindow.addToHistory("Build phenotype curves", 
				normPhenToControl(phenStandardNormM, phenGroup, controlGroup));

		//if (true) {
		///	return;
		//}

		// Where the phenotype stands out from the control
		Map<Integer, Map<Comb, Set<Integer>>> phenPatterns = 
				patterns(phenNormM, delta, phenSupport, phenSupportOnly, minGenes);

		//Map<Integer, Pattern> phenMaxPatterns = maximalPatterns(phenPatterns, minGenes);

		//Map<Comb, Collection<Integer>> phenCombMap = 
		//		organizeGenesByComb(phenMaxPatterns);

		AnnotationMatrix controlStandardNormM = zScoreSortedM; //mWindow.addToHistory("Normalize To Phenotype", normalize(orderM, phenGroup));

		AnnotationMatrix controlNormM = mWindow.addToHistory("Build control curves", 
				normPhenToControl(controlStandardNormM, controlGroup, phenGroup));



		// Where the control stands out from the phenotype. Should be similar
		// to phen patterns but cannot be guaranteed.
		Map<Integer, Map<Comb, Set<Integer>>> controlPatterns = 
				patterns(controlNormM, delta, controlSupport, controlSupportOnly, minGenes);

		//Map<Integer, Pattern> controlMaxPatterns = 
		//		maximalPatterns(controlPatterns, minGenes);

		//Map<Comb, Collection<Integer>> controlCombMap = 
		//		organizeGenesByComb(controlMaxPatterns);

		/// See if there are any combs in both
		//List<Comb> biDirectionalCombs = CollectionUtils.intersect(phenCombFilteredMap, controlCombFilteredMap);

		/*

		// Find the largest list of genes shared bidirectionally by the combs

		Collection<Integer> biggestCombGenes = Collections.emptySet();

		if (bidirectional) {
			for (Comb phenComb : phenCombMap.keySet()) {
				for (Comb controlComb : controlCombMap.keySet()) {
					System.err.println("phen comb " + phenComb.size() + " " + controlComb.size());

					List<Integer> genes = CollectionUtils.intersect(phenCombMap.get(phenComb), 
							controlCombMap.get(controlComb));

					if (genes.size() >= minGenes && 
							genes.size() > biggestCombGenes.size()) {
						//biggestComb = phenComb;
						biggestCombGenes = genes; //CollectionUtils.sort(genes);
					}
				}
			}
		} else {
			biggestCombGenes = new HashSet<Integer>();

			for (Comb c : phenCombMap.keySet()) {
				biggestCombGenes.addAll(phenCombMap.get(c));
			}

			for (Comb c : controlCombMap.keySet()) {
				biggestCombGenes.addAll(controlCombMap.get(c));
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

		AnnotationMatrix patternM = mWindow.addToHistory("Create Pattern",
				"delta: " + delta + ", min phenotype support: " + phenSupport + ", min control support: " + controlSupport + ", min genes: " + minGenes,
				AnnotatableMatrix.copyInnerRows(zScoreSortedM, biDirectionalIndices));

		 */

		PatternsPanel patternsPanel = new PatternsPanel(mWindow, 
				zScoreSortedM,
				phenGroup, 
				controlGroup,
				sortPatterns(phenPatterns), 
				sortPatterns(controlPatterns),
				groups,
				comparisonGroups,
				properties);

		mWindow.addToHistory(new PatternPanelTransform(mWindow,
				patternsPanel,
				zScoreSortedM));

		/*
		if (plot) {
			CountGroups countGroups = new CountGroups()
					.add(new CountGroup("up", 0, posZScores.size() - 1))
					.add(new CountGroup("down", posZScores.size(), indices.size() - 1));

			List<String> history = mWindow.getTransformationHistory();

			mWindow.addToHistory(new PatternDiscoveryPlotMatrixTransform(mWindow,
					zScoreSortedM, 
					groups,
					comparisonGroups, 
					rowGroups,
					countGroups,
					history,
					properties));
		}

		
		 */
		
		//mWindow.addToHistory("Results", zScoreSortedM);
	}

	public void setLeftPane(List<Pattern> phenPatternMap,
			List<Pattern> controlPatternMap) {

	}

	/**
	 * Normalize the matrix row wise so the control group is bounded by
	 * [0, 1] and the phenotype group is relative to the control.
	 * 
	 * @param m
	 * @param g
	 * @return
	 */
	private AnnotationMatrix normalize(AnnotationMatrix m, MatrixGroup g) {
		// clone the inner matrix as well since we want to modify it
		AnnotationMatrix ret = new AnnotatableMatrix(m, true);

		List<Integer> columns = MatrixGroup.findColumnIndices(m, g);

		for (int i = 0; i < m.getRowCount(); ++i) {
			double max = MatrixOperations.max(ret, i, columns);

			if (max > 0) {
				// Divide the rows
				MatrixArithmetic.divide(i, max, ret);
			}
		}

		return ret;
	}

	/**
	 * Keep only those combs which contain a minimum number of genes.
	 * 
	 * @param combMap
	 * @param minGenes
	 * @return
	 */
	/*
	private static Map<Comb, Collection<Integer>> filterCombs(Map<Comb, 
			Collection<Integer>> combMap, 
			int minGenes) {
		Map<Comb, Collection<Integer>> ret = 
				DefaultTreeMap.create(new TreeSetCreator<Integer>());

		for (Comb c : combMap.keySet()) {
			if (combMap.get(c).size() > minGenes) {
				ret.put(c, combMap.get(c));
			}
		}

		return combMap;
	}
	 */

	/**
	 * Organize genes by combs so we now have groupings telling us what
	 * experiment groups contain which genes.
	 * 
	 * @param controlPatterns
	 * @return 
	 */
	private Map<Comb, Set<Integer>> organizeGenesByComb(Map<Integer, Pattern> patterns) {
		Map<Comb, Set<Integer>> combMap = 
				DefaultTreeMap.create(new TreeSetCreator<Integer>());

		for (int support : patterns.keySet()) {
			Pattern p = patterns.get(support);

			Comb c = p.getComb();

			for (int g : p) {
				combMap.get(c).add(g);
			}
		}

		return combMap;
	}

	/**
	 * Returns the maximal pattern by support size.
	 * 
	 * @param m
	 * @param delta
	 * @param minSupport
	 * @param minGenes
	 * @return
	 */
	public static Map<Integer, Map<Comb, Set<Integer>>> patterns(final AnnotationMatrix m, 
			double delta,
			int minSupport,
			boolean minSupportOnly,
			int minGenes) {
		int ng = m.getRowCount();
		int ne = m.getColumnCount();

		// Normalize control

		Map<Integer, List<Comb>> elementaryPatterns = 
				elementaryPatterns(m, ne, delta, minSupport, minSupportOnly);

		LOG.info("Creating Patterns...");

		// Make a list of all genes with patterns

		

		Map<Integer, Map<Comb, Set<Integer>>> maximalPatterns = growPatterns2(ng,
				ne,
				elementaryPatterns,
				minSupport,
				minSupportOnly,
				minGenes);

		/*
		for (Pattern pattern : maximalPatterns) {
			double p = p(pattern.getComb().size(), 
					pattern.size(), 
					m.getColumnCount(), 
					n, 
					delta);

			System.err.println("p " + pattern.getComb() + " " + p);
		}
		 */


		/*
		for (int sg = 0; sg < n; ++sg) {
			List<Pattern> sourceGeneSeeds = (List<Pattern>)elementaryPatterns.get(sg);

			if (sourceGeneSeeds.size() == 0) {
				continue;
			}

			List<Pattern> genePatterns = new ArrayList<Pattern>();

			for (Pattern elemSourcePattern : sourceGeneSeeds) {
				List<Pattern> twoGenePatterns = new ArrayList<Pattern>();

				for (int tg = sg + 1; tg < m.getRowCount(); ++tg) {
					Collection<Pattern> targetGeneSeeds = 
							elementaryPatterns.get(tg);

					//System.err.println("tg " + tg + " " + targetGeneSeeds);

					if (targetGeneSeeds.size() == 0) {
						continue;
					}

					for (Pattern elemTargetPattern : targetGeneSeeds) {
						Pattern newPattern = Pattern.intersect(elemSourcePattern, elemTargetPattern);

						//System.err.println("test pattern " + newPattern.getGenes().size() + " " + newPattern.getComb().size());

						if (newPattern.getComb().size() >= minSupport && 
								usedCombs.isExpCombNew(newPattern)) {
							twoGenePatterns.add(newPattern);
							usedCombs.addComb(newPattern);
						}
					}
				}

				System.err.println("new pattern " + twoGenePatterns.size());

				genePatterns.addAll(growPatterns(twoGenePatterns, minSupport, minGenes, usedCombs));
			}

			//genePatterns.addAll(growPatterns(sourceGeneSeeds, minSupport, minGenes, usedCombs));

			if (genePatterns.size() > 0) {
				for (Pattern p : genePatterns) {
					for (int gene : p) {
						maximalPatterns.put(gene, genePatterns);
					}
				}

				System.err.println("maximalPatterns " + sg + " " + genePatterns.size());
			}

			// For testing only
			//if (sourceGeneSeeds.size() > 0) {
			//	break;
			//}
		}
		 */

		// For each gene, we know which patterns/experiments it supports
		return maximalPatterns;
	}

	private static List<Pattern> growPatterns(List<Pattern> twoGenePatterns,
			int minSupport,
			int minGenes,
			LeftMaximalCombs usedCombs) {
		List<Pattern> maximalPatterns = new ArrayList<Pattern>();

		// The method as written is extremely inefficient since it needlessly
		// generates all possible tests at once n(n-1)/2. Instead we
		// take a pattern and grow it until it is maximal, then add it
		// to the solutions and move to the next.

		for (int sg = 0; sg < twoGenePatterns.size(); ++sg) {
			Pattern source = twoGenePatterns.get(sg);

			List<Integer> genes = new ArrayList<Integer>(twoGenePatterns.size());

			genes.add(sg);

			for (int tg = 1; tg < twoGenePatterns.size(); ++tg) {
				Pattern target = twoGenePatterns.get(tg);

				Comb newComb = Comb.intersect(source, target);

				if (newComb.size() >= minSupport && 
						usedCombs.isExpCombNew(newComb)) {
					// Hurrah, this is still a good pattern

					// change the source to reflect we have increased its
					// size

					///Pattern newPattern = new Pattern(newComb);
					//newPattern.addAll(source.getGenes());
					//newPattern.addAll(target.getGenes());

					//source = newPattern;
					//usedCombs.addComb(newPattern);

					genes.add(tg);
				}
			}

			if (genes.size() > minGenes) {
				source = new Pattern(genes, source.getComb());

				maximalPatterns.add(source);
			}



			// If we were success at increasing the source size, we can
			// add it to the maximal patterns
			//if (source.getComb().size() >= minSupport &&
			//		source.size() > minGenes &&
			//		usedCombs.isExpCombNew(source)) {
			//	maximalPatterns.add(source);
			//}
		}

		// Questionable method written from paper algorithm.
		// Highlighting why algorithms written in papers are a terrible
		// way to explain a concept.

		/*
		Deque<List<Pattern>> stack = new ArrayDeque<List<Pattern>>();

		stack.push(twoGenePatterns);



		Map<Pattern, Boolean> keepMap = DefaultHashMap.create(true);

		System.err.println("prev " + twoGenePatterns.size());

		while (!stack.isEmpty()) {
			List<Pattern> prevLevelPatterns = stack.pop();

			System.err.println("size " + prevLevelPatterns.size());

			while (prevLevelPatterns.size() > 0) {
				Pattern sourcePattern = prevLevelPatterns.get(0);

				prevLevelPatterns = CollectionUtils.tail(prevLevelPatterns); //new TailList<Pattern>(prevLevelPatterns);

				//LOG.info("{} previous patterns left...", prevLevelPatterns.size() + " " + usedCombs.size() + " " + maximalPatterns.size() +  " " + usedCombs.values().iterator().next().size());

				//if (prevLevelPatterns.size() % 1000 == 0) {
				//	LOG.info("{} previous patterns left...", prevLevelPatterns.size());
				//}


				// (B)
				if (!keepMap.get(sourcePattern)) {
					continue;
				}

				// H
				//if (!usedCombs.isExpCombNew2(sourcePattern)) {
				//	continue;
				//}

				List<Pattern> nextLevelPatterns = new ArrayList<Pattern>();

				for (Pattern targetPattern : prevLevelPatterns) {
					if (!keepMap.get(targetPattern)) {
						continue;
					}

					Pattern newPattern = 
							Pattern.intersect(sourcePattern, targetPattern);

					System.err.println("huh " + newPattern.getGenes().size() + " " + newPattern.getComb().size() + " " + keepMap.size());

					if (newPattern.getComb().size() >= support && 
							usedCombs.isExpCombNew(newPattern)) {
						if (sourcePattern.getComb().size() == newPattern.getComb().size()) {
							keepMap.put(sourcePattern, false);
						}

						if (targetPattern.getComb().size() == newPattern.getComb().size()) {
							keepMap.put(targetPattern, false);
						}

						//System.err.println("hmm " + newPattern.getGenes().size());

						nextLevelPatterns.add(newPattern);
						usedCombs.addComb(newPattern);
					}
				}



				if (keepMap.get(sourcePattern)) {
					//System.err.println("hmm " + sourcePattern + " " + keepMap.get(sourcePattern));

					//if (prevLevelPatterns.size() == 0) {
					maximalPatterns.add(sourcePattern);
					//usedCombs.addComb(sourcePattern);
				}

				if (nextLevelPatterns.size() > 0) {
					stack.push(prevLevelPatterns);
					stack.push(nextLevelPatterns);
				}
			}
		}
		 */

		return maximalPatterns;
	}

	/**
	 * Returns patterns mapped to the support size. If minSupportOnly is
	 * true, then each pattern will be the largest pattern with a support
	 * exactly equal to minSupport, otherwise it will be the largest pattern
	 * with a support greater than or equal to minSupport.
	 * 
	 * @param ng						The number of genes
	 * @param ne						The number of experiments
	 * @param elementaryPatterns
	 * @param minSupport
	 * @param minGenes
	 * @param usedCombs
	 * @return
	 */
	private static Map<Integer, Map<Comb, Set<Integer>>> growPatterns2(int ng,
			int ne,
			Map<Integer, List<Comb>> elementaryPatterns,
			int minSupport,
			boolean minSupportOnly,
			int minGenes) {


		// The method as written in the paper seems extremely inefficient 
		// since it needlessly generates all possible tests at once n(n-1)/2. 
		// Instead we take a pattern and grow it until it is maximal, then 
		// add it to the solutions and move to the next.

		// Store the maximal pattern for combs of a given size
		Map<Integer, Map<Comb, Set<Integer>>> allPatternsMap = 
				DefaultHashMap.create(new DefaultHashMapCreator<Comb, Set<Integer>>(new TreeSetCreator<Integer>()));

		for (int support = minSupport; support <= ne; ++support) {
			System.err.println("Support " + support);
			
			Map<Integer, Map<Comb, Set<Integer>>> patternMap = 
					DefaultHashMap.create(new DefaultHashMapCreator<Comb, Set<Integer>>(new TreeSetCreator<Integer>()));
			
			for (int sg = 0; sg < ng; ++sg) {

				Collection<Comb> sources = elementaryPatterns.get(sg);

				if (sources.size() == 0) {
					continue;
				}


				// Iterate over all the combs for this starting gene and try to
				// build the largest possible pattern we can
				for (Comb source : sources) {
					
					int sn = source.size();
					
					// Must have a support at least equal to what we want
					if (sn < support) {
						continue;
					}

					// Don't start a new pattern if the gene has already been added
					// to a pattern created from a comb of a previous gene since
					// it cannot be any larger
					if (isSuperComb(source, patternMap)) {
						continue;
					}

					//genes.add(sg);

					// Seed the pattern with the comb of the source gene
					patternMap.get(source.size()).get(source).add(sg);

					for (int tg = sg + 1; tg < ng; ++tg) {
						Collection<Comb> targets = elementaryPatterns.get(tg);

						if (targets.size() == 0) {
							continue;
						}

						for (Comb target : targets) {
							int tn = target.size();
							
							if (tn < support) {
								continue;
							}
							
							Comb intersectionComb = 
									CombService.getInstance().intersect(source, target);

							int n = intersectionComb.size();

							if (n >= support) {
								patternMap.get(n).get(intersectionComb).add(tg);
							}
						}
					}
				}

				if (sg % 1000 == 0) {
					LOG.info("Processed {} genes...", sg);
				}
			}
			
			// Union of all patterns found so far
			for (int cs : patternMap.keySet()) {
				for (Comb c : patternMap.get(cs).keySet()) { 
					allPatternsMap.get(cs).put(c, patternMap.get(cs).get(c));
				}
			}
			
			if (minSupportOnly) {
				break;
			}

		}

		System.err.println("sizes " + allPatternsMap.keySet());

		//return maxPatternMap;
		return filterPatterns(allPatternsMap, minGenes);
	}

	/**
	 * We test whether a comb is a super pattern of any of the existing
	 * combs we have created. Since the genes are processed in the same order,
	 * if a comb is a super comb (equal to or bigger than an existing comb) of
	 * another, then it must have been added to the pattern of a previous
	 * gene. If this is the case, this new pattern cannot be bigger than
	 * an existing one since the existing pattern must contain at least one
	 * more gene. We can therefore abandon creating a pattern using this
	 * comb as a starting point.
	 * 
	 * @param source
	 * @param geneMap
	 * @return
	 */
	private static boolean combsMatch(Comb source, 
			Map<Integer, Map<Comb, Collection<Integer>>> geneMap) {
		for (int s : geneMap.keySet()) {
			for (Comb c : geneMap.get(s).keySet()) {
				if (Comb.combsMatch(c, source)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * We test whether a comb is a super pattern of any of the existing
	 * combs we have created. Since the genes are processed in the same order,
	 * if a comb is a super comb (equal to or bigger than an existing comb) of
	 * another, then it must have been added to the pattern of a previous
	 * gene. If this is the case, this new pattern cannot be bigger than
	 * an existing one since the existing pattern must contain at least one
	 * more gene. We can therefore abandon creating a pattern using this
	 * comb as a starting point.
	 * 
	 * @param source
	 * @param patternMap
	 * @return
	 */
	private static boolean isSuperComb(Comb source,
			Map<Integer, Map<Comb, Set<Integer>>> patternMap) {

		for (int cs : patternMap.keySet()) {
			for (Comb c : patternMap.get(cs).keySet()) {
				if (Comb.isSuper(c, source)) {
					return true;
				}
			}
		}

		return false;
	}



	/**
	 * Generate all elementary patterns, that is the patterns for each gene
	 * with minimum support based on the delta. Genes without a requisite
	 * pattern, will not have an entry in the map. This saves checking
	 * elementary patterns that cannot yield a usable solution.
	 * 
	 * @param m
	 * @param delta
	 * @param minSupport
	 * @return
	 */
	private static Map<Integer, List<Comb>> elementaryPatterns(AnnotationMatrix m,
			int ne,
			double delta,
			int minSupport,
			boolean minSupportOnly) {
		Map<Integer, List<Comb>> combMap =
				DefaultHashMap.create(new ArrayListCreator<Comb>());

		for (int sg = 0; sg < m.getRowCount(); ++sg) {
			// Sort values in order and keep track of which sample (column)
			// they are
			List<Indexed<Integer, Double>> p1 = 
					CollectionUtils.sort(CollectionUtils.index(m.rowAsDouble(sg)));

			// Try to find all possible supports of a given size for the
			// gene
			for (int support = minSupport; support <= ne; ++support) {
				for (int e1 = 0; e1 < ne; ++e1) {
					int clusterSize = 1;

					for (int e2 = e1 + 1; e2 < ne; ++e2) {
						double d = p1.get(e2).getValue() - p1.get(e1).getValue();

						if (d > delta) {
							break;
						}

						++clusterSize;
					}

					// This run of samples are sufficiently close to each other
					// that they form a group with support = minSupport
					if (clusterSize == support) {
						//if (clusterSize > 1) {
						// A cluster must have at least 2 samples

						List<Integer> indices = new ArrayList<Integer>();

						for (int i = e1; i < e1 + clusterSize; ++i) {
							indices.add(p1.get(i).getIndex());
						}

						combMap.get(sg).add(CombService.getInstance().createComb(indices));
						//System.err.println("max pattern " + sg + " " + pattern + " " + Matrix.rowToList(m, sg));
					}
				}
			}
		}

		if (minSupportOnly) {
			// prune genes with combs greater than minsupport

			System.err.println("Min Support Prune");
			
			List<Integer> rem = new ArrayList<Integer>();

			for (int sg : combMap.keySet()) {
				for (Comb c : combMap.get(sg)) {
					if (c.size() > minSupport) {
						rem.add(sg);
						break;
					}
				}
			}

			for (int sg : rem) {
				combMap.remove(sg);
			}
		}

		return combMap;
	}

	/*
	private static AnnotationMatrix subMatrix(final AnnotationMatrix m, 
			final MatrixGroup phenGroup,
			final MatrixGroup controlGroup) {
		List<Integer> phenIndices = 
				MatrixGroup.findColumnIndices(m, phenGroup);

		List<Integer> controlIndices = 
				MatrixGroup.findColumnIndices(m, controlGroup);

		AnnotationMatrix ret = AnnotatableMatrix.createNumericalMatrix(m.getRowCount(), 
				phenIndices.size() + controlIndices.size());

		AnnotationMatrix.copyRowAnnotations(m, ret);

		//
		// First copy column names
		//

		int pc = 0;

		for (int c : phenIndices) {
			//ret.setColumnName(pc, m.getColumnName(c));

			ret.copyColumn(m, c, pc); 

			++pc;
		}

		for (int c : controlIndices) {
			//ret.setColumnName(pc, m.getColumnName(c));
			//ret.copyColumn(m, c, pc);

			ret.copyColumn(m, c, pc); 

			++pc;
		}

		return ret;
	}
	 */

	/**
	 * Normalize based on genes@work paper. The returned matrix will
	 * contain only the columns associated with the phenotype group.
	 * 
	 * @param m
	 * @param phenGroup
	 * @param controlGroup
	 * @return
	 */
	private static AnnotationMatrix normPhenToControl(final AnnotationMatrix m, 
			final MatrixGroup phenGroup,
			final MatrixGroup controlGroup) {
		List<Integer> phenIndices = 
				MatrixGroup.findColumnIndices(m, phenGroup);

		List<Integer> controlIndices = 
				MatrixGroup.findColumnIndices(m, controlGroup);

		AnnotationMatrix ret = AnnotatableMatrix.createNumericalMatrix(m.getRowCount(), 
				phenIndices.size());

		AnnotationMatrix.copyRowAnnotations(m, ret);

		//
		// First copy column names
		//

		int pc = 0;

		for (int c : phenIndices) {
			ret.setColumnName(pc++, m.getColumnName(c));
		}

		//
		// Now normalize phen to control signal
		//


		for (int i = 0; i < m.getRowCount(); ++i) {
			double[] phenPoints = 
					CollectionUtils.subList(m.rowAsDouble(i), phenIndices);

			double[] controlPoints = 
					CollectionUtils.subList(m.rowAsDouble(i), controlIndices); //MatrixOperations.rowToList(m, i, controlIndices);

			Arrays.sort(phenPoints);
			Arrays.sort(controlPoints);

			//System.err.println("aha " + m.getRowName(i) + " " + Arrays.toString(controlPoints));

			double[] p1y;


			if (Mathematics.sum(phenPoints) == 0) {
				p1y = Mathematics.zerosArray(phenPoints.length);
			} else {
				KernelDensity density = new NormKernelDensity(controlPoints);

				// evaluate p1 normalizing to p2
				p1y = density.cdf(phenPoints);
			}

			// System.err.println("phen " + Arrays.toString(controlPoints) + " " + Arrays.toString(p1y));

			for (int s1 = 0; s1 < p1y.length; ++s1) {
				ret.set(i, s1, p1y[s1]);
			}
		}

		return ret;
	}

	/**
	 * Randomizes the dataset to find a distribution of
	 * fold changes between groups and then selecting
	 * only those indices that are above a minimum.
	 *
	 * @param mlog2 the mlog2
	 * @param g1 the g1
	 * @param g2 the g2
	 * @param n the n
	 * @param percentile the percentile
	 * @return the list
	 */
	/*
	private List<Integer> foldChangeIndices(AnnotationMatrix mlog2,
			MatrixGroup g1, 
			MatrixGroup g2,
			int n,
			int percentile) {
		// randomize 1000 times

		List<Integer> g11 = MatrixGroup.findColumnIndices(mlog2, g1);
		List<Integer> g21 = MatrixGroup.findColumnIndices(mlog2, g1);

		List<Integer> columns = new ArrayList<Integer>(g11.size() + g21.size());

		for (int c : g11) {
			columns.add(c);
		}

		for (int c : g21) {
			columns.add(c);
		}

		List<Double> foldChanges1 = new ArrayList<Double>(n);
		List<Double> foldChanges2 = new ArrayList<Double>(n);

		Random rand = new Random();

		for (int p = 0; p < n; ++p) {
			// create two groups 

			List<Integer> l1 = new ArrayList<Integer>(g11.size());

			for (int i = 0; i < g11.size(); ++i) {
				l1.add(columns.get(rand.nextInt(columns.size())));
			}

			List<Integer> l2 = new ArrayList<Integer>(g21.size());

			for (int i = 0; i < g21.size(); ++i) {
				l2.add(columns.get(rand.nextInt(columns.size())));
			}

			double maxD1 = -1;
			double maxD2 = -1;

			for (int r = 0; r < mlog2.getRowCount(); ++r) {
				int row = rand.nextInt(mlog2.getRowCount()); 

				double m1 = 0;

				for (int c : l1) {
					m1 += mlog2.getValue(row, c);
				}

				m1 /= (double)l1.size();

				double m2 = 0;

				for (int c : l2) {
					m2 += mlog2.getValue(row, c);
				}

				m2 /= (double)l2.size();

				double d = m1 - m2;

				if (d > maxD1) {
					maxD1 = d;
					//maxDAbs = dAbs;
				}

				d = m2 - m1;

				if (d > maxD2) {
					maxD2 = d;
					//maxDAbs = dAbs;
				}
			}

			foldChanges1.add(maxD1);
			foldChanges2.add(maxD2);

			//System.err.println("l1 " + l1.toString() + " " + maxD1);
			//System.err.println("l2 " + l2.toString() + " " + maxD2);
		}

		Collections.sort(foldChanges1);
		Collections.sort(foldChanges2);

		// 95th percentile

		int ns = n - 1;

		int rank = (int)(percentile / 100.0 * ns);

		double minFoldChange1 = foldChanges1.get(rank);
		double minFoldChange2 = foldChanges2.get(rank);

		List<Integer> indices = new ArrayList<Integer>();

		double maxD = -1;

		for (int row = 0; row < mlog2.getRowCount(); ++row) {
			double m1 = 0;

			for (int c : g11) {
				m1 += mlog2.getValue(row, c);
			}

			m1 /= g11.size();

			double m2 = 0;

			for (int c : g21) {
				m2 += mlog2.getValue(row, c);
			}

			m2 /= g21.size();

			double d = m1 - m2;

			boolean add1 = false;

			if (d >= minFoldChange1) {
				add1 = true;
			}

			d = m2 - m1;

			boolean add2 = false;

			if (d >= minFoldChange2) {
				add2 = true;
			}

			if (add1 || add2) {
				indices.add(row);
			}

			if (Math.abs(d) > maxD) {
				maxD = Math.abs(d);
			}
		}

		return indices;
	}
	 */


	/**
	 * Return the row indices of rows that have differential expression.
	 *
	 * @param m the m
	 * @param g1 the g1
	 * @param g2 the g2
	 * @param delta the delta
	 * @param support the support
	 * @return the list
	 */
	/*
	private static List<Integer> deltaSupportFilter(AnnotationMatrix m, 
			MatrixGroup g1,
			MatrixGroup g2,
			double delta, 
			int support) {
		List<Integer> g11 = MatrixGroup.findColumnIndices(m, g1);
		List<Integer> g21 = MatrixGroup.findColumnIndices(m, g2);


		List<Integer> indices = new ArrayList<Integer>();

		NormKernelDensity density = new NormKernelDensity();

		for (int i = 0; i < m.getRowCount(); ++i) {
			List<Double> p1 = 
					CollectionUtils.sort(MatrixOperations.rowToList(m, i, g11));

			List<Double> p2 = 
					CollectionUtils.sort(MatrixOperations.rowToList(m, i, g21));

			// p2 should begin at 0
			if (p2.get(0) != 0) {
				p2.add(0, 0.0);
			}

			List<Double> p1y;

			if (Mathematics.sum(p2) > 0) {
				// evaluate p1 normalizing to p2
				p1y = density.cdf(p1, p2);
			} else {
				p1y = Mathematics.ones(p1.size());
			}


			// see which match the delta and support

			Collections.sort(p1y);

			//System.err.println("p1 " + p1.toString());
			///System.err.println("p2 " + p2.toString());
			//System.err.println("p1y " + p1y.toString());

			boolean found = false;

			// Given a starting position see how many
			// values lie within delta of the position.
			// If we can find a grouping equal to the
			// the size of the support, then we can
			// say this is a differentially expressed
			// gene

			for (int s1 = 0; s1 < p1y.size(); ++s1) {
				int clusterSize = 0;

				for (int s2 = s1; s2 < p1y.size(); ++s2) {
					double d = p1y.get(s2) - p1y.get(s1);

					if (d < delta) {
						++clusterSize;
					}

					if (clusterSize >= support) {
						found = true;
						break;
					}
				}

				if (found) {
					break;
				}
			}

			if (found) {
				indices.add(i);
			}
		}

		return indices;
	}
	 */

	public static <X extends MatrixGroup> AnnotationMatrix groupZScoreMatrix(AnnotationMatrix m,
			XYSeriesGroup comparisonGroups,
			List<X> groups) {

		AnnotationMatrix ret = 
				AnnotatableMatrix.createNumericalMatrix(m);

		//AnnotationMatrix.copyColumnAnnotations(m, ret);
		//AnnotationMatrix.copyRowAnnotations(m, ret);



		// We normalize the comparison groups separately to the the others
		List<List<Integer>> comparisonIndices = 
				MatrixGroup.findColumnIndices(m, comparisonGroups);

		for (XYSeries g : comparisonGroups) {
			System.err.println("used " + g.getName());
		}

		// We ignore these indices when calculating the means for 
		//the other groups
		Set<Integer> used = 
				CollectionUtils.toSet(CollectionUtils.flatten(comparisonIndices));

		System.err.println("used " + used);


		List<List<Integer>> groupIndices = 
				MatrixGroup.findColumnIndices(m, groups);

		System.err.println("all " + CollectionUtils.flatten(groupIndices));



		// Now normalize the the other groups

		for (int r = 0; r < m.getRowCount(); ++r) {
			double mean = 0;
			double sd = 0;

			int groupCount = 0;

			// Only take the means and sd of the comparison groups

			for (List<Integer> indices : comparisonIndices) {
				List<Double> d1 = new ArrayList<Double>(indices.size());

				for (int c : indices) {
					// Do not count indices in the comparison groups
					// since the other groups must be normalized
					// independently
					//if (!used.contains(c)) {
					d1.add(m.getValue(r, c));
					//}
				}

				mean += Statistics.mean(d1);
				sd += Statistics.popStdDev(d1); // sampleStandardDeviation

				++groupCount;
			}

			//System.err.println("m " + mean + " " + sd + " " + groupCount);

			mean /= groupCount;
			sd /= groupCount;

			// Normalize the values
			for (List<Integer> indices : groupIndices) {
				for (int c : indices) {
					//if (!used.contains(c)) {
					ret.set(r, c, (m.getValue(r, c) - mean) / sd);
					//}
				}
			}
		}

		// Normalize the comparisons

		/*
		for (int i = 0; i < m.getRowCount(); ++i) {
			double mean = 0;
			double sd = 0;

			int groupCount = 0;

			for (List<Integer> indices : comparisonIndices) {
				if (indices.size() == 0) {
					continue;
				}

				List<Double> d1 = new ArrayList<Double>(indices.size());

				for (int c : indices) {
					d1.add(m.getValue(i, c));
				}

				mean += Statistics.mean(d1);
				sd += Statistics.popStdDev(d1); // sampleStandardDeviation

				++groupCount;
			}

			mean /= groupCount;
			sd /= groupCount;

			// Normalize the values
			for (List<Integer> indices : comparisonIndices) {
				if (indices.size() == 0) {
					continue;
				}

				for (int c : indices) {
					ret.setValue(i, c, (m.getValue(i, c) - mean) / sd);
				}
			}
		}
		 */

		return ret;
	}

	/**
	 * Create the largest patterns from the collection of patterns.
	 * 
	 * @param controlPatterns
	 * @param minGenes
	 * @return
	 */
	public static Map<Integer, Pattern> maximalPatterns(Map<Integer, Map<Comb, Collection<Integer>>> patternMap,
			int minGenes) {
		Map<Integer, Pattern> maxPatternMap = new HashMap<Integer, Pattern>();

		for (int cs : CollectionUtils.sortKeys(patternMap)) {
			for (Comb c : CollectionUtils.sortKeys(patternMap.get(cs))) {
				Collection<Integer> genes = patternMap.get(cs).get(c);

				int n = genes.size();

				System.err.println("c "  + cs  + " " + c + " " + n);

				if (n >= minGenes) {
					if (!maxPatternMap.containsKey(cs)) {
						maxPatternMap.put(cs, new Pattern(genes, c));
					} else {
						if (n > maxPatternMap.get(cs).size()) {
							maxPatternMap.put(cs, new Pattern(genes, c));
						}
					}
				}
			}
		}


		return maxPatternMap;
	}

	public static Map<Integer, Map<Comb, Set<Integer>>> filterPatterns(Map<Integer, Map<Comb, Set<Integer>>> patternMap,
			int minGenes) {
		Map<Integer, Map<Comb, Set<Integer>>> ret = 
				DefaultHashMap.create(new HashMapCreator<Comb, Set<Integer>>());

		for (int cs : CollectionUtils.sortKeys(patternMap)) {
			for (Comb c : CollectionUtils.sortKeys(patternMap.get(cs))) {
				Set<Integer> genes = patternMap.get(cs).get(c);

				int n = genes.size();

				if (n >= minGenes) {
					ret.get(cs).put(c, genes);
				}
			}
		}


		return ret;
	}

	public static List<Pattern> sortPatterns(Map<Integer, Map<Comb, Set<Integer>>> patternMap) {
		List<Pattern> patterns = new ArrayList<Pattern>();

		for (int cs : CollectionUtils.sortKeys(patternMap)) {
			Map<Integer, Map<Comb, Collection<Integer>>> sizeMap = 
					DefaultHashMap.create(new HashMapCreator<Comb, Collection<Integer>>());

			for (Comb c : patternMap.get(cs).keySet()) {
				Collection<Integer> genes = patternMap.get(cs).get(c);

				sizeMap.get(genes.size()).put(c, genes);
			}

			for (int gs : CollectionUtils.sortKeys(sizeMap)) {
				for (Comb c : CollectionUtils.sortKeys(sizeMap.get(gs))) {
					patterns.add(new Pattern(sizeMap.get(gs).get(c), c));
				}
			}
		}


		return patterns;
	}
}
