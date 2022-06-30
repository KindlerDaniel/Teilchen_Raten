import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
A Universe is derived from a number of decisions.
Decision could be: "Observation o1 was particle p1".
The probability that these decisions correspond to the reality on the Board
is the probability that the Universe exists (prob_existence).
A new Distribution is created when a new particle has been added,
so there is one Distribution for each particle.
Universe can compute a single combined probability distribution from its Distributions.
 */
public class Universe implements Cloneable, Comparable<Universe> {
    static final double TOL = 1e15;
    private static int height;
    private static int width;
    double probExistence;
    private List<Distribution> distributions;
    private double[][] totalDistribution;
    private boolean distributionChanged;
    private double information;
    private boolean informationChanged;


    public Universe(final int height, final int width, final double probability) {
        this.probExistence = probability;
        this.height = height;
        this.width = width;
        distributions = new LinkedList<>();
        totalDistribution = new double[height][width];
        distributionChanged = false;
        information = 0;
        informationChanged = false;
    }

    @Override
    public Universe clone() {
        Universe clone = new Universe(height, width, probExistence);
        clone.distributions = distributions.stream().map(f -> f.clone()).collect(Collectors.toList());
        clone.distributions.forEach(f -> f.setUniverse(clone));
        clone.totalDistribution = Distribution.copyDistribution(totalDistribution);
        clone.distributionChanged = distributionChanged;
        clone.information = information;
        clone.informationChanged = informationChanged;
        return clone;
    }


    // get probability at position
    public double probability(Pair<Integer, Integer> position) {
        updateTotalDistribution();
        return totalDistribution[position.first][position.second];
    }

    // Compute total distribution if something changed.
    private void updateTotalDistribution() {
        if (distributionChanged) {
            Board.all_positions(height, width).stream().forEach(pos -> {
                double probability = 0;
                for (Distribution distribution : distributions) {
                    probability += (1 - probability) * distribution.probability(pos);
                }
                totalDistribution[pos.first][pos.second] = probability;
            });
            distributionChanged = false;
        }
    }


    @Override
    public int hashCode() {
        double[] numbers = new double[height * width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                numbers[i * width + j] = Math.round(probability(new Pair<>(i, j)) * TOL) / TOL;
            }
        }
        return Arrays.hashCode(numbers);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Universe)) {
            return false;
        }
        Universe u = (Universe) o;
        // Are the total distributions the same up to a tolerance tol?
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double thisProb = Math.round(probability(new Pair<>(i, j)) * TOL) / TOL;
                double otherProb = Math.round(u.probability((new Pair<>(i, j))) * TOL) / TOL;
                if (thisProb != otherProb) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int compareTo(final Universe universe) {
        return Double.compare(universe.information(), this.information());
    }


    // The Bhattacharyya distance
    public double similarity(final Universe universe) {
        return Board.all_positions(height, width).stream().
                mapToDouble(pos -> Math.sqrt(probability(pos) * universe.probability(pos))).sum();
    }

    // Add a new distribution that is homogeneous within the given area.
    public void addDistribution(final List<Pair<Integer, Integer>> area) {
        distributions.add(new Distribution(height, width, area, this));
        distributionChanged = true;
        informationChanged = true;
    }

    // Observe a CERTAIN particle (Distribution) at position.
    public void observeDistribution(final Pair<Integer, Integer> position, final int distributionIndex) {
        distributions.get(distributionIndex).concentrateAt(position);
    }

    // Observe absence at position.
    public boolean observeAbsence(final Pair<Integer, Integer> position, final Set<Integer> except) {
        for (int i = 0; i < distributions.size(); i++) {
            Distribution distribution = distributions.get(i);
            if (except.contains(i)) {
                continue;
            }
            distribution.normalize();
            if (distribution.probability(position) >= 1) {
                return false;
            }
            distribution.vanishFrom(position);
        }
        distributionChanged = true;
        informationChanged = true;
        return true;
    }

    // Delete a CERTAIN particle (Distribution).
    public void deleteDistribution(final int distribution) {
        distributions.remove(distribution);
        distributionChanged = true;
        informationChanged = true;
    }

    // Diffuses all distributions to compute probabilities after one step.
    public void simulateTimeStep(final Board board) {
        for (Distribution distribution : distributions) {
            distribution.diffuse(board);
        }
        distributionChanged = true;
        informationChanged = true;
    }

    // Which particle has been deleted? Create one (sub) Universe for each possibility.
    public List<Universe> splitAfterDeletion(final List<Pair<Integer, Integer>> area) {
        // For each Distribution get the probability that it is within the area
        Map<Distribution, Double> areaProbs = new HashMap<>(distributions.size());
        for (Distribution distribution : distributions) {
            areaProbs.put(distribution, area.stream().mapToDouble(pos -> distribution.probability(pos)).sum());
        }
        double summedProb = areaProbs.values().stream().reduce(0.0, Double::sum);
        // Create one (sub) Universe for each distribution.
        List<Universe> subUniverses = new ArrayList<>(distributions.size());
        for (int f = 0; f < distributions.size(); f++) {
            double birthProbability = areaProbs.get(distributions.get(f)) / summedProb;
            if (birthProbability == 0) {
                continue;
            }
            Universe subUniverse = this.clone();
            subUniverse.probExistence *= birthProbability;
            subUniverse.deleteDistribution(f);
            subUniverses.add(subUniverse);
        }
        return subUniverses;
    }

    // Which particles have been observed? Create one (sub) Universe for each possibility.
    // But smart! The method recognizes duplicates and summarizes them directly.
    public List<Universe> splitAfterObservation(final Pair<Integer, Integer> position, final int number) {
        distributions.stream().forEach(f -> f.normalize());
        // MultiSet can contain duplicates according to the equals() method
        Map<Multiset<Distribution>, Universe> possibleUniverses = new HashMap<>();
        // For each possible combination of observed particles (distributions)
        for (Set<Integer> combination : possibleSplittingSets(position, number)) {
            // transform combination into MultiSet of distributions.
            Multiset<Distribution> splittingSet = HashMultiset.create();
            combination.stream().forEach(i -> splittingSet.add(distributions.get(i)));
            // Has a Universe already been made by splitting at distributions that equal these?
            if (!possibleUniverses.containsKey(splittingSet)) {
                // No, this kind of Universe is new. Create a daughter with probability 0.
                Universe daughter = daughterUniverse(position, combination);
                assert (daughter != null);
                possibleUniverses.put(splittingSet, daughter);
            }
            // Compute the birth probability and add it to representative.
            double prob = probExistence * birthProbability(position, combination);
            possibleUniverses.get(splittingSet).probExistence += prob;
        }
        return new ArrayList<>(possibleUniverses.values());
    }


    // Which particle combinations are possible?
    private Set<Set<Integer>> possibleSplittingSets(final Pair<Integer, Integer> position, final int number) {
        Set<Integer> positives = positiveDistributions(position);
        Set<Integer> certains = certainDistributions(position);
        Set<Set<Integer>> combinations = generateCombinations(number, distributions.size()).stream().
                filter(combination -> (combination.containsAll(certains) && positives.containsAll(combination)))
                .collect(Collectors.toSet());
        return combinations;
    }


    // All distributions that have nonzero probability at this position
    private Set<Integer> positiveDistributions(final Pair<Integer, Integer> position) {
        return IntStream.range(0, distributions.size()).filter(i -> distributions.get(i).probability(position) > 0)
                .boxed().collect(Collectors.toSet());

    }

    // All distributions that have probability 1 at this ppsition
    private Set<Integer> certainDistributions(final Pair<Integer, Integer> position) {
        return IntStream.range(0, distributions.size()).filter(i -> distributions.get(i).probability(position) >= 1)
                .boxed().collect(Collectors.toSet());
    }

    // The daughter Universe that follows from the observation of CERTAIN particles (distributions) at position.
    private Universe daughterUniverse(final Pair<Integer, Integer> position, final Set<Integer> splittingSet) {
        Universe daughter = this.clone();
        daughter.probExistence = 0;
        splittingSet.stream().forEach(i -> daughter.observeDistribution(position, i));
        boolean possible = daughter.observeAbsence(position, splittingSet);
        if (!possible) {
            return null;
        }
        return daughter;
    }

    // Probability n observed particles are the given particles (distributions)
    private double birthProbability(final Pair<Integer, Integer> position, final Set<Integer> distributions) {
        double birthProb = 1;
        for (int i = 0; i < this.distributions.size(); i++) {
            double distributionsProb = this.distributions.get(i).probability(position);
            birthProb *= (distributions.contains(i) ? distributionsProb : (1 - distributionsProb));
            if (birthProb == 0) {
                return 0;
            }
        }
        return birthProb;

    }

    // Merge the distributions of another Universe pairwise into own distributions.
    public void mergeIn(final Universe universe) {
        List<Distribution> partnersLeft = new LinkedList<>(distributions);
        for (Distribution distribution : universe.distributions) {
            Distribution mostSimilar = mostSimilarDistribution(partnersLeft, distribution);
            double proportion = universe.probExistence / probExistence;
            mostSimilar.mergeIn(distribution, proportion);
            partnersLeft.remove(mostSimilar);
        }
        probExistence += universe.probExistence;
    }


    // Finds among the list of distributions the one most similar to the distribution.
    private Distribution mostSimilarDistribution(final List<Distribution> compareWith, final Distribution distribution) {
        assert (!compareWith.isEmpty());
        double highestSimilarity = -1;
        Distribution mostSimilar = null;
        for (Distribution other : compareWith) {
            double similarity = distribution.similarity(other);
            assert (Double.isFinite(similarity));
            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
                mostSimilar = other;
            }
        }
        assert (mostSimilar != null);
        return mostSimilar;
    }


    // A Universe's information depends on the information of its distributions
    public double information() {
        if (informationChanged) {
            information = distributions.stream().mapToDouble(distribution -> distribution.information()).sum();
            information *= probExistence;
            informationChanged = false;
        }
        return information;
    }

    // generate combinations. Select n out of total number.
    private Set<Set<Integer>> generateCombinations(final int selectN, final int fromTotal) {
        Set<Set<Integer>> combinations = new HashSet<>();
        combinations.add(new TreeSet<>());
        for (int times = 0; times < selectN; times++) {
            Set<Set<Integer>> nextStep = new HashSet<>();
            for (Set<Integer> existing : combinations) {
                for (int i = 0; i < fromTotal; i++) {
                    Set<Integer> extended = new TreeSet<>(existing);
                    if (extended.add(i)) {
                        nextStep.add(extended);
                    }
                }
            }
            combinations = nextStep;
        }
        return combinations;
    }
}
