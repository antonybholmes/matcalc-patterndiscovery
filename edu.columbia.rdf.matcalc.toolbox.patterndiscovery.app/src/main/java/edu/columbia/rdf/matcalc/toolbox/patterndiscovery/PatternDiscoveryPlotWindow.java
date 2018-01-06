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
package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.util.List;

import org.jebtk.core.Properties;
import org.jebtk.graphplot.figure.heatmap.legacy.CountGroups;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.graphplot.figure.series.XYSeriesModel;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.window.ModernRibbonWindow;

import edu.columbia.rdf.matcalc.toolbox.plot.heatmap.legacy.DifferentialExpressionPlotWindow;

/**
 * Merges designated segments together using the merge column. Consecutive rows
 * with the same merge id will be merged together. Coordinates and copy number
 * will be adjusted but genes, cytobands etc are not.
 *
 * @author Antony Holmes Holmes
 *
 */
public class PatternDiscoveryPlotWindow extends DifferentialExpressionPlotWindow {
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates a new pattern discovery plot window.
   *
   * @param appInfo
   *          the app info
   * @param matrix
   *          the matrix
   * @param comparisonGroups
   *          the comparison groups
   * @param groups
   *          the all groups
   * @param history
   *          the history
   * @param properties
   *          the properties
   */
  public PatternDiscoveryPlotWindow(ModernRibbonWindow window, DataFrame matrix, XYSeriesModel groups,
      XYSeriesGroup comparisonGroups, XYSeriesModel rowGroups, CountGroups countGroups, List<String> history,
      Properties properties) {
    super(window, "Pattern Discovery", matrix, groups, comparisonGroups, rowGroups, countGroups, history, properties);
  }

  /*
   * @Override public void setMatrix(DataFrame matrix) { super.setMatrix(matrix);
   * 
   * setFormatPane(); }
   */

}
