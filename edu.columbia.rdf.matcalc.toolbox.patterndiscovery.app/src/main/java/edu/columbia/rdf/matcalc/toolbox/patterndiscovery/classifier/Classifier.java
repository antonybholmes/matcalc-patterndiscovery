package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.classifier;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jebtk.core.NameProperty;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.xml.XmlRepresentation;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.math.matrix.AnnotatableMatrix;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.math.matrix.MatrixGroup;
import org.jebtk.math.matrix.utils.MatrixOperations;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Classifier implements Comparable<Classifier>, Iterable<String>, NameProperty, XmlRepresentation {
	private String mName;
	private AnnotationMatrix mPhenM;
	private AnnotationMatrix mControlM;
	private Map<String, Integer> mGeneMap;
	private String mPhenotype;
	private String mControl;
	private List<String> mGenes;

	public Classifier(String name, 
			AnnotationMatrix m, 
			XYSeries phenotypeGroup, 
			XYSeries controlGroup,
			String annotation) {
		mName = name;
		
		mPhenotype = phenotypeGroup.getName();
		mControl = controlGroup.getName();
		
		List<Integer> phenotypeIndices = 
				MatrixGroup.findColumnIndices(m, phenotypeGroup);
		
		List<Integer> controlIndices = 
				MatrixGroup.findColumnIndices(m, controlGroup);
		
		mGenes = m.getRowAnnotationText(annotation);
		
		mGeneMap = CollectionUtils.toIndexMap(mGenes);
		
		mPhenM = AnnotatableMatrix.createNumericalMatrix(mGenes.size(), 
				phenotypeIndices.size());
		
		mControlM = AnnotatableMatrix.createNumericalMatrix(mGenes.size(), 
				controlIndices.size());
		
		int c = 0;
		
		for (int i : phenotypeIndices) {
			mPhenM.copyColumn(m, i, c++);
		}
		
		c = 0;
		
		for (int i : controlIndices) {
			mControlM.copyColumn(m, i, c++);
		}
	}
	
	public Classifier(String name, 
			String phenotype,
			AnnotationMatrix phenM,
			String control,
			AnnotationMatrix controlM,
			List<String> genes) {
		mName = name;
		
		mPhenotype = phenotype;
		mControl = control;
		
		mGenes = genes;
		mGeneMap = CollectionUtils.toIndexMap(genes);
		
		mPhenM = phenM;
		
		mControlM = controlM;
	}
	
	@Override
	public String getName() {
		return mName;
	}

	@Override
	public Iterator<String> iterator() {
		return mGenes.iterator();
	}
	

	public int getGeneCount() {
		return mGenes.size();
	}

	public String getGene(int g) {
		return mGenes.get(g);
	}
	
	public double getPhenotypeMean(String gene) {
		return MatrixOperations.mean(mPhenM, mGeneMap.get(gene));
	}
	
	public AnnotationMatrix getPhenotype() {
		return mPhenM;
	}
	
	public String getPhenotypeName() {
		return mPhenotype;
	}
	
	public double getControlMean(String gene) {
		return MatrixOperations.mean(mControlM, mGeneMap.get(gene));
	}

	public AnnotationMatrix getControl() {
		return mControlM;
	}
	
	public String getControlName() {
		return mControl;
	}
	
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Classifier) {
			return compareTo((Classifier)o) == 0;
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(Classifier c) {
		return mName.compareTo(c.mName);
	}
	
	@Override
	public int hashCode() {
		return mName.hashCode();
	}
	
	public static Classifier create(String name,
			AnnotationMatrix m, 
			XYSeries phenotypeGroup, 
			XYSeries controlGroup,
			String annotation) {
		return new Classifier(name,
				m,
				phenotypeGroup,
				controlGroup,
				annotation);
	}
	
	public static Classifier create(String name, 
			String phenotype,
			AnnotationMatrix phenM,
			String control,
			AnnotationMatrix controlM,
			List<String> genes) {
		return new Classifier(name,
				phenotype,
				phenM,
				control,
				controlM,
				genes);
	}

	@Override
	public Element toXml(Document doc) {
		Element e = doc.createElement("classifier");
		e.setAttribute("name", mName);
		e.setAttribute("phenotype", mPhenotype);
		e.setAttribute("phenotype-size", Integer.toString(mPhenM.getColumnCount()));
		e.setAttribute("control", mControl);
		e.setAttribute("control-size", Integer.toString(mControlM.getColumnCount()));
		e.setAttribute("size", Integer.toString(mGenes.size()));
		
		//XmlElement gse = new XmlElement("genes");
		
		for (String name : CollectionUtils.sort(mGenes)) {
			Element ge = doc.createElement("gene");
			
			ge.setAttribute("name", name);
			
			Element pe = doc.createElement("phenotype");
			
			ge.appendChild(pe);
			
			for (int i = 0; i < mPhenM.getColumnCount(); ++i) {
				Element se = doc.createElement("sample");
				//se.setAttribute("name", mPhenM.getColumnName(i));
				se.setAttribute("value", mPhenM.getText(mGeneMap.get(name), i));
				pe.appendChild(se);
			}
			
			Element ce = doc.createElement("control");
			
			ge.appendChild(ce);
			
			for (int i = 0; i < mControlM.getColumnCount(); ++i) {
				Element se = doc.createElement("sample");
				//se.setAttribute("name", mControlM.getColumnName(i));
				se.setAttribute("value", mControlM.getText(mGeneMap.get(name), i));
				ce.appendChild(se);
			}
			
			e.appendChild(ge);
		}
		
		return e;
	}

	public List<String> getGenes() {
		return mGenes;
	}
}
