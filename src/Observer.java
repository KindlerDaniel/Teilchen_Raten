import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
The Observer's job is to guess for each position on the Board
the probability that there is a particle.
The Observer observes particles on a visible fields,
but does not know WHICH particles it has observed.
There are many possibilities... what to do?
The Observer initiates its Universes to split into daughter Universes,
one for each possibility. But the number of Universes will explode... what to do about that?
The Observer reduces the number of Universes to max_universes by using an approximation.
 */
public class Observer implements Cloneable {
    private static int height;
    private static int width;
    static final int max_universes = 15;
    private List<Universe> universes;
    private Board board;


    public Observer(int height, int width){
        this.height = height;
        this.width = width;
        universes = new ArrayList<>(max_universes);
        universes.add(new Universe(height, width, 1));
    }

    public Observer(int height, int width, List<Universe> universes){
        this.height = height;
        this.width = width;
        this.universes = universes;
    }

    public void connect_board(Board board){
        this.board = board;
    }


    @Override
    public Observer clone(){
        List<Universe> cloned_universes = universes.stream().map(u -> u.clone()).
            collect(Collectors.toList());
        return new Observer(height, width, cloned_universes);
    }

    // Takes the weighted average over all Universes
    public double probability(Pair<Integer, Integer> position){
        return universes.stream().mapToDouble(u ->
                u.prob_existence * u.probability(position)).sum();
    }

    // Observation: There are n points at certain position
    public void observe_field(Pair<Integer, Integer> position, int number){
        List<Universe> daughter_universes = new ArrayList<>();
        universes.stream().forEach(u -> daughter_universes.addAll(u.split_after_observation(position, number)));
        universes = daughter_universes;
        shrink();

    }

    // Observation: A new particle has been added within a certain area
    public void new_particle_within(List<Pair<Integer, Integer>> area){
        for (Universe universe : universes){
            universe.add_distribution(area);
        }
    }

    // Observation: A new particle has been deleted within a certain area
    public void deleted_particle_within(List<Pair<Integer, Integer>> area){
        List<Universe> daughter_universes = new ArrayList<>();
        universes.stream().forEach(u -> daughter_universes.addAll(u.split_after_deletion(area)));
        universes = daughter_universes;
        shrink();
    }

    // Observation: Step in time has happened
    public void observe_timeStep(){
        universes.stream().forEach(u -> u.simulate_timeStep(board));
    }


    // Reduce the number of Universes back to max_universes.
    private void shrink(){
        // Eliminate all uniform Universes (see definition of Universe.equals())
        Set<Universe> representatives = new HashSet<>(universes);
        // Each remaining representative gets the probability of its uniform Universes.
        representatives.stream().forEach(universe -> universe.prob_existence =
        universes.stream().filter(u -> universe.equals(u)).mapToDouble(u -> u.prob_existence).sum());
        universes = new ArrayList<>(representatives);

        // Sort remaining Universes by their information content.
        universes.sort(Universe::compareTo);
        // Delete the less informative Universes and store them in less_relevant.
        int split_pos = Integer.min(universes.size(), max_universes);
        List<Universe> less_relevant = new ArrayList<>(universes.subList(split_pos, universes.size()));
        universes = new ArrayList<>(universes.subList(0, split_pos));

        dissolve(less_relevant);
        normalize();
    }

    // Dissolve Universes into the existing
    private void dissolve(List<Universe> dissolving){
        Universe[] merge_into = new Universe[dissolving.size()];
        for (int i = 0; i < dissolving.size(); i++){
            merge_into[i] = most_similar_universe(dissolving.get(i));
        }

        for (int i = 0; i < dissolving.size(); i++) {
            merge_into[i].merge_in(dissolving.get(i));
        }

    }

    // Returns the Universe that is most similar to the given other.
    private Universe most_similar_universe(Universe other){
        Universe most_similar = null;
        double highest_similarity = -1;
        for (Universe universe : universes){
            double similarity = universe.similarity(other);
            if (similarity > highest_similarity){
                most_similar = universe;
                highest_similarity = similarity;
            }
        }
        return most_similar;
    }


    // Make sure that universes probabilities sum up to 1.
    private void normalize(){
        double total_probability = universes.stream().mapToDouble(u -> u.prob_existence).sum();
        universes.stream().forEach(u -> u.prob_existence /= total_probability);
    }

}
