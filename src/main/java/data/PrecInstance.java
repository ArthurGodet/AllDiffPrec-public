/*
@author Arthur Godet <arth.godet@gmail.com>
@since 19/01/2021
*/

package data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import alldifferentprec.PropAllDiffPrec;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import main.ConfigurationPrec;
import main.PrecModel;
import org.chocosolver.solver.exception.ContradictionException;

public class PrecInstance {
    private final String name;
    private final int size;
    private final int[][] ancestors;
    private final int[][] descendants;
    private final int[][] domains;

    @JsonCreator
    public PrecInstance(
        @JsonProperty("name") String name,
        @JsonProperty("ancestors") int[][] ancestors,
        @JsonProperty("descendants") int[][] descendants,
        @JsonProperty("domains") int[][] domains
    ) {
        this.size = ancestors.length;
        this.name = name;
        this.ancestors = ancestors;
        this.descendants = descendants;
        this.domains = domains;
    }

    public String getName() {
        return name;
    }

    public int[][] getDomains() {
        return domains;
    }

    /**
     * Returns the ancestors matrix: ancestors[i][k] = j means that j is an ancestor of i.
     *
     * @return the ancestors matrix
     */
    public int[][] getAncestors() {
        return ancestors;
    }

    /**
     * Returns the descendants matrix: descendants[i][k] = j means that j is a descendant of i.
     *
     * @return the descendants matrix
     */
    public int[][] getDescendants() {
        return descendants;
    }

    public int getSize() {
        return size;
    }

    ////////////////////////////////////////////////////////////////////////
    ///////////////////////// Benchmark Generation /////////////////////////
    ////////////////////////////////////////////////////////////////////////

    public static long SEED = 0;
    public static final Random RND = new Random(SEED);
    private static final TIntArrayList list = new TIntArrayList();
    private static final TIntArrayList tmp = new TIntArrayList();
    private static final TIntHashSet set = new TIntHashSet();

    /**
     * Generates an instance of the AllDiffPrec constraint with the given name and of the given size.
     * The density parameter gives the domains' density of each variable, while probPrec indicates the probability
     * such that i will precede j for each i < j.
     *
     * @param size the number of variables
     * @param density the domains' density
     * @param probPrec the probability of precedence
     * @param name the name of the instance
     * @return the instance
     */
    public static PrecInstance generateDensityInstance(int size, int density, int probPrec, String name) {
        boolean hasSolution = false;
        int nbFails = 0;
        int[][] ancestors, descendants, domains;
        int[] mins = new int[size];
        int[] maxs = new int[size];

        do {
            ancestors = new int[size][];
            for (int j = 0; j < size; j++) {
                set.clear();
                for (int i = j - 1; i >= 0; i--) {
                    if(RND.nextInt(100) <= probPrec) {
                        set.add(i);
                        set.addAll(ancestors[i]);
                    }
                }
                list.clear();
                list.addAll(set);
                list.sort();
                ancestors[j] = list.toArray();
            }
            descendants = new int[size][];
            for (int i = 0; i < size; i++) {
                list.clear();
                for (int j = 0; j < size; j++) {
                    if (Factory.contains(ancestors[j], i)) {
                        list.add(j);
                    }
                }
                list.sort();
                descendants[i] = list.toArray();
            }
            domains = new int[size][];
            int[] topologicalTraversal = PropAllDiffPrec.buildTopologicalTraversal(
                PropAllDiffPrec.buildPrecGraph(
                    PropAllDiffPrec.buildPrecedence(ancestors, descendants, true)
                )
            );
            do {
                int maxDomain = size + (size/10)*RND.nextInt(size);
                while(maxDomain == size) {
                    maxDomain = size + (size/10)*RND.nextInt(size);
                }
                for(int k = 0; k < size; k++) {
                    maxs[k] = size+RND.nextInt(maxDomain-size);
                    mins[k] = RND.nextInt(maxs[k]/(size >= 10 ? 10 : 2));
                }
                Arrays.sort(mins);
                Arrays.sort(maxs);
                for (int i = 0; i < size; i++) {
                    list.clear();
                    tmp.clear();
                    int min = mins[i];
                    int max = maxs[i];
                    for(int v = min; v <= max; v++) {
                        tmp.add(v);
                    }
                    tmp.shuffle(RND);
                    for(int k = 0; k < density * tmp.size() / 100; k++) {
                        list.add(tmp.getQuick(k));
                    }
                    list.sort();
                    domains[topologicalTraversal[i]] = list.toArray();
                }
                PrecInstance inst = new PrecInstance(name, ancestors, descendants, domains);
                PrecModel precModel = new PrecModel(inst, ConfigurationPrec.DECOMPOSITION);
                try {
                    precModel.getModel().getSolver().propagate();
                    hasSolution = true;
                } catch(ContradictionException ignored) {
                    nbFails++;
                }
            } while(!hasSolution && nbFails % 10000 > 0);
        } while(!hasSolution);
        TIntArrayList indexes = new TIntArrayList();
        for(int i = 0; i < size; i++) {
            indexes.add(i);
        }
        indexes.shuffle(RND);
        TIntArrayList tmp2 = new TIntArrayList();
        int[][] ancestorsRand = new int[size][];
        int[][] descendantsRand = new int[size][];
        int[][] domainsRand = new int[size][];
        for(int i = 0; i < size; i++) {
            tmp2.clear();
            for(int j = 0; j < ancestors[i].length; j++) {
                tmp2.add(indexes.getQuick(ancestors[i][j]));
            }
            tmp2.sort();
            ancestorsRand[indexes.getQuick(i)] = tmp2.toArray();
            tmp2.clear();
            for(int j = 0; j < descendants[i].length; j++) {
                tmp2.add(indexes.getQuick(descendants[i][j]));
            }
            tmp2.sort();
            descendantsRand[indexes.getQuick(i)] = tmp2.toArray();
            domainsRand[indexes.getQuick(i)] = domains[i];
        }
        return new PrecInstance(name, ancestorsRand, descendantsRand, domainsRand);
    }

    public static void generateBenchmarkDensity(int size) {
        int densityInc = 5;
        int nbInstancesPerSize = 100;
        int[] probs = new int[]{20, 40, 60, 80};
        for(int density = 100; density >= 10; density -= densityInc) {
            for (int k = 0; k < nbInstancesPerSize; k++) {
                RND.setSeed(SEED + k);
                String name = "Prec_" + size + "_" + density + "_" + (k + 1);
                String path = "data/"+size+"/"+name+".json";
                if (!Files.exists(Paths.get(path))) {
                    System.out.println(name);
                    int prob = probs[k/(nbInstancesPerSize/probs.length)];
                    Factory.toFile(
                        path,
                        generateDensityInstance(size, density, prob, name)
                    );
                } else {
                    System.out.println("Already exists : " + name);
                }
            }
        }
    }

    public static void main(String[] args) {
        generateBenchmarkDensity(20);
        generateBenchmarkDensity(50);
        generateBenchmarkDensity(100);
        generateBenchmarkDensity(150);
        generateBenchmarkDensity(200);
    }
}
