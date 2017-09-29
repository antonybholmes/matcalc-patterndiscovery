package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.classifier;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.jebtk.core.text.Parser;
import org.jebtk.math.matrix.AnnotatableMatrix;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ClassifierXmlHandler extends DefaultHandler {
	private String mName;
	private String mPhenotype;
	private String mControl;
	private int mPhenSize;
	private int mControlSize;
	private int mSize;
	private AnnotationMatrix mControlM;
	private AnnotationMatrix mPhenM;
	private String mGene;
	private List<String> mGenes = new ArrayList<String>();
	private boolean mPhenMode;
	private boolean mControlMode;

	private int mGeneIndex = -1;
	private int mColumnIndex = 0;

	@Override
	public void startElement(String uri, 
			String localName,
			String qName, 
			Attributes attributes) throws SAXException {

		if (qName.equals("classifier")) {
			mName = attributes.getValue("name");
			mPhenotype = attributes.getValue("phenotype");
			mGeneIndex = -1;
			
			System.err.println("classifier " + mName);
			
			try {
				mPhenSize = Parser.toInt(attributes.getValue("phenotype-size"));
			} catch (ParseException e2) {
				e2.printStackTrace();
			}

			mControl = attributes.getValue("control");

			try {
				mControlSize = Parser.toInt(attributes.getValue("control-size"));
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			try {
				mSize = Parser.toInt(attributes.getValue("size"));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			mPhenM = AnnotatableMatrix.createNumericalMatrix(mSize, mPhenSize);
			mControlM = AnnotatableMatrix.createNumericalMatrix(mSize, mControlSize);
			mGenes = new ArrayList<String>();
		} else if (qName.equals("gene")) {
			mGene = attributes.getValue("name");

			mGenes.add(mGene);

			++mGeneIndex;
		} else if (qName.equals("phenotype")) {
			mPhenMode = true;
			mColumnIndex = 0;
		} else if (qName.equals("control")) {
			mControlMode = true;
			mColumnIndex = 0;
		} else if (qName.equals("sample")) {
			try {
				double v = Parser.toDouble(attributes.getValue("value"));

				if (mPhenMode) {
					mPhenM.set(mGeneIndex, mColumnIndex++, v);
				}

				if (mControlMode) {
					mControlM.set(mGeneIndex, mColumnIndex++, v);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			// Do nothing
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (qName.equals("classifier")) {
			ClassifierService.getInstance().add(Classifier.create(mName,
					mPhenotype,
					mPhenM,
					mControl,
					mControlM,
					mGenes));
		} else if (qName.equals("phenotype")) {
			mPhenMode = false;
		} else if (qName.equals("control")) {
			mControlMode = false;
		} else {
			// Do nothing
		}
	}
}
