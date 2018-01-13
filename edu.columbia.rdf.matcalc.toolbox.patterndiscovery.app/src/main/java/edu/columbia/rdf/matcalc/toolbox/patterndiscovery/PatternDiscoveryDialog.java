/**
 * Copyright 2016 Antony Holmes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import javax.swing.Box;

import org.jebtk.core.settings.SettingsService;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.math.matrix.MatrixGroup;
import org.jebtk.modern.UI;
import org.jebtk.modern.button.ModernCheckSwitch;
import org.jebtk.modern.combobox.ModernComboBox;
import org.jebtk.modern.dialog.ModernDialogHelpWindow;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.panel.HBox;
import org.jebtk.modern.panel.VBox;
import org.jebtk.modern.spinner.ModernCompactSpinner;
import org.jebtk.modern.text.ModernLabel;
import org.jebtk.modern.widget.ModernTwoStateWidget;
import org.jebtk.modern.widget.ModernWidget;
import org.jebtk.modern.window.ModernWindow;
import org.jebtk.modern.window.WindowWidgetFocusEvents;

import edu.columbia.rdf.matcalc.GroupsCombo;
import edu.columbia.rdf.matcalc.figure.PlotConstants;

/**
 * The class PatternDiscoveryDialog.
 */
public class PatternDiscoveryDialog extends ModernDialogHelpWindow
    implements ModernClickListener {

  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /** The Constant FIELD_WIDTH. */
  private static final int FIELD_WIDTH = 120;

  /**
   * The member group1 combo.
   */
  private ModernComboBox mPhenCombo = null;

  /**
   * The member group2 combo.
   */
  private ModernComboBox mControlCombo = null;

  /**
   * The check log2.
   */
  // private ModernCheckBox checkLog2 =
  // new ModernCheckBox(PlotConstants.MENU_LOG_TRANSFORM);

  /**
   * The delta field.
   */
  private ModernCompactSpinner mDeltaField = new ModernCompactSpinner(0, 1,
      SettingsService.getInstance().getAsDouble("pattern-discovery.delta"),
      0.01);

  /**
   * The support1 field.
   */
  private ModernCompactSpinner mPhenField = new ModernCompactSpinner(1, 100,
      10);

  /**
   * The support2 field.
   */
  private ModernCompactSpinner mControlField = new ModernCompactSpinner(1, 100,
      10);

  /** The m genes field. */
  private ModernCompactSpinner mGenesField = new ModernCompactSpinner(2,
      1000000,
      SettingsService.getInstance().getAsDouble("pattern-discovery.min-genes"));

  /** The m check phen min sup only. */
  private ModernTwoStateWidget mCheckPhenMinSupOnly = new ModernCheckSwitch(
      "only");

  /** The m check con min sup only. */
  private ModernTwoStateWidget mCheckConMinSupOnly = new ModernCheckSwitch(
      "only");

  /** The m check log. */
  private ModernTwoStateWidget mCheckLogMode = new ModernCheckSwitch(
      PlotConstants.MENU_LOG_TRANSFORM);

  private ModernTwoStateWidget mCheckLog = new ModernCheckSwitch(
      PlotConstants.MENU_IS_LOG_TRANSFORMED);

  // private ModernTwoStateWidget mCheckBidirectional =
  // new ModernCheckSwitch("Intersect phenotype and control", true);

  /** The m Z score field. */
  private ModernCompactSpinner mZScoreField = null;

  /**
   * The n field.
   */
  // private ModernTextField nField =
  // new ModernNumericalTextField("1000");

  /**
   * The percentile field.
   */
  // private ModernTextField percentileField =
  /// new ModernNumericalTextField("80");

  /**
   * The snr field.
   */
  // private ModernTextField snrField =
  // new ModernNumericalTextField("2.0");

  /**
   * The check plot.
   */
  private ModernTwoStateWidget mCheckPlot = new ModernCheckSwitch(
      PlotConstants.MENU_PLOT, true);

  /**
   * The check reset.
   */
  private ModernTwoStateWidget mCheckReset = new ModernCheckSwitch(
      PlotConstants.MENU_RESET_HISTORY);

  /**
   * The member groups.
   */
  private XYSeriesGroup mGroups;

  /**
   * The member matrix.
   */
  private DataFrame mMatrix;

  /**
   * The class Group1ModernClickEvent.
   */
  private class Group1ModernClickEvent implements ModernClickListener {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.abh.lib.ui.modern.event.ModernClickListener#clicked(org.abh.lib.ui.
     * modern .event.ModernClickEvent)
     */
    @Override
    public void clicked(ModernClickEvent e) {
      int n = MatrixGroup.findColumnIndices(mMatrix, getPhenGroup()).size();

      // mPhenField.setMax(n);
      mPhenField.setValue(n);
    }

  }

  /**
   * The class Group2ModernClickEvent.
   */
  private class Group2ModernClickEvent implements ModernClickListener {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.abh.lib.ui.modern.event.ModernClickListener#clicked(org.abh.lib.ui.
     * modern .event.ModernClickEvent)
     */
    @Override
    public void clicked(ModernClickEvent e) {
      int n = MatrixGroup.findColumnIndices(mMatrix, getControlGroup()).size();

      // mControlField.setMax(n);
      mControlField.setValue(n);
    }

  }

  /**
   * Instantiates a new pattern discovery dialog.
   *
   * @param parent the parent
   * @param matrix the matrix
   * @param groups the groups
   */
  public PatternDiscoveryDialog(ModernWindow parent, DataFrame matrix,
      XYSeriesGroup groups) {
    super(parent, "patterndiscovery.help.url");

    mMatrix = matrix;
    mGroups = groups;

    setTitle("Pattern Discovery");

    mZScoreField = new ModernCompactSpinner(0, 1000, SettingsService
        .getInstance().getAsDouble("pattern-discovery.min-z-score"));
    mZScoreField.setBounded(false);

    setup();

    createUi();

  }

  /**
   * Setup.
   */
  private void setup() {
    addWindowListener(new WindowWidgetFocusEvents(mOkButton));

    mPhenCombo = new GroupsCombo(mGroups);
    mControlCombo = new GroupsCombo(mGroups);

    // mGenesField.setMax(mMatrix.getRowCount());

    mPhenCombo.addClickListener(new Group1ModernClickEvent());
    mControlCombo.addClickListener(new Group2ModernClickEvent());

    mPhenCombo.setSelectedIndex(0);
    mControlCombo.setSelectedIndex(1);

    setSize(680, 480);

    UI.centerWindowToScreen(this);
  }

  /**
   * Creates the ui.
   */
  private final void createUi() {
    // this.getWindowContentPanel().add(new JLabel("Change " +
    // getProductDetails().getProductName() + " settings", JLabel.LEFT),
    // BorderLayout.PAGE_START);

    Box box = VBox.create();

    // box.add(new ModernDialogSectionSeparator("Filter options"));

    // matrixPanel = new MatrixPanel(rows, cols, ModernWidget.PADDING,
    // ModernWidget.PADDING);
    // matrixPanel.add(new ModernLabel("Signal/noise ratio"));
    // matrixPanel.add(new ModernTextBorderPanel(snrField));
    // matrixPanel.add(new ModernLabel("Fold change tests"));
    // matrixPanel.add(new ModernTextBorderPanel(nField));
    // matrixPanel.add(new ModernLabel("Fold percentile"));
    // matrixPanel.add(new ModernTextBorderPanel(percentileField));
    // matrixPanel.setBorder(ModernPanel.LARGE_BORDER);

    // box.add(matrixPanel);

    // box.add(new ModernDialogSectionSeparator("Group options"));

    sectionHeader("Samples", box);

    Box box3 = HBox.create();
    box3.add(new ModernLabel("Phenotype", FIELD_WIDTH));
    box3.add(ModernWidget.setSize(mPhenCombo, 200));
    box3.add(UI.createHGap(20));
    box3.add(new ModernLabel("with support", 90));
    // box3.add(UI.createHGap(10));
    box3.add(mPhenField);
    box3.add(UI.createHGap(10));
    box3.add(mCheckPhenMinSupOnly);
    box.add(box3);

    box.add(UI.createVGap(5));

    box3 = HBox.create();
    box3.add(new ModernLabel("Control", FIELD_WIDTH));
    box3.add(ModernWidget.setSize(mControlCombo, 200));
    box3.add(UI.createHGap(20));
    box3.add(new ModernLabel("with support", 90));
    // box3.add(UI.createHGap(10));
    box3.add(mControlField);
    box3.add(UI.createHGap(10));
    box3.add(mCheckConMinSupOnly);
    box.add(box3);

    // box.add(UI.createVGap(5));
    // box.add(mCheckBidirectional);

    midSectionHeader("Filtering", box);

    box3 = HBox.create();
    box3.add(new ModernLabel("Delta", FIELD_WIDTH));
    box3.add(mDeltaField);
    box.add(box3);
    box.add(UI.createVGap(5));
    box3 = HBox.create();
    box3.add(new ModernLabel("Minimum Genes", FIELD_WIDTH));
    box3.add(mGenesField);
    box.add(box3);
    box.add(UI.createVGap(5));
    box3 = HBox.create();
    box3.add(new ModernLabel("Minimum Z-score", FIELD_WIDTH));
    box3.add(mZScoreField);
    box.add(box3);

    box.add(UI.createVGap(30));
    box.add(mCheckLogMode);
    box.add(UI.createVGap(5));
    box.add(mCheckLog);
    box.add(UI.createVGap(5));
    box.add(mCheckPlot);

    // ModernComponent content = new ModernComponent();

    // content.setBody(new ModernDialogContentPanel(box));

    // box = VBox.create();

    // box.add(UI.createVGap(10));
    // box.add(mCheckPlot);
    // box.add(UI.createVGap(10));

    // content.setFooter(box);

    setDialogCardContent(box);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.abh.lib.ui.modern.event.ModernClickListener#clicked(org.abh.lib.ui.
   * modern .event.ModernClickEvent)
   */
  @Override
  public final void clicked(ModernClickEvent e) {
    if (e.getMessage().equals(UI.BUTTON_OK)) {

      SettingsService.getInstance().update("pattern-discovery.delta",
          mDeltaField.getValue());
      SettingsService.getInstance().update("pattern-discovery.min-genes",
          mGenesField.getIntValue());
      SettingsService.getInstance().update("pattern-discovery.min-z-score",
          mZScoreField.getValue());
    }

    super.clicked(e);
  }

  /**
   * Gets the group1.
   *
   * @return the group1
   */
  public XYSeries getPhenGroup() {
    return mGroups.get(mPhenCombo.getSelectedIndex());
  }

  /**
   * Gets the group2.
   *
   * @return the group2
   */
  public XYSeries getControlGroup() {
    return mGroups.get(mControlCombo.getSelectedIndex());
  }

  /**
   * Gets the delta.
   *
   * @return the delta
   */
  public double getDelta() {
    return mDeltaField.getValue();
  }

  /**
   * Gets the genes.
   *
   * @return the genes
   */
  public int getGenes() {
    return mGenesField.getIntValue();
  }

  /**
   * Gets the support1.
   *
   * @return the support1
   */
  public int getPhenSupport() {
    return mPhenField.getIntValue();
  }

  /**
   * Gets the support2.
   *
   * @return the support2
   */
  public int getControlSupport() {
    return mControlField.getIntValue();
  }

  /**
   * Gets the phen support min only.
   *
   * @return the phen support min only
   */
  public boolean getPhenSupportMinOnly() {
    return mCheckPhenMinSupOnly.isSelected();
  }

  /**
   * Gets the control support min only.
   *
   * @return the control support min only
   */
  public boolean getControlSupportMinOnly() {
    return mCheckConMinSupOnly.isSelected();
  }

  /**
   * Gets the min Z score.
   *
   * @return the min Z score
   */
  public double getMinZScore() {
    return mZScoreField.getValue();
  }

  /**
   * Gets the min snr.
   *
   * @return the min snr
   */
  // public double getMinSNR() {
  // return Double.parseDouble(snrField.getText());
  // }

  /**
   * Gets the plot.
   *
   * @return the plot
   */
  public boolean getPlot() {
    return mCheckPlot.isSelected();
  }

  /**
   * Gets the n.
   *
   * @return the n
   */
  // public int getN() {
  // return Integer.parseInt(nField.getText());
  // }

  /**
   * Gets the percentile.
   *
   * @return the percentile
   */
  // public int getPercentile() {
  // return Integer.parseInt(percentileField.getText());
  // }

  /**
   * Gets the reset.
   *
   * @return the reset
   */
  public boolean getReset() {
    return mCheckReset.isSelected();
  }

  /**
   * Gets the checks if is log data.
   *
   * @return the checks if is log data
   */
  public boolean getIsLogData() {
    return mCheckLog.isSelected();
  }

  public boolean getLogMode() {
    return mCheckLogMode.isSelected();
  }

  // public boolean getBidirectional() {
  // return mCheckBidirectional.isSelected();
  // }
}
