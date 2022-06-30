import java.util.*;
import java.util.stream.Collectors;

/*
The Observer's job is to guess for each position on the Board
the probability that there is a particle.
The Observer observes particles on a visible fields,
but does not know WHICH particles it has observed.
There are many possibilities... what to do?
The Observer tells its Universes to split into daughter Universes,
one for each possibility. But the number of Universes will explode... what to do about that?
The Observer reduces the number of Universes to max_universes by using an approximation.
 */
public class Observer implements Cloneable {
    private static int height;
    private static int width;
    static final int MAX_UNIVERSES = 15;
    private List<Universe> universes;
    private Board board;


    public Observer(final int height, final int width) {
        this.height = height;
        this.width = width;
        universes = new ArrayList<>(MAX_UNIVERSES);
        universes.add(new Universe(height, width, 1));
    }

    public Observer(final int height, final int width, final List<Universe> universes) {
        this.height = height;
        this.width = width;
        this.universes = universes;
    }

    public void connectBoard(final Board board) {
        this.board = board;
    }


    @Override
    public Observer clone() {
        List<Universe> clonedUniverses = universes.stream().map(u -> u.clone()).
            collect(Collectors.toList());
        return new Observer(height, width, clonedUniverses);
    }

    // Takes the weighted average over all Universes
    public double probability(final Pair<Integer, Integer> position) {
        return universes.stream().mapToDouble(u ->
                u.probExistence * u.probability(position)).sum();
    }

    // Observation: There are n points at certain position
    public void observeField(final Pair<Integer, Integer> position, final int number) {
        List<Universe> daughterUniverses = new ArrayList<>();
        universes.stream().forEach(u -> daughterUniverses.addAll(u.splitAfterObservation(position, number)));
        universes = daughterUniverses;
        shrink();

    }

    // Observation: A new particle has been added within a certain area
    public void newParticleWithin(final List<Pair<Integer, Integer>> area) {
        for (Universe universe : universes) {
            universe.addDistribution(area);
        }
    }

    // Observation: A new particle has been deleted within a certain area
    public void deletedParticleWithin(final List<Pair<Integer, Integer>> area) {
        List<Universe> daughterUniverses = new ArrayList<>();
        universes.stream().forEach(u -> daughterUniverses.addAll(u.splitAfterDeletion(area)));
        universes = daughterUniverses;
        shrink();
    }

    // Observation: Step in time has happened
    public void observeTimeStep() {
        universes.stream().forEach(u -> u.simulateTimeStep(board));
    }


    // Reduce the number of Universes back to max_universes.
    private void shrink() {
        // Eliminate all uniform Universes (see definition of Universe.equals())
        Set<Universe> representatives = new HashSet<>(universes);
        // Each remaining representative gets the probability of its uniform Universes.
        representatives.stream().forEach(universe -> universe.probExistence =
        universes.stream().filter(u -> universe.equals(u)).mapToDouble(u -> u.probExistence).sum());
        universes = new ArrayList<>(representatives);

        // Sort remaining Universes by their information content.
        universes.sort(Universe::compareTo);
        // Delete the less informative Universes and store them in less_relevant.
        int splitPos = Integer.min(universes.size(), MAX_UNIVERSES);
        List<Universe> lessRelevant = new ArrayList<>(universes.subList(splitPos, universes.size()));
        universes = new ArrayList<>(universes.subList(0, splitPos));

        dissolve(lessRelevant);
        normalize();
    }

    // Dissolve Universes into the existing
    private void dissolve(final List<Universe> dissolving) {
        Universe[] mergeInto = new Universe[dissolving.size()];
        for (int i = 0; i < dissolving.size(); i++) {
            mergeInto[i] = mostSimilarUniverse(dissolving.get(i));
        }

        for (int i = 0; i < dissolving.size(); i++) {
            mergeInto[i].mergeIn(dissolving.get(i));
        }

    }

    // Returns the Universe that is most similar to the given other.
    private Universe mostSimilarUniverse(final Universe other) {
        Universe mostSimilar = null;
        double highestSimilarity = -1;
        for (Universe universe : universes) {
            double similarity = universe.similarity(other);
            if (similarity > highestSimilarity) {
                mostSimilar = universe;
                highestSimilarity = similarity;
            }
        }
        return mostSimilar;
    }


    // Make sure that universes probabilities sum up to 1.
    private void normalize() {
        double totalProbability = universes.stream().mapToDouble(u -> u.probExistence).sum();
        universes.stream().forEach(u -> u.probExistence /= totalProbability);
    }

}
