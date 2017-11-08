package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.util.Map;
import java.util.Set;

import org.jebtk.core.collections.DefaultHashMap;
import org.jebtk.core.collections.DefaultHashMapCreator;
import org.jebtk.core.collections.HashSetCreator;
import org.jebtk.core.collections.IterMap;

public class LeftMaximalCombs {
	private Map<Integer, IterMap<Integer, Set<Comb>>> mCombs =
			DefaultHashMap.create(new DefaultHashMapCreator<Integer, Set<Comb>>(new HashSetCreator<Comb>()));
	
	private int mMax = -1;
	
	public boolean isExpCombNew(Pattern p) {
		return isExpCombNew(p.getComb()); //!combs.contains(p.getComb().hashCode());
	}

	/**
	 * Returns true only if the comb is not in a set of previously used
	 * combs.
	 * 
	 * @param c
	 * @param combs
	 * @return
	 */
	public boolean isExpCombNew(Comb c) {
		// Test whether a larger comb includes this comb
		
		if (mMax > c.size()) {
			for (int i = c.size(); i <= mMax; ++i) {
				for (Comb ct : mCombs.get(i).get(c.hashCode())) {
					// If there is an existing comb which is larger than c and 
					// contains c then this is not a new exp comb
					if (Comb.isSuper(c, ct)) {
						return false;
					}
				}
			}
		}

		return true; //!combs.contains(p.getComb().hashCode());
	}

	public void addComb(Pattern p) {
		addComb(p.getComb());
	}
	
	public void addComb(Comb c) {
		if (!mCombs.get(c.size()).get(c.hashCode()).contains(c)) {
			mCombs.get(c.size()).get(c.hashCode()).add(c);
		
			mMax = Math.max(mMax, c.size());

			//System.err.println("comb " + c.hashCode() + " " + c.toString());
		}
	}
}
