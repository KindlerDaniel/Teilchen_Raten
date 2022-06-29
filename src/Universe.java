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
    static final double tol = 1e15;
    private static int height;
    private static int width;
    double prob_existence;
    private List<Distribution> distributions;
    private double[][] total_distribution;
    private boolean distribution_changed;
    private double information;
    private boolean information_changed;


    public Universe(int height, int width, double probability){
        this.prob_existence = probability;
        this.height = height;
        this.width = width;
        distributions = new LinkedList<>();
        total_distribution = new double[height][width];
        distribution_changed = false;
        information = 0;
        information_changed = false;
    }

    @Override
    public Universe clone(){
        Universe clone = new Universe(height, width, prob_existence);
        clone.distributions = distributions.stream().map(f -> f.clone()).collect(Collectors.toList());
        clone.distributions.forEach(f -> f.setUniverse(clone));
        clone.total_distribution = Distribution.copy_distribution(total_distribution);
        clone.distribution_changed = distribution_changed;
        clone.information = information;
        clone.information_changed = information_changed;
        return clone;
    }


    // get probability at position
    public double probability(Pair<Integer, Integer> position){
        update_total_distribution();
        return total_distribution[position.first][position.second];
    }

    // Compute total distribution if something changed.
    private void update_total_distribution(){
        if (distribution_changed) {
            Board.all_positions(height, width).stream().forEach(pos ->
            {
                double probability = 0;
                for (Distribution distribution : distributions) {
                    probability += (1 - probability) * distribution.probability(pos);
                }
                total_distribution[pos.first][pos.second] = probability;
            });
            distribution_changed = false;
        }
    }


    @Override
    public int hashCode(){
        double[] numbers = new double[height * width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                numbers[i * width + j] = Math.round(probability(new Pair<>(i, j)) * tol) / tol;
            }
        }
        return Arrays.hashCode(numbers);
    }

    @Override
    public boolean equals(Object o){
        if (o == this) return true;
        if (!(o instanceof Universe)) return false;
        Universe u = (Universe) o;
        // Are the total distributions the same up to a tolerance tol?
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double this_prob = Math.round(probability(new Pair<>(i, j)) * tol) / tol;
                double other_prob = Math.round(u.probability((new Pair<>(i, j))) * tol) / tol;
                if (this_prob != other_prob){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int compareTo(Universe universe) {
        return Double.compare(universe.information(), this.information());
    }


    // The Bhattacharyya distance
    public double similarity(Universe universe){
        return Board.all_positions(height, width).stream().
                mapToDouble(pos -> Math.sqrt(probability(pos) * universe.probability(pos))).sum();
    }

    // Add a new distribution that is homogeneous within the given area.
    public void add_distribution(List<Pair<Integer, Integer>> area){
        distributions.add(new Distribution(height, width, area, this));
        distribution_changed = true;
        information_changed = true;
    }

    // Observe a CERTAIN particle (Distribution) at position.
    public void observe_distribution(Pair<Integer, Integer> position, int distribution_index){
        distributions.get(distribution_index).concentrate_at(position);
    }

    // Observe absence at position.
    public boolean observe_absence(Pair<Integer, Integer> position, Set<Integer> except){
        for (int i = 0; i < distributions.size(); i++){
            Distribution distribution = distributions.get(i);
            if (except.contains(i)){ continue;}
            distribution.normalize();
            if (distribution.probability(position) >= 1){return false;}
            distribution.vanish_from(position);
        }
        distribution_changed = true;
        information_changed = true;
        return true;
    }

    // Delete a CERTAIN particle (Distribution).
    public void delete_distribution(int distribution){
        distributions.remove(distribution);
        distribution_changed = true;
        information_changed = true;
    }

    // Diffuses all distributions to compute probabilities after one step.
    public void simulate_timeStep(Board board){
        for (Distribution distribution : distributions){
            distribution.diffuse(board);
        }
        distribution_changed = true;
        information_changed = true;
    }

    // Which particle has been deleted? Create one (sub) Universe for each possibility.
    public List<Universe> split_after_deletion(List<Pair<Integer, Integer>> area){
        // For each Distribution get the probability that it is within the area
        Map<Distribution, Double> area_probs = new HashMap<>(distributions.size());
        for (Distribution distribution : distributions){
            area_probs.put(distribution, area.stream().mapToDouble(pos -> distribution.probability(pos)).sum());
        }
        double summed_prob = area_probs.values().stream().reduce(0.0, Double::sum);
        // Create one (sub) Universe for each distribution.
        List<Universe> sub_universes = new ArrayList<>(distributions.size());
        for (int f = 0; f < distributions.size(); f++){
            double birth_probability = area_probs.get(distributions.get(f)) / summed_prob;
            if (birth_probability == 0){continue;}
            Universe sub_universe = this.clone();
            sub_universe.prob_existence *= birth_probability;
            sub_universe.delete_distribution(f);
            sub_universes.add(sub_universe);
        }
        return sub_universes;
    }

    // Which particles have been observed? Create one (sub) Universe for each possibility.
    // But smart! The method recognizes duplicates and summarizes them directly.
    public List<Universe> split_after_observation(Pair<Integer, Integer> position, int number){
        distributions.stream().forEach(f -> f.normalize());
        // MultiSet can contain duplicates according to the equals() method
        Map<Multiset<Distribution>, Universe> possible_universes = new HashMap<>();
        // For each possible combination of observed particles (distributions)
        for (Set<Integer> combination : possible_splitting_sets(position, number)){
            // transform combination into MultiSet of distributions.
            Multiset<Distribution> splitting_set = HashMultiset.create();
            combination.stream().forEach(i -> splitting_set.add(distributions.get(i)));
            // Has a Universe already been made by splitting at distributions that equal these?
            if (!possible_universes.containsKey(splitting_set)){
                // No, this kind of Universe is new. Create a daughter with probability 0.
                Universe daughter = daughter_universe(position, combination);
                assert(daughter != null);
                possible_universes.put(splitting_set, daughter);
            }
            // Compute the birth probability and add it to representative.
            double prob = prob_existence * birth_probability(position, combination);
            possible_universes.get(splitting_set).prob_existence += prob;
        }
        return new ArrayList<>(possible_universes.values());
    }


    // Which particle combinations are possible?
    private Set<Set<Integer>> possible_splitting_sets(Pair<Integer, Integer> position, int number){
        Set<Integer> positives = positive_distributions(position);
        Set<Integer> certains = certain_distributions(position);
        Set<Set<Integer>> combinations = generate_combinations(number, distributions.size()).stream().
                filter(combination -> (combination.containsAll(certains) && positives.containsAll(combination)))
                .collect(Collectors.toSet());
        return combinations;
    }


    // All distributions that have nonzero probability at this position
    private Set<Integer> positive_distributions(Pair<Integer, Integer> position){
        return IntStream.range(0, distributions.size()).filter(i -> distributions.get(i).probability(position) > 0)
                .boxed().collect(Collectors.toSet());

    }

    // All distributions that have probability 1 at this ppsition
    private Set<Integer> certain_distributions(Pair<Integer, Integer> position){
        return IntStream.range(0, distributions.size()).filter(i -> distributions.get(i).probability(position) >= 1)
                .boxed().collect(Collectors.toSet());
    }

    // The daughter Universe that follows from the observation of CERTAIN particles (distributions) at position.
    private Universe daughter_universe(Pair<Integer, Integer> position, Set<Integer> splitting_set){
        Universe daughter = this.clone();
        daughter.prob_existence = 0;
        splitting_set.stream().forEach(i -> daughter.observe_distribution(position, i));
        boolean possible = daughter.observe_absence(position, splitting_set);
        if (!possible){ return null;}
        return daughter;
    }

    // Probability n observed particles are the given particles (distributions)
    private double birth_probability(Pair<Integer, Integer> position, Set<Integer> distributions){
        double birth_prob = 1;
        for (int i = 0; i < this.distributions.size(); i++){
            double distributions_prob = this.distributions.get(i).probability(position);
            birth_prob *= (distributions.contains(i) ? distributions_prob : (1 - distributions_prob));
            if (birth_prob == 0){ return 0;}
        }
        return birth_prob;

    }

    // Merge the distributions of another Universe pairwise into own distributions.
    public void merge_in(Universe universe){
        List<Distribution> partners_left = new LinkedList<>(distributions);
        for (Distribution distribution : universe.distributions){
            Distribution most_similar = most_similar_distribution(partners_left, distribution);
            double proportion = universe.prob_existence / prob_existence;
            most_similar.merge_in(distribution, proportion);
            partners_left.remove(most_similar);
        }
        prob_existence += universe.prob_existence;
    }


    // Finds among the list of distributions the one most similar to the distribution.
    private Distribution most_similar_distribution(List<Distribution> compare_with, Distribution distribution){
        assert(!compare_with.isEmpty());
        double highest_similarity = -1;
        Distribution most_similar = null;
        for (Distribution other : compare_with){
            double similarity = distribution.similarity(other);
            assert(Double.isFinite(similarity));
            if (similarity > highest_similarity){
                highest_similarity = similarity;
                most_similar = other;
            }
        }
        assert(most_similar != null);
        return most_similar;
    }


    // A Universe's information depends on the information of its distributions
    public double information(){
        if (information_changed){
            information = distributions.stream().mapToDouble(distribution -> distribution.information()).sum();
            information *= prob_existence;
            information_changed = false;
        }
        return information;
    }

    // generate combinations. Select n out of total number.
    private Set<Set<Integer>> generate_combinations(int select_n, int from_total){
        Set<Set<Integer>> combinations = new HashSet<>();
        combinations.add(new TreeSet<>());
        for (int times = 0; times < select_n; times++){
            Set<Set<Integer>> next_step = new HashSet<>();
            for (Set<Integer> existing : combinations){
                for (int i = 0; i < from_total; i++){
                    Set<Integer> extended = new TreeSet<>(existing);
                    if (extended.add(i)){
                        next_step.add(extended);
                    }
                }
            }
            combinations = next_step;
        }
        return combinations;
    }
}
