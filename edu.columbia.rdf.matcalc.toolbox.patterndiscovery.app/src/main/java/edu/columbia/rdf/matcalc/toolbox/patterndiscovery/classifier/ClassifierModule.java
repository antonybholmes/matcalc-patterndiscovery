/**
 * Copyright (C) 2016, Antony Holmes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of copyright holder nor the names of its contributors 
 *     may be used to endorse or promote products derived from this software 
 *     without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.classifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jebtk.core.ColorUtils;
import org.jebtk.core.Mathematics;
import org.jebtk.core.collections.ArrayListCreator;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.collections.DefaultTreeMap;
import org.jebtk.core.xml.XmlUtils;
import org.jebtk.graphplot.figure.Axes;
import org.jebtk.graphplot.figure.Axis;
import org.jebtk.graphplot.figure.BoxWhiskerScatterLayer2;
import org.jebtk.graphplot.figure.Figure;
import org.jebtk.graphplot.figure.LabelPlotLayer;
import org.jebtk.graphplot.figure.Plot;
import org.jebtk.graphplot.figure.SubFigure;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.graphplot.icons.ShapeStyle;
import org.jebtk.math.Linspace;
import org.jebtk.math.cluster.DistanceMetric;
import org.jebtk.math.matrix.AnnotatableMatrix;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.math.matrix.Matrix;
import org.jebtk.math.matrix.MatrixGroup;
import org.jebtk.math.statistics.Statistics;
import org.jebtk.modern.UIService;
import org.jebtk.modern.dialog.ModernMessageDialog;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.graphics.icons.PlusVectorIcon;
import org.jebtk.modern.graphics.icons.RunVectorIcon;
import org.jebtk.modern.io.FileDialog;
import org.jebtk.modern.io.RecentFilesService;
import org.jebtk.modern.ribbon.RibbonLargeButton;
import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.figure.graph2d.Graph2dWindow;
import edu.columbia.rdf.matcalc.toolbox.CalcModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


/**
 * Merges designated segments together using the merge column. Consecutive rows with the same
 * merge id will be merged together. Coordinates and copy number will be adjusted but
 * genes, cytobands etc are not.
 *
 * @author Antony Holmes Holmes
 *
 */
public class ClassifierModule extends CalcModule {

	/**
	 * The member convert button.
	 */
	private RibbonLargeButton mClassifyButton = new RibbonLargeButton("Classify", 
			UIService.getInstance().loadIcon(RunVectorIcon.class, 24));

	private RibbonLargeButton mAddButton = new RibbonLargeButton("Add Classifier", 
			UIService.getInstance().loadIcon(PlusVectorIcon.class, 24));

	private final static Logger LOG = 
			LoggerFactory.getLogger(ClassifierModule.class);


	/**
	 * Add a small amount to the classifier score to ensure it is never 0.
	 * The net result is that the maximum classifier score is 1/0.001 = 1000.
	 */
	private static final double DISTANCE_SHIFT = 0.001;

	private static final int CLASSIFIER_PLOT_WIDTH = 100;

	//private RibbonLargeButton2 mExportButton = new RibbonLargeButton2("Export", 
	//		UIResources.getInstance().loadScalableIcon(SaveVectorIcon.class, 24));

	/**
	 * The member window.
	 */
	private MainMatCalcWindow mWindow;


	/* (non-Javadoc)
	 * @see org.abh.lib.NameProperty#getName()
	 */
	@Override
	public String getName() {
		return "Classifier";
	}

	/* (non-Javadoc)
	 * @see edu.columbia.rdf.apps.matcalc.modules.Module#init(edu.columbia.rdf.apps.matcalc.MainMatCalcWindow)
	 */
	@Override
	public void init(MainMatCalcWindow window) {
		mWindow = window;

		// home
		mWindow.getRibbon().getToolbar("Statistics").getSection("Classifier").add(mClassifyButton);
		mWindow.getRibbon().getToolbar("Statistics").getSection("Classifier").add(mAddButton);
		//mWindow.getRibbon().getToolbar("Statistics").getSection("Classifier").add(mExportButton);

		mClassifyButton.addClickListener(new ModernClickListener() {
			@Override
			public void clicked(ModernClickEvent e) {
				classify();
			}}
				);

		mAddButton.addClickListener(new ModernClickListener() {
			@Override
			public void clicked(ModernClickEvent e) {
				addClassifier();
			}}
				);

		/*
		mExportButton.addClickListener(new ModernClickListener() {
			@Override
			public void clicked(ModernClickEvent e) {
				try {
					export();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}}
				);
		 */
	}

	private void classify() {
		ClassifierDialog dialog = new ClassifierDialog(mWindow);

		dialog.setVisible(true);

		if (dialog.isCancelled()) {
			return;
		}

		AnnotationMatrix m = mWindow.getCurrentMatrix();

		List<Classifier> classifiers = dialog.getClassifiers();

		int permutations = dialog.getPermutations();

		DistanceMetric distance = dialog.getDistanceMetric();

		// Map row names to indices
		Map<String, Integer> geneRowIndexMap = 
				CollectionUtils.toIndexMap(m.getRowNames());

		AnnotationMatrix resultsM = 
				AnnotatableMatrix.createNumericalMatrix(m.getColumnCount(), classifiers.size());


		Map<Classifier, List<Double>> scoreMap = 
				DefaultTreeMap.create(new ArrayListCreator<Double>());

		List<AnnotationMatrix> summaries =
				new ArrayList<AnnotationMatrix>();

		for (int ci = 0; ci < classifiers.size(); ++ci) {
			Classifier classifier = classifiers.get(ci);

			for (int queryColumn = 0; queryColumn < m.getColumnCount(); ++queryColumn) {
				// Score each sample in the query set

				double score = score(classifier,
						m,
						queryColumn,
						geneRowIndexMap,
						distance);

				//System.err.println("score " + queryColumn + " " + name + " " + score);


				scoreMap.get(classifier).add(score);

				resultsM.set(queryColumn, ci, score);
			}

			// Now we use the training set to find the 5% and 95% significance
			// levels by bootstrapping and randomly selecting samples

			LOG.info("bootstrapping {} permutations...", permutations);

			List<Double> permScores = Mathematics.array(permutations); //new ArrayList<Double>(1000);

			int trainingSetSize = classifier.getPhenotype().getColumnCount() +
					classifier.getControl().getColumnCount();

			List<Integer> permColIndices = 
					Mathematics.randIntSeqWithRep(trainingSetSize, permutations);

			Map<String, Integer> classRowIndexMap = 
					CollectionUtils.toIndexMap(classifier.getGenes());

			for (int column : permColIndices) {
				double[] qExp;

				double score;

				if (column < classifier.getPhenotype().getColumnCount()) {
					qExp = classifier.getPhenotype().columnAsDouble(column);
				} else {
					// The column must have the number of phenotype columns
					// subtracted so that i is relative to the size of the
					// control group
					qExp = classifier.getControl().columnAsDouble(column - classifier.getPhenotype().getColumnCount());

				}

				qExp = Mathematics.randSubsetWithReplacement(qExp, qExp.length);

				score = score(classifier,
						qExp,
						classRowIndexMap,
						distance);

				permScores.add(score);
			}

			//System.err.println("p score " + CollectionUtils.sort(permScores));

			double t5 = Statistics.quantile(permScores, 5);
			double t95 = Statistics.quantile(permScores, 95);

			System.err.println(scoreMap.get(classifier));

			AnnotationMatrix summaryM = 
					AnnotatableMatrix.createAnnotatableMatrix(scoreMap.get(classifier).size(), 7);

			summaryM.setColumnName(0, "Sample");
			summaryM.setColumnName(1, "Classifier");
			summaryM.setColumnName(2, "Classifier Score");
			summaryM.setColumnName(3, "Class");
			summaryM.setColumnName(4, "t_5");
			summaryM.setColumnName(5, "t_95");
			summaryM.setColumnName(6, "Total Genes");

			int r = 0;

			for (int queryColumn = 0; queryColumn < m.getColumnCount(); ++queryColumn) {
				// Score each sample in the query set

				String name = m.getColumnName(queryColumn);

				double score = ((List<Double>)scoreMap.get(classifier)).get(queryColumn);

				String cl;

				if (score >= t95) {
					cl = classifier.getPhenotypeName();
				} else if (score <= t5) {
					cl = classifier.getControlName();
				} else {
					cl = "undecided";
				}


				summaryM.set(r, 0, name);
				summaryM.set(r, 1, classifier.getName());
				summaryM.set(r, 2, score);
				summaryM.set(r, 3, cl);
				summaryM.set(r, 4, t5);
				summaryM.set(r, 5, t95);
				summaryM.set(r, 6, classifier.getGeneCount());

				++r;
			}

			mWindow.addToHistory("Run classifier", summaryM);

			summaries.add(summaryM);
		}

		plot(m, mWindow.getGroups(), classifiers, resultsM, summaries);
	}

	private void plot(AnnotationMatrix m,
			XYSeriesGroup groups,
			List<Classifier> classifiers,
			AnnotationMatrix resultsM,
			List<AnnotationMatrix> summaries) {

		// We need to create some series for each classifier

		Figure figure = Figure.createFigure();

		// SubFigure to hold new plot
		SubFigure subFigure = figure.newSubFigure();

		// We will use one set of axes for the whole plot
		Axes axes = subFigure.newAxes();

		// We add multiple plots to the figure, one for each classifier

		double max = 0;

		for (int ci = 0; ci < classifiers.size(); ++ci) {
			//Classifier classifier = classifiers.get(ci);

			AnnotationMatrix summaryM = summaries.get(ci);

			//
			// Summary
			//

			Plot plot = axes.newPlot();

			double confMin = summaryM.getValue(0, 4);
			double confMax = summaryM.getValue(0, 5);

			System.err.println("Conf " + confMin + " " + confMax);

			plot.addChild(new ConfidenceLayer(ci + 0.5, confMin, confMax));



			plot = axes.newPlot();
			plot.addChild(new BoxWhiskerScatterLayer2(ci + 0.5, 0.8));

			// Plot for each group

			for (XYSeries g : groups) {
				XYSeries series = new XYSeries(g.getName(), g.getColor());

				series.setMarker(ShapeStyle.CIRCLE);
				series.getMarkerStyle().getLineStyle().setColor(g.getColor());
				series.getMarkerStyle().getFillStyle().setColor(ColorUtils.getTransparentColor50(g.getColor()));
				//series.getMarker().setSize(size);

				List<Integer> indices = MatrixGroup.findColumnIndices(m, g);

				AnnotationMatrix cm = 
						AnnotatableMatrix.createNumericalMatrix(indices.size(), 1);

				for (int i = 0; i < indices.size(); ++i) {
					double v = resultsM.getValue(indices.get(i), ci);

					cm.set(i, 0, v);

					max = Math.max(max, Math.abs(v));
				}

				series.setMatrix(cm);

				plot.getAllSeries().add(series);	
			}
		}



		//
		// The axis
		//
		
		Axis axis = axes.getX1Axis();

		axis.setLimits(0, classifiers.size(), 1);
		axis.getTicks().setTicks(Linspace.evenlySpaced(0.5, classifiers.size() - 0.5, 1));
		axis.getTicks().getMajorTicks().setRotation(-Mathematics.HALF_PI);

		// Don't render the axis, instead use labels to indicate the
		// phenotype and control
		axis.setVisible(false);
		axis.getTicks().getMajorTicks().getLineStyle().setVisible(false);
		axis.getTicks().getMinorTicks().getLineStyle().setVisible(false);
		axis.getLineStyle().setVisible(false);
		axis.getTitle().setVisible(false);

		// The labels are the series names

		List<String> labels = new ArrayList<String>(classifiers.size());

		for (Classifier c : classifiers) {
			labels.add(c.getName());
		}

		axis.getTicks().getMajorTicks().setLabels(labels);

		//
		// The y axis
		//
		
		axis = axes.getY1Axis();
		axis.setLimitsAutoRound(-max, max);
		axis.setShowZerothLine(true);
		axis.getTitle().setText("Classifier Score");

		// Add some classifier labels to indicate what the score means

		// Get the graph max after adjustments
		max = axis.getMax();
		
		for (int ci = 0; ci < classifiers.size(); ++ci) {
			Classifier classifier = classifiers.get(ci);

			Plot plot = axes.newPlot();

			plot.addChild(new LabelPlotLayer(classifier.getPhenotypeName(), ci + 0.5, max, true, true, 0, -40));
			plot.addChild(new LabelPlotLayer(classifier.getControlName(), ci + 0.5, -max, true, true, 0, 40));
		}


		axes.setMargins(100);
		//axes.setBottomMargin(PlotFactory.autoSetX1LabelMargin(axes));

		axes.setInternalSize(classifiers.size() * CLASSIFIER_PLOT_WIDTH, 600);

		subFigure.setMargins(100);

		Graph2dWindow window = new Graph2dWindow(mWindow, figure, false);

		window.setVisible(true);
	}

	private static double score(Classifier classifier,
			Matrix m,
			int queryColumn,
			Map<String, Integer> geneRowIndexMap,
			DistanceMetric distance) {
		return score(classifier,
				m.columnAsDouble(queryColumn),
				geneRowIndexMap,
				distance);
	}

	private static double score(Classifier classifier,
			double[] qExp,
			Map<String, Integer> geneRowIndexMap,
			DistanceMetric distance) {

		double phenScore = d(classifier,
				true,
				qExp,
				geneRowIndexMap,
				distance);

		double controlScore = d(classifier,
				false,
				qExp,
				geneRowIndexMap,
				distance);

		//System.err.println("ph score " + phenScore + " " + controlScore);

		double score = (1.0 / phenScore) - (1.0 / controlScore);

		return score;
	}

	/**
	 * Find the min distance from each of the classifiers subjects to the 
	 * given query sample.
	 * 
	 * @param classifier
	 * @param classifierMatrix
	 * @param queryM
	 * @param queryColumn
	 * @param geneRowIndexMap
	 * @return
	 */
	private static double d(Classifier classifier,
			boolean isPhenotype,
			AnnotationMatrix queryM,
			int queryColumn,
			Map<String, Integer> geneRowIndexMap,
			DistanceMetric distance) {
		return d(classifier,
				isPhenotype,
				queryM.columnAsDouble(queryColumn),
				geneRowIndexMap,
				distance);
	}

	private static double d(Classifier classifier,
			boolean isPhenotype,
			double[] qExp,
			Map<String, Integer> geneRowIndexMap,
			DistanceMetric distance) {

		double minD = Double.MAX_VALUE;

		AnnotationMatrix classifierM;

		if (isPhenotype) {
			classifierM = classifier.getPhenotype();
		} else {
			classifierM = classifier.getControl();
		}

		for (int classiferSample = 0; classiferSample < classifierM.getColumnCount(); ++classiferSample) {
			/*
			double d = distance(classifier,
					isPhenotype,
					classiferSample,
					qExp,
					geneRowIndexMap);
			 */

			double d = distance(classifier,
					isPhenotype,
					classiferSample,
					qExp,
					geneRowIndexMap,
					distance);

			minD = Math.min(d, minD);
		}

		return minD;
	}

	private static double distance(Classifier classifier,
			boolean isPhenotype,
			int classifierSample,
			AnnotationMatrix queryM,
			int queryColumn,
			Map<String, Integer> geneRowIndexMap) {
		return distance(classifier,
				isPhenotype,
				classifierSample,
				queryM.columnAsDouble(queryColumn),
				geneRowIndexMap);
	}

	private static double distance(Classifier classifier,
			boolean isPhenotype,
			int classifierSample,
			double[] qExp,
			Map<String, Integer> geneRowIndexMap) {

		// Distance is a small amount so that the distance can never
		// be zero which would yield a infinity exception when the score
		// is calculated
		double distance = DISTANCE_SHIFT;

		AnnotationMatrix classifierM;

		if (isPhenotype) {
			classifierM = classifier.getPhenotype();
		} else {
			classifierM = classifier.getControl();
		}

		// Keep track of how many genes we find
		int gc = 0;

		for (int g = 0; g < classifier.getGeneCount(); ++g) {
			String gene = classifier.getGene(g);

			if (geneRowIndexMap.containsKey(gene)) {
				// As per Klein et al. Gene Expression Profiling of B
				// Cell Chronic Lymphocytic Leukemia Reveal a Homogeneous
				// Phenotype Related to Memory B Cells eq. 3

				double v = classifierM.getValue(g, classifierSample);

				double q = qExp[geneRowIndexMap.get(gene)];

				double d = v - q;

				distance += d * d;

				++gc;
			}
		}

		distance = Math.sqrt(distance);

		// Divide by 1/nc
		distance /= gc;

		return distance;
	}

	private static double distance(Classifier classifier,
			boolean isPhenotype,
			int classifierSample,
			double[] qExp,
			Map<String, Integer> geneRowIndexMap,
			DistanceMetric distance) {
		AnnotationMatrix classifierM;

		if (isPhenotype) {
			classifierM = classifier.getPhenotype();
		} else {
			classifierM = classifier.getControl();
		}

		// Extract the values from the classifier matrix

		List<Double> v1 = new ArrayList<Double>(classifier.getGeneCount());
		List<Double> v2 = new ArrayList<Double>(classifier.getGeneCount());

		for (int g = 0; g < classifier.getGeneCount(); ++g) {
			String gene = classifier.getGene(g);

			if (geneRowIndexMap.containsKey(gene)) {
				double v = classifierM.getValue(g, classifierSample);
				double q = qExp[geneRowIndexMap.get(gene)];

				v1.add(v);
				v2.add(q);
			}
		}

		double d = distance.distance(v1, v2) + DISTANCE_SHIFT;


		//System.err.println("pd1 " + v1.size() + " " + v2.size() + " " + d);

		return d;
	}

	private void addClassifier() {
		if (mWindow.getGroups().size() == 0) {
			ModernMessageDialog.createWarningDialog(mWindow, 
					"You must create some groups.");

			return;
		}

		AnnotationMatrix m = mWindow.getCurrentMatrix();

		AddClassifierDialog dialog = new AddClassifierDialog(mWindow, m);

		dialog.setVisible(true);

		if (dialog.isCancelled()) {
			return;
		}

		String name = dialog.getClassifierName();
		XYSeries g1 = dialog.getGroup1(); // new Group("g1");
		XYSeries g2 = dialog.getGroup2();


		Classifier classifier = 
				Classifier.create(name, m, g1, g2, dialog.getAnnotation());

		ClassifierService.getInstance().add(classifier);

		ModernMessageDialog.createInformationDialog(mWindow, 
				"The classifier was created.");
	}

	private void export() throws TransformerException, ParserConfigurationException {
		writeXml(FileDialog
				.save(mWindow)
				.filter(new ClassifierGuiFileFilter())
				.getFile(RecentFilesService.getInstance().getPwd()));
	}

	public final void writeXml(java.nio.file.Path file) throws TransformerException, ParserConfigurationException {
		Document doc = XmlUtils.createDoc();

		doc.appendChild(ClassifierService.getInstance().toXml(doc));

		XmlUtils.writeXml(doc, file);

		//LOG.info("Wrote settings to {}", Path.getAbsoluteFile());
	}
}
