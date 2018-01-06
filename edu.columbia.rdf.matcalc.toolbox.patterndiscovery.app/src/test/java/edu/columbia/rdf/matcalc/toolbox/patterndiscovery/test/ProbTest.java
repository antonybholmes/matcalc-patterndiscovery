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
package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.test;

import org.junit.Test;

import edu.columbia.rdf.matcalc.toolbox.patterndiscovery.Pattern;

public class ProbTest {

  @Test
  public void probTest() {

    System.out.println("Path test");

    int experiments = 5;
    int genes = 5;
    int totalExp = 10;
    int totalGenes = 10;
    double delta = 0.05;

    System.err.println("p " + Pattern.p(experiments, genes, totalExp, totalGenes, delta));
  }
}
