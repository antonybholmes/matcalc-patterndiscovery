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

import java.awt.Color;
import java.awt.Graphics2D;

import org.jebtk.core.ColorUtils;
import org.jebtk.graphplot.figure.Axes;
import org.jebtk.graphplot.figure.Figure;
import org.jebtk.graphplot.figure.Plot;
import org.jebtk.graphplot.figure.PlotClippedLayer;
import org.jebtk.graphplot.figure.SubFigure;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.modern.graphics.DrawingContext;

/**
 * Concrete implementation of Graph2dCanvas for generating
 * scatter plots.
 *
 * @author Antony Holmes Holmes
 */
public class ConfidenceLayer extends PlotClippedLayer {

	/**
	 * The constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	private double mMinY;

	private double mMaxY;

	private double mX;
	
	private static final Color COLOR =
			ColorUtils.decodeHtmlColor("#c0c0c080");

	public ConfidenceLayer(double x, double minY, double maxY) {
		super("Confidence");

		mX = x;
		mMinY = minY;
		mMaxY = maxY;
	}

	/* (non-Javadoc)
	 * @see edu.columbia.rdf.lib.bioinformatics.plot.figure.PlotClippedLayer#plotLayer(java.awt.Graphics2D, org.abh.common.ui.ui.graphics.DrawingContext, edu.columbia.rdf.lib.bioinformatics.plot.figure.Figure, edu.columbia.rdf.lib.bioinformatics.plot.figure.Axes, edu.columbia.rdf.lib.bioinformatics.plot.figure.Plot, org.abh.lib.math.matrix.AnnotationMatrix)
	 */
	@Override
	public void plotLayer(Graphics2D g2,
			DrawingContext context,
			Figure figure,
			SubFigure subFigure,
			Axes axes,
			Plot plot, 
			AnnotationMatrix m) {

		// the width of the arms of the plot assuming each bar has a 
		// nominal width of 1
		int x1 = axes.toPlotX1(mX - 0.4);
		int x2 = axes.toPlotX1(mX + 0.4);
		
		int w = x2 - x1 + 1;
		
		int y1 = axes.toPlotY1(mMaxY);
		int h = axes.toPlotY1(mMinY) - y1  + 1;

		g2.setColor(COLOR);
		
		System.err.println("rect " + x1 + " " + x2 + " " + y1 + " " + w + " " + h);
		
		g2.fillRect(x1, y1, w, h);
	}
}
