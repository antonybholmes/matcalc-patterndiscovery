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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache combs since they will frequently be the same for multiple genes.
 *
 * @author Antony Holmes
 *
 */
public class CombService {

  /**
   * The Class SettingsServiceLoader.
   */
  private static class CombServiceLoader {
    private static final CombService INSTANCE = new CombService();
  }

  /**
   * Gets the single instance of SettingsService.
   *
   * @return single instance of SettingsService
   */
  public static CombService getInstance() {
    return CombServiceLoader.INSTANCE;
  }

  private Map<String, Comb> mCombMap = new HashMap<String, Comb>();

  /**
   * Create a comb and cache it so that we minimize comb instances.
   * 
   * @param experiments
   * @return
   */
  public Comb createComb(Collection<Integer> experiments) {
    String s = Comb.toString(experiments);

    if (!mCombMap.containsKey(s)) {
      mCombMap.put(s, new Comb(experiments));
    }

    return mCombMap.get(s);
  }

  /**
   * Returns the intersection of two combs
   * 
   * @param source
   * @param target
   * @return
   */
  public Comb intersect(Comb source, Comb target) {
    List<Integer> experiments = new ArrayList<Integer>(source.size());

    for (int c1 : source) {
      if (target.contains(c1)) {
        experiments.add(c1);
      }
    }

    return createComb(experiments);
  }
}
