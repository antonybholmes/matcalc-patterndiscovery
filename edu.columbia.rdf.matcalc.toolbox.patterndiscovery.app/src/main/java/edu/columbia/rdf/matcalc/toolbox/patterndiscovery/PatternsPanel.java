package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;

import org.jebtk.core.Properties;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.collections.CountMap;
import org.jebtk.core.collections.UniqueArrayList;
import org.jebtk.graphplot.figure.heatmap.legacy.CountGroup;
import org.jebtk.graphplot.figure.heatmap.legacy.CountGroups;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.graphplot.figure.series.XYSeriesModel;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.BorderService;
import org.jebtk.modern.ModernComponent;
import org.jebtk.modern.UI;
import org.jebtk.modern.AssetService;
import org.jebtk.modern.button.CheckBox;
import org.jebtk.modern.button.ModernButton;
import org.jebtk.modern.button.ModernButtonGroup;
import org.jebtk.modern.button.ModernCheckBox;
import org.jebtk.modern.button.ModernCheckSwitch;
import org.jebtk.modern.button.ModernRadioButton;
import org.jebtk.modern.dialog.ModernMessageDialog;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.graphics.icons.RunVectorIcon;
import org.jebtk.modern.help.ModernDialogHelpButton;
import org.jebtk.modern.listpanel.ModernListPanel;
import org.jebtk.modern.listpanel.ModernListPanelItem;
import org.jebtk.modern.panel.HBox;
import org.jebtk.modern.panel.VBox;
import org.jebtk.modern.scrollpane.ModernScrollPane;
import org.jebtk.modern.scrollpane.ScrollBarLocation;
import org.jebtk.modern.scrollpane.ScrollBarPolicy;
import org.jebtk.modern.text.ModernSubHeadingLabel;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.figure.PlotConstants;

public class PatternsPanel extends ModernComponent
    implements ModernClickListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private ModernRadioButton mCheckUnion = new ModernRadioButton("Union", true);

  private ModernRadioButton mCheckIntersect = new ModernRadioButton(
      "Intersection");

  private CheckBox mCheckPlot = new ModernCheckSwitch(PlotConstants.MENU_PLOT,
      true);

  private CheckBox mCheckMerge = new ModernCheckSwitch("Merge Patterns", true);

  private CheckBox mCheckUnique = new ModernCheckSwitch("Unique Samples", true);

  private CheckBox mCheckSelectAll = new ModernCheckSwitch(UI.MENU_SELECT_ALL,
      true);

  private ModernButton mButtonUpdate = new ModernButton(
      PlotConstants.MENU_UPDATE,
      AssetService.getInstance().loadIcon(RunVectorIcon.class, 16));

  private DataFrame mM;

  private MainMatCalcWindow mWindow;

  // private Map<Pattern, CheckBox> mPhenSelMap =
  // new HashMap<Pattern, CheckBox>();

  // private Map<Pattern, CheckBox> mConSelMap =
  // new HashMap<Pattern, CheckBox>();

  private XYSeriesModel mGroups;

  private XYSeriesGroup mComparisonGroups;

  private Properties mProperties;

  private ModernListPanel mML = new ModernListPanel();

  public PatternsPanel(MainMatCalcWindow window, DataFrame m,
      XYSeries phenGroup, XYSeries controlGroup, List<Pattern> phenPatterns,
      List<Pattern> phenPatterns2, List<Pattern> controlPatterns,
      List<Pattern> controlPatterns2, XYSeriesModel groups,
      XYSeriesGroup comparisonGroups, boolean plot, Properties properties) {
    mWindow = window;
    mM = m;

    mGroups = groups;
    mComparisonGroups = comparisonGroups;
    mProperties = properties;

    Box box;

    box = VBox.create();

    // box.add(mCheckPlot);

    // box.add(UI.createVGap(10));
    // box.add(new ModernSeparator());
    // box.add(UI.createVGap(10));

    box.add(UI.createVGap(10));
    box.add(mCheckIntersect);
    box.add(mCheckUnion);
    box.add(UI.createVGap(5));
    box.add(mCheckMerge);
    box.add(UI.createVGap(5));
    box.add(mCheckUnique);
    box.add(UI.createVGap(20));

    box.add(new ModernSubHeadingLabel("Patterns"));
    box.add(UI.createVGap(10));

    setHeader(box);

    // box = VBox.create();

    ModernComponent panel = new ModernComponent();

    panel.setHeader(
        new ModernComponent(mCheckSelectAll, BorderService.getInstance()
            .createBorder(PADDING, PADDING, DOUBLE_PADDING, PADDING)));

    loadPatterns(phenGroup, phenPatterns, phenGroup.getColor(), mML);
    loadPatterns(controlGroup, phenPatterns2, phenGroup.getColor(), mML);

    loadPatterns(controlGroup, controlPatterns, controlGroup.getColor(), mML);
    loadPatterns(phenGroup, controlPatterns2, controlGroup.getColor(), mML);

    ModernScrollPane scrollPane = new ModernScrollPane(mML)
        .setHorizontalScrollBarPolicy(ScrollBarPolicy.NEVER)
        .setVerticalScrollBarPolicy(ScrollBarPolicy.AUTO_SHOW)
        .setVScrollBarLocation(ScrollBarLocation.FLOATING);

    panel.setBody(scrollPane);
    setBody(panel);

    box = VBox.create();
    box.add(UI.createVGap(20));

    Box box2 = HBox.create();
    box2.add(mButtonUpdate);
    box2.add(UI.createHGap(10));
    box2.add(mCheckPlot);
    box.add(box2);
    box.add(UI.createVGap(20));
    box.add(new ModernDialogHelpButton("patterndiscovery.help.url"));
    box.add(UI.createVGap(10));

    setFooter(box);

    // setBorder(LARGE_BORDER);

    mCheckPlot.setSelected(plot);

    // Select the smallest patterns from each
    // selectPattern(phenPatterns, mPhenSelMap);
    // selectPattern(controlPatterns, mConSelMap);

    new ModernButtonGroup(mCheckUnion, mCheckIntersect);

    mButtonUpdate.addClickListener(this);

    mCheckSelectAll.addClickListener(new ModernClickListener() {

      @Override
      public void clicked(ModernClickEvent e) {
        selectAll();
      }
    });
  }

  private void selectAll() {
    for (ModernListPanelItem item : mML) {
      PatternPanel pp = (PatternPanel) item.getComponent();

      pp.setSelected(mCheckSelectAll.isSelected());
    }
  }

  @Override
  public void clicked(ModernClickEvent e) {
    filter();
  }

  public void filter() {
    List<Integer> biggestCombGenes;

    if (mCheckUnique.isSelected()) {
      biggestCombGenes = new UniqueArrayList<Integer>(mM.getRows());
    } else {
      biggestCombGenes = new ArrayList<Integer>(mM.getRows());
    }

    if (mCheckIntersect.isSelected()) {
      // Intersection of genes.

      int sn = countSelected();

      // See how many times each gene is included in a pattern
      // For the intersection, the sum of a gene must equal the
      // the of patterns it is supposed to be in.
      CountMap<Integer> countMap = CountMap.create();

      for (ModernListPanelItem item : mML) {
        PatternPanel pp = (PatternPanel) item.getComponent();

        if (pp.isSelected()) {
          // countMap.putAll(pp.getPattern());

          for (int g : pp.getPattern()) {
            countMap.inc(g);
          }
        }
      }

      for (ModernListPanelItem item : mML) {
        PatternPanel pp = (PatternPanel) item.getComponent();

        if (pp.isSelected()) {
          for (int g : pp.getPattern()) {
            if (countMap.get(g) == sn) {
              biggestCombGenes.add(g);
            }
          }
        }
      }
    } else {
      // Union of all genes

      for (ModernListPanelItem item : mML) {
        PatternPanel pp = (PatternPanel) item.getComponent();

        if (pp.isSelected()) {
          CollectionUtils.addAll(pp.getPattern(), biggestCombGenes);
        }
      }

      /*
       * for (Pattern p : mPhenPatterns) { if (mPhenSelMap.get(p).isSelected())
       * { CollectionUtils.addAll(p, biggestCombGenes); } }
       * 
       * for (Pattern p : mControlPatterns) { if
       * (mConSelMap.get(p).isSelected()) { CollectionUtils.addAll(p,
       * biggestCombGenes); } }
       */
    }

    if (biggestCombGenes.size() == 0) {
      ModernMessageDialog.createWarningDialog(mWindow,
          "No suitable patterns could be found.");

      return;
    }

    //
    // The rest of this method is boilerpoint code for sorting
    // and displaying the matrix and is not specific to pattern discovery

    // which indices occur in both groups

    if (mCheckMerge.isSelected()) {
      biggestCombGenes = CollectionUtils.sort(biggestCombGenes); // Collections.emptyList();
    }

    // CollectionUtils.sort(CollectionUtils.intersect(phenPatterns.keySet(),
    // controlPatterns.keySet()));

    DataFrame biggestPatternM = DataFrame.copyRows(mM, biggestCombGenes);

    if (mCheckPlot.isSelected()) {
      // Count how many are up or down

      CountGroups countGroups = null;

      if (mCheckMerge.isSelected()) {
        countGroups = createCountGroups(biggestPatternM);
      } else {
        countGroups = new CountGroups();

        int c = 0;

        Set<Integer> used = new HashSet<Integer>();

        boolean unique = mCheckUnique.isSelected();

        for (ModernListPanelItem item : mML) {
          PatternPanel pp = (PatternPanel) item.getComponent();

          if (pp.isSelected()) {
            int s = 0;

            if (unique) {
              for (int g : pp.getPattern()) {
                if (!used.contains(g)) {
                  ++s;
                  used.add(g);
                }
              }
            } else {
              s = pp.getPattern().size();
            }

            countGroups.add(new CountGroup(pp.getTitle(), c, c + s - 1));

            c += s;
          }
        }
      }

      List<String> history = mWindow.getTransformationHistory();

      int index = mWindow.searchHistory("Patterns");

      // Replace history after control curves
      mWindow.history().addToHistory(index,
          new PatternDiscoveryPlotMatrixTransform(mWindow, "Patterns Plot",
              biggestPatternM, mGroups, mComparisonGroups, XYSeriesModel.EMPTY_SERIES,
              countGroups, history, mProperties, true));

      // if (mCheckMerge.isSelected()) {
      // mWindow.history().addToHistory("Merged", patternM);
      // } else {
      // Create a separate matrix for each pattern

      for (ModernListPanelItem item : mML) {
        PatternPanel pp = (PatternPanel) item.getComponent();

        // if (pp.isSelected()) {
        DataFrame pM = DataFrame.copyRows(mM,
            CollectionUtils.toList(pp.getPattern()));

        // mWindow.history().addToHistory(pp.getName(), pM);

        countGroups = createCountGroups(pM);

        mWindow.history().addToHistory(new PatternDiscoveryPlotMatrixTransform(mWindow,
            pp.getName() + " Plot", pM, mGroups, mComparisonGroups,
            XYSeriesModel.EMPTY_SERIES, countGroups, history, mProperties,
            false));

        // }
      }
      // }

      // Change selection to main plot
      index = mWindow.searchHistory("Patterns Plot");
      mWindow.selectHistory(index);

      // mWindow.history().addToHistory("Results", patternM);
    }
  }

  /**
   * Count how many patterns the user selected.
   * 
   * @param phenSelMap
   * @param conSelMap
   * @return
   */
  private int countSelected() {
    int ret = 0;

    for (ModernListPanelItem item : mML) {
      PatternPanel p = (PatternPanel) item.getComponent();

      if (p.isSelected()) {
        ++ret;
      }
    }

    /*
     * for (Pattern p : phenSelMap.keySet()) { if
     * (phenSelMap.get(p).isSelected()) { ++ret; } }
     * 
     * for (Pattern p : conSelMap.keySet()) { if (conSelMap.get(p).isSelected())
     * { ++ret; } }
     */

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
      Color color,
      ModernListPanel box) {
    // box.add(new ModernLabelBold(group.getName()));
    // //ModernSubHeadingLabel(group.getName()));
    // box.add(UI.createVGap(5));

    for (Pattern p : patterns) {
      PatternPanel c = new PatternPanel(group, p, color);

      box.add(c, color);
    }
  }

  private void loadPatterns(XYSeries group,
      List<Pattern> patterns,
      Map<Pattern, CheckBox> selMap) {
    for (Pattern p : patterns) {
      String label = "<" + p.getComb().size() + ", " + p.size() + "> ("
          + p.getComb() + ")";

      ModernCheckBox c = new ModernCheckBox(label, true);

      selMap.put(p, c);
    }
  }

  private static CountGroups createCountGroups(DataFrame m) {
    CountGroups countGroups = new CountGroups();

    double[] zscores = m.getRowAnnotationValues("Z-score");

    int c = 0;

    // Since the z-scores are ordered, simply find the inflection
    // point from positive to negative to know how many positive
    // and negative samples there are

    while (c < zscores.length) {
      if (zscores[c] < 0) {
        break;
      }

      ++c;
    }

    System.err.println("up " + c);

    if (c > 0) {
      countGroups.add(new CountGroup("up", 0, c - 1));
    }

    int c2 = zscores.length - 1;

    if (c2 - c > 0) {
      countGroups.add(new CountGroup("down", c, c2));
    }

    return countGroups;
  }
}
