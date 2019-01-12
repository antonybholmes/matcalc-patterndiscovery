package edu.columbia.rdf.matcalc.toolbox.patterndiscovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A comb is an ordered set of experiment ids. Combs with the same indices are
 * considered equal and have the same hash and equals will return true, even if
 * they are are different objects.
 * 
 * @author Antony Holmes
 *
 */
public class Comb extends BaseComb {

  public Comb(Collection<Integer> values) {
    super(values);
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
}
