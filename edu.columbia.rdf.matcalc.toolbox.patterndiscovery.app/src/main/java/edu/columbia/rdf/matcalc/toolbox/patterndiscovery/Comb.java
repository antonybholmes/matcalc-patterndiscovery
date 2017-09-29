package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.text.Join;
import org.jebtk.math.MathUtils;

/**
 * A comb is an ordered set of experiment ids. Combs with the same
 * indices are considered equal and have the same hash and equals will
 * return true, even if they are are different objects.
 * 
 * @author Antony Holmes Holmes
 *
 */
public class Comb implements Comparable<Comb>, Iterable<Integer> {

	private Set<Integer> mExp = new HashSet<Integer>();
	private List<Integer> mSorted;
	private String mToString;
	private int mHash;

	public Comb(Collection<Integer> values) {
		mExp.addAll(values);
		mSorted = CollectionUtils.sort(mExp);
		mToString = toString(mSorted);
		mHash = mToString.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Comb) {
			return isSuper(this, (Comb)o);
		} else {
			return false;
		}
	}
	
	@Override
	public int compareTo(Comb p) {
		if (mExp.size() < p.mExp.size()) {
			return -1;
		} else if (mExp.size() > p.mExp.size()) {
			return 1;
		} else {
			return mToString.compareTo(p.mToString);
		}
	}
	
	@Override
	public int hashCode() {
		return mHash;
	}
	
	@Override
	public String toString() {
		return mToString;
	}

	@Override
	public Iterator<Integer> iterator() {
		return mSorted.iterator();
	}

	public int size() {
		return mExp.size();
	}
	
	public boolean contains(int e) {
		return mExp.contains(e);
	}
	
	/**
	 * Test whether comb c1 super pattern of c2, that is c1 must contain all
	 * of the samples of c2 although it can be larger.
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static boolean isSuper(Comb c1, Comb c2) {
		for (int e : c1) {
			if (!c2.contains(e)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean combsMatch(Comb c1, Comb c2) {
		if (c1.size() != c2.size()) {
			return false;
		}
		
		return isSuper(c1, c2);
	}

	public static Comb intersect(Pattern source, Pattern target) {
		return intersect(source.getComb(), target.getComb());
	}
	
	public static Comb intersect(Comb source, Comb target) {
		List<Integer> experiments = new ArrayList<Integer>(100);
		
		for (int c1 : source) {
			if (target.contains(c1)) {
				experiments.add(c1);
			}
		}
		
		return new Comb(experiments);
	}

	public static String toString(Collection<Integer> values) {
		// We add one so that the indices are more familiar to users who
		// count starting at one
		return Join.on(", ").values(MathUtils.add(values, 1)).toString();
	}
}
