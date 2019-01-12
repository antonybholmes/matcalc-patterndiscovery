package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.jebtk.core.text.Join;
import org.jebtk.math.MathUtils;

/**
 * A comb is an ordered set of experiment ids. Combs with the same indices are
 * considered equal and have the same hash and equals will return true, even if
 * they are are different objects.
 * 
 * @author Antony Holmes
 *
 */
public class BaseComb implements Comparable<BaseComb>, Iterable<Integer> {

  private Set<Integer> mExp = new TreeSet<Integer>();
  // private List<Integer> mSorted;
  private String mToString;
  private int mHash;

  public BaseComb(Collection<Integer> values) {
    mExp.addAll(values);
    // mSorted = CollectionUtils.sort(mExp);
    mToString = toString(mExp);
    mHash = mToString.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BaseComb) {
      return isSuper(this, (BaseComb) o);
    } else {
      return false;
    }
  }

  @Override
  public int compareTo(BaseComb p) {
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
    return mExp.iterator();
  }

  public int size() {
    return mExp.size();
  }

  public boolean contains(int e) {
    return mExp.contains(e);
  }

  /**
   * Test whether comb c1 super pattern of c2, that is c1 must contain all of
   * the samples of c2 although it can be larger.
   * 
   * @param c1
   * @param c2
   * @return
   */
  public static boolean isSuper(BaseComb c1, BaseComb c2) {
    for (int e : c1) {
      if (!c2.contains(e)) {
        return false;
      }
    }

    return true;
  }

  public static boolean combsMatch(BaseComb c1, BaseComb c2) {
    if (c1.size() != c2.size()) {
      return false;
    }

    return isSuper(c1, c2);
  }

  public static String toString(Collection<Integer> values) {
    // We add one so that the indices are more familiar to users who
    // count starting at one
    return Join.on(", ").values(MathUtils.add(values, 1)).toString();
  }
}
