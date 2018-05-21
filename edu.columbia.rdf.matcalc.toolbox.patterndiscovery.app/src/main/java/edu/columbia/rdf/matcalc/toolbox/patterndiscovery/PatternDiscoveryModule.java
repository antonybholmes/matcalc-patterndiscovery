package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jebtk.core.Indexed;
import org.jebtk.core.IndexedInt;
import org.jebtk.core.Mathematics;
import org.jebtk.core.Properties;
import org.jebtk.core.collections.ArrayListCreator;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.collections.DefaultTreeMap;
import org.jebtk.core.collections.DefaultTreeMapCreator;
import org.jebtk.core.collections.IterMap;
import org.jebtk.core.collections.TreeMapCreator;
import org.jebtk.core.collections.TreeSetCreator;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.graphplot.figure.series.XYSeriesModel;
import org.jebtk.math.MathUtils;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.math.matrix.DoubleMatrix;
import org.jebtk.math.matrix.MatrixGroup;
import org.jebtk.math.matrix.utils.MatrixOperations;
import org.jebtk.math.statistics.KernelDensity;
import org.jebtk.math.statistics.NormKernelDensity;
import org.jebtk.math.statistics.Statistics;
import org.jebtk.modern.AssetService;
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

public class PatternDiscoveryModule extends CalcModule
    implements ModernClickListener {

  // private static final int DEFAULT_POINTS =
  // SettingsService.getInstance().getInt("pattern-discovery.cdf.points");

  // private static final List<Double> EVAL_POINTS =
  // Linspace.generate(0, 1, DEFAULT_POINTS);

  private MainMatCalcWindow mWindow;

  private static final Logger LOG = LoggerFactory
      .getLogger(PatternDiscoveryModule.class);

  @Override
  public String getName() {
    return "Pattern Discovery";
  }

  @Override
  public void init(MainMatCalcWindow window) {
    mWindow = window;

    RibbonLargeButton button = new RibbonLargeButton("Pattern Discovery",
        AssetService.getInstance().loadIcon(PatternDiscoveryIcon.class, 24),
        "Pattern Discovery", "Supervised differentially expressed genes.");
    button.addClickListener(this);
    mWindow.getRibbon().getToolbar("Classification").getSection("Classifier")
        .add(button);
  }

  @Override
  public void clicked(ModernClickEvent e) {
    try {
      patternDiscovery();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private void patternDiscovery() throws IOException {
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

    DataFrame m = mWindow.getCurrentMatrix();

    if (m == null) {
      return;
    }

    PatternDiscoveryDialog dialog = new PatternDiscoveryDialog(mWindow, m,
        mWindow.getGroups());

    dialog.setVisible(true);

    if (dialog.getStatus() == ModernDialogStatus.CANCEL) {
      return;
    }

    // We are only interested in the opened matrix
    // without transformations.

    // resetHistory();

    if (dialog.getReset()) {
      mWindow.resetHistory();
    }

    XYSeries g1 = dialog.getPhenGroup();
    XYSeries g2 = dialog.getControlGroup();

    // double snr = dialog.getMinSNR();
    double delta = dialog.getDelta();
    int support1 = dialog.getPhenSupport();
    boolean support1Only = dialog.getPhenSupportMinOnly();
    int support2 = dialog.getControlSupport();
    boolean support2Only = dialog.getControlSupportMinOnly();
    int minGenes = dialog.getGenes();
    double minZ = dialog.getMinZScore();

    // int percentile = dialog.getPercentile();
    boolean plot = dialog.getPlot();
    boolean logMode = dialog.getLogMode();
    boolean isLogData = dialog.getIsLogData();
    // boolean bidirectional = dialog.getBidirectional();

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
        logMode,
        isLogData,
        plot,
        properties);
  }

  /**
   * Discover differential expression patterns.
   * 
   * @param m The matrix to analyze.
   * @param delta The max separation between experiments.
   * @param phenGroup The columns in the phenotype group.
   * @param controlGroup The columns in the control group.
   * @param groups All the groups in the matrix.
   * @param rowGroups Any row groups.
   * @param phenSupport Minimum support in the phenotype group.
   * @param phenSupportOnly Comb must contain exactly minimum support.
   * @param controlSupport Minimum support in the control group.
   * @param controlSupportOnly Comb must contain exactly minimum support.
   * @param minGenes Minimum number of genes in pattern.
   * @param minZ Minimum z-score.
   * @param logMode Log data.
   * @param isLogData Is data log transformed.
   * @param properties Heat map properties.
   * @throws IOException
   */
  public void patternDiscovery(DataFrame m,
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
      boolean logMode,
      boolean isLogData,
      boolean plot,
      Properties properties) throws IOException {

    XYSeriesGroup comparisonGroups = new XYSeriesGroup();
    comparisonGroups.add(phenGroup);
    comparisonGroups.add(controlGroup);

    DataFrame logM;

    if (logMode) {
      logM = mWindow.addToHistory("Log2",
          MatrixOperations.log2(MatrixOperations.add(m, 1)));
    } else {
      logM = m;
    }

    // Order table by groups

    // List<Integer> indices =
    // MatrixGroup.findAllColumnIndices(logM, comparisonGroups);

    // Rule of thumb, lets look at genes where at least half the
    // samples are non zero
    // DataFrame filterM = m; //mWindow.addToHistory("Min Exp Filter",
    // MatrixOperations.minExpFilter(m, 0.01, indices.size() / 2));

    //
    // Filter z scores to ensure min z scores
    //

    List<Double> zscores = DoubleMatrix
        .diffGroupZScores(logM, phenGroup, controlGroup);

    DataFrame zscoresM = new DataFrame(logM);
    zscoresM.setNumRowAnnotations("Z-score", zscores);

    mWindow.addToHistory("Z-score", zscoresM);

    List<Indexed<Integer, Double>> zscoresIndexed = CollectionUtils
        .index(zscores);

    List<Integer> indices = new ArrayList<Integer>();

    for (Indexed<Integer, Double> index : zscoresIndexed) {
      if (Math.abs(index.getValue()) > minZ) {
        indices.add(index.getIndex());
      }
    }

    // Filter the zscores and re-index
    zscores = CollectionUtils.subList(zscores, indices);
    zscoresIndexed = CollectionUtils.index(zscores);

    DataFrame zScoreFilteredM = mWindow.addToHistory("Filter z-score",
        DataFrame.copyRows(zscoresM, indices));

    //
    // Fold Changes
    //

    List<Double> foldChanges;

    if (isLogData || logMode) {
      foldChanges = DoubleMatrix
          .logFoldChange(zScoreFilteredM, phenGroup, controlGroup);
    } else {
      foldChanges = DoubleMatrix
          .foldChange(zScoreFilteredM, phenGroup, controlGroup);
    }

    // filter by fold changes
    // filter by fdr

    String name = isLogData || logMode ? "Log fold change" : "Fold change";

    DataFrame foldChangesM = new DataFrame(zScoreFilteredM);
    foldChangesM.setNumRowAnnotations(name, foldChanges);

    mWindow.addToHistory(name, foldChangesM);

    //
    // Order by pos and negative z score
    //

    List<Indexed<Integer, Double>> posZScores = CollectionUtils
        .reverseSort(CollectionUtils.subList(zscoresIndexed,
            MathUtils.gt(zscoresIndexed, 0)));

    DataFrame posZM = DataFrame.copyRows(foldChangesM,
        IndexedInt.indices(posZScores));

    List<Indexed<Integer, Double>> negZScores = CollectionUtils
        .sort(CollectionUtils.subList(zscoresIndexed,
            MathUtils.lt(zscoresIndexed, 0)));

    DataFrame negZM = DataFrame.copyRows(foldChangesM,
        IndexedInt.indices(negZScores));

    // Now make a list of the new zscores in the correct order,
    // positive decreasing, negative, decreasing
    List<Indexed<Integer, Double>> sortedZscores = CollectionUtils
        .cat(posZScores, negZScores);

    // Put the zscores in order
    indices = IndexedInt.indices(sortedZscores);

    zscores = CollectionUtils.subList(zscores, indices);

    System.err.println("zscore " + zscores + " " + indices);

    DataFrame zScoreSortedM = DataFrame.copyRows(foldChangesM, indices); // mWindow.addToHistory("Sort
                                                                         // by
                                                                         // z-score",
                                                                         // AnnotatableMatrix.copyRows(foldChangesM,
                                                                         // indices));

    //
    // Normalize control and phenotype by max in control group
    //

    // DataFrame phenStandardNormM = zScoreSortedM;
    // //mWindow.addToHistory("Normalize To Control", normalize(orderM,
    // controlGroup));

    // mWindow.addToHistory("Build phenotype curves",
    // normPhenToControl(zScoreSortedM, phenGroup, controlGroup));

    DataFrame phenNormM;
    DataFrame controlNormM;

    // phenNormM = normPhenToControl(zScoreSortedM, phenGroup, controlGroup);
    phenNormM = normPhenToControl(posZM, phenGroup, controlGroup);

    // Where the phenotype stands out from the control

    Map<Integer, IterMap<Comb, Set<Integer>>> phenPatterns1 = DefaultTreeMap
        .create(new DefaultTreeMapCreator<Comb, Set<Integer>>(
            new TreeSetCreator<Integer>()));

    patterns(phenNormM,
        phenSupport,
        delta,
        phenSupportOnly,
        minGenes,
        0,
        phenPatterns1);

    System.err.println("phen " + phenPatterns1.size());

    controlNormM = normPhenToControl(posZM, controlGroup, phenGroup);

    Map<Integer, IterMap<Comb, Set<Integer>>> phenPatterns2 = DefaultTreeMap
        .create(new DefaultTreeMapCreator<Comb, Set<Integer>>(
            new TreeSetCreator<Integer>()));

    patterns(controlNormM,
        controlSupport,
        delta,
        controlSupportOnly,
        minGenes,
        0,
        phenPatterns2);

    // mWindow.addToHistory("Build control curves",
    // normPhenToControl(zScoreSortedM,
    // controlGroup, phenGroup));
    // controlNormM = normPhenToControl(zScoreSortedM, controlGroup, phenGroup);
    controlNormM = normPhenToControl(negZM, controlGroup, phenGroup);

    // Where the control stands out from the phenotype. Should be similar
    // to phen patterns but cannot be guaranteed.
    // Since we are using the lower half of the matrix, the gene indices
    // need to be offset by the number of rows in the positive matrix so
    // that the indices remain consistent with the whole matrix.

    int offset = posZM.getRows();

    Map<Integer, IterMap<Comb, Set<Integer>>> controlPatterns1 = DefaultTreeMap
        .create(new DefaultTreeMapCreator<Comb, Set<Integer>>(
            new TreeSetCreator<Integer>()));

    patterns(controlNormM,
        controlSupport,
        delta,
        controlSupportOnly,
        minGenes,
        offset,
        controlPatterns1);

    phenNormM = normPhenToControl(negZM, phenGroup, controlGroup);

    Map<Integer, IterMap<Comb, Set<Integer>>> controlPatterns2 = DefaultTreeMap
        .create(new DefaultTreeMapCreator<Comb, Set<Integer>>(
            new TreeSetCreator<Integer>()));

    patterns(phenNormM,
        phenSupport,
        delta,
        phenSupportOnly,
        minGenes,
        offset,
        controlPatterns2);

    PatternsPanel patternsPanel = new PatternsPanel(mWindow, zScoreSortedM,
        phenGroup, controlGroup, sortPatterns(phenPatterns1),
        sortPatterns(phenPatterns2), sortPatterns(controlPatterns1),
        sortPatterns(controlPatterns2), groups, comparisonGroups, plot,
        properties);

    mWindow.addToHistory(
        new PatternsPanelTransform(mWindow, patternsPanel, zScoreSortedM));

    patternsPanel.filter();
  }

  public void setLeftPane(List<Pattern> phenPatternMap,
      List<Pattern> controlPatternMap) {
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
  public static void patterns(final DataFrame phenM,
      int phenSupport,
      double delta,
      boolean minSupportOnly,
      int minGenes,
      int offset,
      Map<Integer, IterMap<Comb, Set<Integer>>> maximalPatterns) {
    int ng = phenM.getRows();
    int ne = phenM.getCols();

    // Normalize control

    Map<Integer, List<Comb>> elPatternMap = DefaultTreeMap
        .create(new ArrayListCreator<Comb>());

    elementaryPatterns(phenM,
        ng,
        ne,
        delta,
        phenSupport,
        minSupportOnly,
        elPatternMap);

    for (int i : elPatternMap.keySet()) {
      for (Comb c : elPatternMap.get(i)) {
        if (c.toString().startsWith("1, 2")) {
          System.err.println("ep " + i + " " + c);
        }
      }
    }

    LOG.info("Creating Patterns...");

    // Make a list of all genes with patterns

    growPatterns(ng,
        ne,
        elPatternMap,
        phenSupport,
        minSupportOnly,
        minGenes,
        offset,
        maximalPatterns);

    // For each gene, we know which patterns/experiments it supports
    // return maximalPatterns;
  }

  /**
   * Returns patterns mapped to the support size. If minSupportOnly is true,
   * then each pattern will be the largest pattern with a support exactly equal
   * to minSupport, otherwise it will be the largest pattern with a support
   * greater than or equal to minSupport.
   * 
   * @param ng The number of genes
   * @param ne The number of experiments
   * @param elementaryPatterns
   * @param minSupport
   * @param minGenes
   * @param usedCombs
   * @return
   */
  private static void growPatterns(int ng,
      int ne,
      Map<Integer, List<Comb>> elementaryPatterns,
      int minSupport,
      boolean minSupportOnly,
      int minGenes,
      int offset,
      Map<Integer, IterMap<Comb, Set<Integer>>> patternMap) {

    // The method as written in the paper seems extremely inefficient
    // since it needlessly generates all possible tests at once n(n-1)/2.
    // Instead we take a pattern and grow it until it is maximal, then
    // add it to the solutions and move to the next.

    // Store the maximal pattern for combs of a given size

    for (int support = minSupport; support <= ne; ++support) {
      System.err.println("Support " + support);

      // Map<Integer, Map<Comb, Set<Integer>>> patternMap =
      // DefaultTreeMap.create(new DefaultTreeMapCreator<Comb, Set<Integer>>(new
      // TreeSetCreator<Integer>()));

      for (int sg = 0; sg < ng; ++sg) {
        Collection<Comb> sources = elementaryPatterns.get(sg);

        if (sources.size() == 0) {
          continue;
        }

        // Iterate over all the combs for this starting gene and try to
        // build the largest possible pattern we can
        for (Comb source : sources) {

          int sn = source.size();

          // System.err.println("hmm " + source.toString() + " " + source.size()
          // + " " +
          // sn + " " + sg);

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

          // genes.add(sg);

          // Seed the pattern with the comb of the source gene
          patternMap.get(source.size()).get(source).add(sg + offset);

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

              Comb intersectionComb = CombService.getInstance()
                  .intersect(source, target);

              int n = intersectionComb.size();

              if (n >= support) {
                System.err.println("intersect " + source.toString() + ":"
                    + target.toString() + " " + n + " " + tg);

                patternMap.get(n).get(intersectionComb).add(tg + offset);
              }
            }
          }
        }

        if (sg % 1000 == 0) {
          LOG.info("Processed {} genes...", sg);
        }
      }

      // Union of all patterns found so far
      // for (int cs : patternMap.keySet()) {
      // for (Comb c : CollectionUtils.sortKeys(patternMap.get(cs))) {
      // allPatternsMap.get(cs).put(c, patternMap.get(cs).get(c));
      // }
      // }

      if (minSupportOnly) {
        break;
      }
    }

    System.err.println("sizes " + patternMap.keySet());

    // return maxPatternMap;
    filterPatterns(minGenes, patternMap);
  }

  /**
   * We test whether a comb is a super pattern of any of the existing combs we
   * have created. Since the genes are processed in the same order, if a comb is
   * a super comb (equal to or bigger than an existing comb) of another, then it
   * must have been added to the pattern of a previous gene. If this is the
   * case, this new pattern cannot be bigger than an existing one since the
   * existing pattern must contain at least one more gene. We can therefore
   * abandon creating a pattern using this comb as a starting point.
   * 
   * @param source
   * @param patternMap
   * @return
   */
  private static boolean isSuperComb(Comb source,
      final Map<Integer, IterMap<Comb, Set<Integer>>> patternMap) {

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
   * Generate all elementary patterns, that is the patterns for each gene with
   * minimum support based on the delta. Genes without a requisite pattern, will
   * not have an entry in the map. This saves checking elementary patterns that
   * cannot yield a usable solution.
   * 
   * @param m
   * @param delta
   * @param minSupport
   * @return
   */
  private static void elementaryPatterns(DataFrame m,
      int ng,
      int ne,
      double delta,
      int minSupport,
      boolean minSupportOnly,
      Map<Integer, List<Comb>> combMap) {
    for (int sg = 0; sg < ng; ++sg) {
      // Sort values in order and keep track of which sample (column)
      // they are
      List<Indexed<Integer, Double>> p1 = CollectionUtils
          .sort(CollectionUtils.index(m.rowToDoubleArray(sg)));

      // Try to find all possible supports of a given size for the
      // gene
      for (int support = minSupport; support <= ne; ++support) {
        for (int e1 = 0; e1 < ne; ++e1) {
          double p1v = p1.get(e1).getValue();

          // We only want curves on the upper half of the curve
          // so that they are positively differentially expressed.
          // if (p1v < 0.5) {
          // continue;
          // }

          List<Double> values = new ArrayList<Double>(ne);

          values.add(p1v);

          for (int e2 = e1 + 1; e2 < ne; ++e2) {
            double p2v = p1.get(e2).getValue();

            double d = p2v - p1v;

            if (d > delta) {
              break;
            } else {
              values.add(p2v);
            }
          }

          int clusterSize = values.size();

          // This run of samples are sufficiently close to each other
          // that they form a group with support = minSupport
          if (clusterSize == support) {
            // if (clusterSize > 1) {
            // A cluster must have at least 2 samples

            List<Integer> indices = new ArrayList<Integer>();

            for (int i = e1; i < e1 + clusterSize; ++i) {
              indices.add(p1.get(i).getIndex());
            }

            // double mean = new Stats(values).mean();

            Comb comb = CombService.getInstance().createComb(indices);

            combMap.get(sg).add(comb);

            if (comb.toString().startsWith("1, 2")) {
              System.err.println("begin " + sg + " " + comb + " " + clusterSize
                  + " " + indices);
            }
          }
        }

        if (minSupportOnly) {
          break;
        }
      }
    }

    if (minSupportOnly) {
      // prune genes with combs greater than minsupport

      System.err.println("Min Support Prune");

      for (int sg : combMap.keySet()) {
        List<Comb> combList = combMap.get(sg);

        List<Integer> rem = new ArrayList<Integer>(combList.size());

        for (int i = 0; i < combList.size(); ++i) {
          Comb c = combList.get(i);

          if (c.size() > minSupport) {
            rem.add(i);
          }
        }

        Collections.sort(rem);
        Collections.reverse(rem);

        for (int i : rem) {
          combList.remove(i);
        }
      }

      // Remove genes with no combs left

      List<Integer> rem = new ArrayList<Integer>(combMap.size());

      for (int sg : combMap.keySet()) {
        if (combMap.get(sg).size() == 0) {
          rem.add(sg);
        }
      }

      for (int sg : rem) {
        combMap.remove(sg);
      }

    }
  }

  /**
   * Normalize based on genes@work paper. The returned matrix will contain only
   * the columns associated with the phenotype group.
   * 
   * @param m
   * @param phenGroup
   * @param controlGroup
   * @return
   */
  private static DataFrame normPhenToControl(final DataFrame m,
      final MatrixGroup phenGroup,
      final MatrixGroup controlGroup) {
    List<Integer> phenIndices = MatrixGroup.findColumnIndices(m, phenGroup);

    List<Integer> controlIndices = MatrixGroup.findColumnIndices(m,
        controlGroup);

    DataFrame ret = DataFrame.createNumericalMatrix(m.getRows(),
        phenIndices.size());

    DataFrame.copyRowAnnotations(m, ret);

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

    for (int i = 0; i < m.getRows(); ++i) {
      double[] phenPoints = CollectionUtils.subList(m.rowToDoubleArray(i),
          phenIndices);

      double[] controlPoints = CollectionUtils.subList(m.rowToDoubleArray(i),
          controlIndices); // MatrixOperations.rowToList(m,
                           // i, controlIndices);

      Arrays.sort(phenPoints);
      Arrays.sort(controlPoints);

      // System.err.println("aha " + m.getRowName(i) + " " +
      // Arrays.toString(controlPoints));

      double[] p1y;

      if (Mathematics.sum(phenPoints) == 0) {
        p1y = Mathematics.zerosArray(phenPoints.length);
      } else {
        KernelDensity density = new NormKernelDensity(controlPoints);

        // evaluate p1 normalizing to p2
        p1y = density.cdf(phenPoints);
      }

      // System.err.println("phen " + Arrays.toString(controlPoints) + " " +
      // Arrays.toString(p1y));

      for (int s1 = 0; s1 < p1y.length; ++s1) {
        ret.set(i, s1, p1y[s1]);
      }
    }

    return ret;
  }

  public static <X extends MatrixGroup> DataFrame groupZScoreMatrix(DataFrame m,
      XYSeriesGroup comparisonGroups,
      List<X> groups) {

    DataFrame ret = DataFrame.createNumericalMatrix(m);

    // DataFrame.copyColumnAnnotations(m, ret);
    // DataFrame.copyRowAnnotations(m, ret);

    // We normalize the comparison groups separately to the the others
    List<List<Integer>> comparisonIndices = MatrixGroup.findColumnIndices(m,
        comparisonGroups);

    for (XYSeries g : comparisonGroups) {
      System.err.println("used " + g.getName());
    }

    // We ignore these indices when calculating the means for
    // the other groups
    Set<Integer> used = CollectionUtils
        .toSet(CollectionUtils.flatten(comparisonIndices));

    System.err.println("used " + used);

    List<List<Integer>> groupIndices = MatrixGroup.findColumnIndices(m, groups);

    System.err.println("all " + CollectionUtils.flatten(groupIndices));

    // Now normalize the the other groups

    for (int r = 0; r < m.getRows(); ++r) {
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
          // if (!used.contains(c)) {
          d1.add(m.getValue(r, c));
          // }
        }

        mean += Statistics.mean(d1);
        sd += Statistics.popStdDev(d1); // sampleStandardDeviation

        ++groupCount;
      }

      // System.err.println("m " + mean + " " + sd + " " + groupCount);

      mean /= groupCount;
      sd /= groupCount;

      // Normalize the values
      for (List<Integer> indices : groupIndices) {
        for (int c : indices) {
          // if (!used.contains(c)) {
          ret.set(r, c, (m.getValue(r, c) - mean) / sd);
          // }
        }
      }
    }

    // Normalize the comparisons

    /*
     * for (int i = 0; i < m.getRowCount(); ++i) { double mean = 0; double sd =
     * 0;
     * 
     * int groupCount = 0;
     * 
     * for (List<Integer> indices : comparisonIndices) { if (indices.size() ==
     * 0) { continue; }
     * 
     * List<Double> d1 = new ArrayList<Double>(indices.size());
     * 
     * for (int c : indices) { d1.add(m.getValue(i, c)); }
     * 
     * mean += Statistics.mean(d1); sd += Statistics.popStdDev(d1); //
     * sampleStandardDeviation
     * 
     * ++groupCount; }
     * 
     * mean /= groupCount; sd /= groupCount;
     * 
     * // Normalize the values for (List<Integer> indices : comparisonIndices) {
     * if (indices.size() == 0) { continue; }
     * 
     * for (int c : indices) { ret.setValue(i, c, (m.getValue(i, c) - mean) /
     * sd); } } }
     */

    return ret;
  }

  public static void filterPatterns(int minGenes,
      Map<Integer, IterMap<Comb, Set<Integer>>> patternMap) {
    for (int cs : patternMap.keySet()) {
      List<Comb> rem = new ArrayList<Comb>(patternMap.size());

      for (Comb c : CollectionUtils.sortKeys(patternMap.get(cs))) {
        Set<Integer> genes = patternMap.get(cs).get(c);

        int n = genes.size();

        if (n < minGenes) {
          System.err.println("fp " + c + " " + genes.size());
          rem.add(c);
        }
      }

      for (Comb c : rem) {
        patternMap.get(cs).remove(c);
      }
    }
  }

  public static List<Pattern> sortPatterns(
      Map<Integer, IterMap<Comb, Set<Integer>>> patternMap) {
    List<Pattern> patterns = new ArrayList<Pattern>();

    for (int cs : patternMap.keySet()) {
      Map<Integer, IterMap<Comb, Collection<Integer>>> sizeMap = DefaultTreeMap
          .create(new TreeMapCreator<Comb, Collection<Integer>>());

      for (Comb c : patternMap.get(cs).keySet()) {
        Collection<Integer> genes = patternMap.get(cs).get(c);

        sizeMap.get(genes.size()).put(c, genes);
      }

      for (int gs : sizeMap.keySet()) {
        for (Comb c : CollectionUtils.sortKeys(sizeMap.get(gs))) {
          patterns.add(new Pattern(c, sizeMap.get(gs).get(c)));
        }
      }
    }

    return patterns;
  }

  @SafeVarargs
  public static List<Pattern> sortPatterns(
      Map<Integer, Map<Comb, Set<Integer>>> patternMap,
      Map<Integer, Map<Comb, Set<Integer>>>... patternMaps) {
    List<Pattern> patterns = sortPatterns(patternMap);

    for (Map<Integer, Map<Comb, Set<Integer>>> pm : patternMaps) {
      patterns.addAll(sortPatterns(pm));
    }

    return patterns;
  }

  /**
   * Create the largest patterns from the collection of patterns.
   * 
   * @param controlPatterns
   * @param minGenes
   * @return
   */
  public static Map<Integer, Pattern> maximalPatterns(
      Map<Integer, Map<Comb, Collection<Integer>>> patternMap,
      int minGenes) {
    Map<Integer, Pattern> maxPatternMap = new TreeMap<Integer, Pattern>();

    for (int cs : patternMap.keySet()) {
      for (Comb c : CollectionUtils.sortKeys(patternMap.get(cs))) {
        Collection<Integer> genes = patternMap.get(cs).get(c);

        int n = genes.size();

        System.err.println("c " + cs + " " + c + " " + n);

        if (n >= minGenes) {
          if (!maxPatternMap.containsKey(cs)) {
            maxPatternMap.put(cs, new Pattern(c, genes));
          } else {
            if (n > maxPatternMap.get(cs).size()) {
              maxPatternMap.put(cs, new Pattern(c, genes));
            }
          }
        }
      }
    }

    return maxPatternMap;
  }
}
