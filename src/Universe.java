import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.HashMultiset;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A Universe follows from a number of decisions.
 * Decision could be: "Observation o1 was particle p1".
 * The probability that these decisions are correct is
 * the probability that the Universe exists (prob_existence).
 * A new Distribution is added whenever a new particle has been added,
 * so there is one Distribution for each particle. Universe computes
 * a combined probability distribution from these Distributions.
 */

public class Universe implements Cloneable, Comparable<Universe> {
    /**
     * Round probability by this constant before checking equality
     * in hashCode() and equals()
     */
    static final double TOL = 1e15;
    /**
     * Height of sensed board (and distributions).
     */
    private static int height;
    /**
     * Weight of sensed board (and distributions).
     */
    private static int width;
    /**
     * Probability that this Universe exists.
     * Probability that the underlying decisions have been correct.
     */
    double probExistence;
    /**
     * One Distribution for each particle.
     */
    private List<Distribution> distributions;
    /**
     * The combined probability.
     * Saved in matrix to avoid re-computation.
     */
    private double[][] totalDistribution;
    /**
     * Has totalDistribution changed after it has been computed last time?
     */
    private boolean totalDistributionChanged;
    /**
     * information content of Universe.
     */
    private double information;
    /**
     * Has information changed after it has been computed last time?
     */
    private boolean informationChanged;

    /**
     * @param height height of sensed board (and distributions).
     * @param width width of sensed board (and distributions).
     * @param probability probability that universe exists.
     */
    public Universe(final int height, final int width, final double probability) {
        this.probExistence = probability;
        this.height = height;
        this.width = width;
        distributions = new LinkedList<>();
        totalDistribution = new double[height][width];
        totalDistributionChanged = false;
        information = 0;
        informationChanged = false;
    }

    @Override
    public Universe clone() {
        Universe clone = new Universe(height, width, probExistence);
        clone.distributions = distributions.stream().map(f -> f.clone()).collect(Collectors.toList());
        clone.distributions.forEach(f -> f.setUniverse(clone));
        clone.totalDistribution = Distribution.copyDistribution(totalDistribution);
        clone.totalDistributionChanged = totalDistributionChanged;
        clone.information = information;
        clone.informationChanged = informationChanged;
        return clone;
    }

    /**
     * compute combined probability at position.
     * @param position position on board.
     * @return combined probability at position.
     */
    public double probability(final Position position) {
        updateTotalDistribution();
        return totalDistribution[position.first][position.second];
    }

    /**
     * Compute and update the combined probability distribution.
     */
    private void updateTotalDistribution() {
        if (totalDistributionChanged) {
            Board.allPositions(height, width).stream().forEach(pos -> {
                double probability = 0;
                for (Distribution distribution : distributions) {
                    probability += (1 - probability) * distribution.probability(pos);
                }
                totalDistribution[pos.first][pos.second] = probability;
            });
            totalDistributionChanged = false;
        }
    }


    @Override
    public int hashCode() {
        double[] numbers = new double[height * width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                numbers[i * width + j] = Math.round(probability(new Position(i, j)) * TOL) / TOL;
            }
        }
        return Arrays.hashCode(numbers);
    }

    /**
     * @param o the object to be checked for equality.
     * @return true iff other Universe's combined distribution is the same.
     */
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
                double thisProb = Math.round(probability(new Position(i, j)) * TOL) / TOL;
                double otherProb = Math.round(u.probability((new Position(i, j))) * TOL) / TOL;
                if (thisProb != otherProb) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param universe the object to be compared.
     * @return positive value if this Universe has higher information content.
     */
    @Override
    public int compareTo(final Universe universe) {
        return Double.compare(universe.information(), this.information());
    }


    /**
     * Compute similarity according to Bhattacharyya distance.
     * @param universe another universe.
     * @return Bhattacharyya distance between the universes distributions.
     */
    public double similarity(final Universe universe) {
        return Board.allPositions(height, width).stream().
                mapToDouble(pos -> Math.sqrt(probability(pos) * universe.probability(pos))).sum();
    }

    /**
     * Add a new Distribution to distributions.
     * @param area Area where new Distribution is present.
     */
    public void addDistribution(final List<Position> area) {
        distributions.add(new Distribution(height, width, area, this));
        totalDistributionChanged = true;
        informationChanged = true;
    }

    /**
     * Observe a CERTAIN particle (Distributions) at given position.
     * @param position Position where particles have been observed.
     * @param distributionIndex Number of observed particles.
     */
    public void observeDistribution(final Position position, final int distributionIndex) {
        distributions.get(distributionIndex).concentrateAt(position);
    }

    /**
     * Observe absence of CERTAIN particles (Distributions) at given position.
     * @param position Position where absence has been observed.
     * @param except The Particles (Distributions) that may not be absent.
     * @return
     */
    public boolean observeAbsence(final Position position, final Set<Integer> except) {
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
        totalDistributionChanged = true;
        informationChanged = true;
        return true;
    }

    /**
     * Delete a CERTAIN particle (Distribution).
     * @param distribution The distribution to be deleted.
     */
    public void deleteDistribution(final int distribution) {
        distributions.remove(distribution);
        totalDistributionChanged = true;
        informationChanged = true;
    }

    /**
     * Diffuse all distributions.
     * @param board Board on which diffusion takes place.
     */
    public void simulateTimeStep(final Board board) {
        for (Distribution distribution : distributions) {
            distribution.diffuse(board);
        }
        totalDistributionChanged = true;
        informationChanged = true;
    }

    /**
     * Which particle has been deleted?
     * Create one (daughter) Universe for each possibility.
     * @param area Area wherein particle has been deleted.
     * @return One daughter Universe for each possibility.
     */
    public List<Universe> splitAfterDeletion(final List<Position> area) {
        // For each Distribution get the probability that it is within the area.
        Map<Distribution, Double> areaProbs = new HashMap<>(distributions.size());
        for (Distribution distribution : distributions) {
            areaProbs.put(distribution, area.stream().mapToDouble(pos -> distribution.probability(pos)).sum());
        }
        double summedProb = areaProbs.values().stream().reduce(0.0, Double::sum);
        // Create one (sub) Universe for each distribution.
        List<Universe> daughterUniverses = new ArrayList<>(distributions.size());
        for (int f = 0; f < distributions.size(); f++) {
            double birthProbability = areaProbs.get(distributions.get(f)) / summedProb;
            if (birthProbability == 0) {
                continue;
            }
            Universe subUniverse = this.clone();
            subUniverse.probExistence *= birthProbability;
            subUniverse.deleteDistribution(f);
            daughterUniverses.add(subUniverse);
        }
        return daughterUniverses;
    }

    /**
     * Which particles have been observed?
     * Create one (daughter) Universe for each possibility.
     * The method recognizes structural duplicates and summarizes them directly.
     * @param position Position where particles have been observed.
     * @param number Number of observed particles at position.
     * @return One daughter Universe for each possibility.
     */
    public List<Universe> splitAfterObservation(final Position position, final int number) {
        distributions.stream().forEach(f -> f.normalize());
        // MultiSet can contain duplicates according to the equals() method.
        Map<Multiset<Distribution>, Universe> daughterUniverses = new HashMap<>();
        // For each possible combination of observed particles (distributions).
        for (Set<Integer> combination : possibleSplittingSets(position, number)) {
            // transform combination into MultiSet of distributions.
            Multiset<Distribution> selectedDistributions = HashMultiset.create();
            combination.stream().forEach(i -> selectedDistributions.add(distributions.get(i)));
            // Has there already been made a Universe like this?
            // like this = by splitting at distributions equal to the selected.
            if (!daughterUniverses.containsKey(selectedDistributions)) {
                // No, a Universe like this is new! Create a daughter.
                Universe daughter = daughterUniverse(position, combination);
                assert (daughter != null);
                daughterUniverses.put(selectedDistributions, daughter);
            }
            // Compute birth probability and add it to corresponding daughter.
            double prob = probExistence * birthProbability(position, combination);
            daughterUniverses.get(selectedDistributions).probExistence += prob;
        }
        return new ArrayList<>(daughterUniverses.values());
    }

    /**
     * Compute all possible particle (Distribution) combinations.
     * @param position Position on the board.
     * @param number Number of particles to combine.
     * @return
     */
    private Set<Set<Integer>> possibleSplittingSets(final Position position, final int number) {
        Set<Integer> positives = positiveDistributions(position);
        Set<Integer> certains = certainDistributions(position);
        Set<Set<Integer>> combinations = generateCombinations(number, distributions.size()).stream().
                filter(combination -> (combination.containsAll(certains) && positives.containsAll(combination)))
                .collect(Collectors.toSet());
        return combinations;
    }

    /**
     * @param position Position on the board.
     * @return Distributions that have nonzero probability at position.
     */
    private Set<Integer> positiveDistributions(final Position position) {
        return IntStream.range(0, distributions.size()).filter(i -> distributions.get(i).probability(position) > 0)
                .boxed().collect(Collectors.toSet());

    }

    /**
     * @param position Position on the board.
     * @return Distributions that have probability 1 at position.
     */
    private Set<Integer> certainDistributions(final Position position) {
        return IntStream.range(0, distributions.size()).filter(i -> distributions.get(i).probability(position) >= 1)
                .boxed().collect(Collectors.toSet());
    }

    /**
     * Create the daughter Universe that follows from decision.
     * Decision = WHICH particles (distributions) have been observed?
     * @param position Position of observation.
     * @param splittingSet Particles (distributions) that have been observed.
     * @return Daughter Universe that follows from decision.
     */
    private Universe daughterUniverse(final Position position, final Set<Integer> splittingSet) {
        Universe daughter = this.clone();
        daughter.probExistence = 0;
        splittingSet.stream().forEach(i -> daughter.observeDistribution(position, i));
        boolean possible = daughter.observeAbsence(position, splittingSet);
        if (!possible) {
            return null;
        }
        return daughter;
    }

    /**
     * @param position Position on the board.
     * @param distributions Particles (distributions) observed at position.
     * @return Probability that observed particles (distributions) are those.
     */
    private double birthProbability(final Position position, final Set<Integer> distributions) {
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

    /**
     * Merge distributions of another Universe into own distributions.
     * @param universe Universe to be merged with.
     */
    public void mergeIn(final Universe universe) {
        List<Distribution> partnersLeft = new LinkedList<>(distributions);
        for (Distribution distribution : universe.distributions) {
            Distribution mostSimilar = mostSimilarDistribution(distribution, partnersLeft);
            double proportion = universe.probExistence / probExistence;
            mostSimilar.mergeIn(distribution, proportion);
            partnersLeft.remove(mostSimilar);
        }
        probExistence += universe.probExistence;
    }

    /**
     * @param distribution Single distribution.
     * @param compareWith Distributions to compare with single distribution.
     * @return The distribution among compareWith most similar to distribution.
     */
    private Distribution mostSimilarDistribution(final Distribution distribution, final List<Distribution> compareWith) {
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


    /**
     * Universe's information content is computed as
     * distributions information contents * Universe's probability of existence.
     * @return Universe's information content.
     */
    public double information() {
        if (informationChanged) {
            information = distributions.stream().mapToDouble(distribution -> distribution.information()).sum();
            information *= probExistence;
            informationChanged = false;
        }
        return information;
    }

    /**
     * Generate all combinations of selectN elements.
     * @param selectN Size of generated sets.
     * @param fromTotal Number of elements to choose from.
     * @return
     */
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
