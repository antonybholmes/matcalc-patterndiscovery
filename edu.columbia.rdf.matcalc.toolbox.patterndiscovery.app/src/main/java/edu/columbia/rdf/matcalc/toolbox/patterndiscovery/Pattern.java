package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * Patterns hold a reference to a comb. Patterns are unique objects thus
 * two patterns with the same comb indices will not be equal, but if their
 * two combs are compared they will be equal. This is because when a new
 * pattern is created from the merge of two sub patterns, it must not appear
 * equal to either of the sub patterns, even if all three have the same comb.
 * 
 * @author Antony Holmes Holmes
 *
 */
public class Pattern extends Comb {

	private Comb mExComb;
	
	public Pattern(Collection<Integer> genes, 
			Comb comb) {
		super(genes);
		mExComb = comb;
	}

	/**
	 * Returns a list representation of the comb with ordered indices.
	 * @return
	 */
	public Comb getComb() {
		return mExComb;
	}
	
	/**
	 * Merge two patterns together. The comb is the intersection of all 
	 * experiments, the genes are the union of the genes from the source
	 * and target.
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	public static Pattern intersect(Pattern source, Pattern target) {
		
		Comb comb = Comb.intersect(source, target);
		
		Set<Integer> genes = new HashSet<Integer>();
		
		for (int g : source) {
			genes.add(g);
		}
		
		for (int g : target) {
			genes.add(g);
		}
		
		Pattern ret = new Pattern(genes, comb);
		
		return ret;
	}

	
	public static double p(int experiments, 
			int genes, 
			int totalExp, 
			int totalGenes, 
			double delta) {
		double p = 0;

		try {
			double alpha = experiments * Math.pow(delta, experiments - 1) - (experiments - 1) * Math.pow(delta, experiments);

			long nt = CombinatoricsUtils.binomialCoefficient(totalGenes, genes) *
					CombinatoricsUtils.binomialCoefficient(totalExp, experiments);

			System.err.println(totalGenes + " " + genes + " " + nt + " " + Math.pow(alpha, genes) + " " + Math.pow(1 - alpha, totalGenes - genes) + " " + Math.pow(1 - Math.pow(1 + 1.0 / experiments, genes) * Math.pow(delta, genes), totalExp - experiments) + " genes");

			double nlk = nt *
					Math.pow(alpha, genes) *
					Math.pow(1 - alpha, totalGenes - genes) *
					Math.pow(1 - Math.pow(1 + 1.0 / experiments, genes) * Math.pow(delta, genes), totalExp - experiments);

			p = 1 - Math.exp(-nlk);
		} catch (Exception e) {
			//e.printStackTrace();
		}

		return p;
	}
}
