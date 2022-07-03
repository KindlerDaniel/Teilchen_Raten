import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * The Observer's job is to guess for each position on the Board
 * the probability that there is at least one particle.
 * The Observer observes particles on visible fields,
 * but does not know WHICH particles it has observed.
 * There are many possibilities... what to do?
 * The Observer tells its Universes to split into daughter Universes,
 * one daughter for each possibility.
 * But the number of Universes will explode... what to do about that?
 * The Observer reduces the number of Universes to max_universes (approximation).
 */
public class Observer implements Cloneable {
    /**
     * Height of observed board.
     */
    private static int height;
    /**
     * Width of observed board.
     */
    private static int width;
    /**
     * Maximal number of parallel Universes simulated at the same time.
     */
    static final int MAX_UNIVERSES = 15;
    /**
     * Simulated Universes.
     */
    private List<Universe> universes;
    /**
     * Observed Board.
     */
    private Board board;

    /**
     * @param height Height of observed board.
     * @param width Width of observed board.
     */
    public Observer(final int height, final int width) {
        this.height = height;
        this.width = width;
        universes = new ArrayList<>(MAX_UNIVERSES);
        universes.add(new Universe(height, width, 1));
    }

    /**
     * @param height Height of observed board.
     * @param width Width of observed board.
     * @param universes Initial Universes.
     */
    public Observer(final int height, final int width, final List<Universe> universes) {
        this.height = height;
        this.width = width;
        this.universes = universes;
    }

    /**
     * @param board Board to connect to.
     */
    public void connectBoard(final Board board) {
        this.board = board;
    }

    @Override
    public Observer clone() {
        List<Universe> clonedUniverses = universes.stream().map(u -> u.clone()).
                collect(Collectors.toList());
        return new Observer(height, width, clonedUniverses);
    }

    /**
     * Computes the final probability as weighted average over all Universes.
     * @param position Position on board.
     * @return Final probability at position.
     */
    public double probability(final Position position) {
        return universes.stream().mapToDouble(u ->
                u.probExistence * u.probability(position)).sum();
    }


    /**
     * Observe particles at position.
     * @param position Position on board.
     * @param number Number of particles that have been observed.
     */
    public void observeParticles(final Position position, final int number) {
        List<Universe> daughterUniverses = new ArrayList<>();
        universes.stream().forEach(u -> daughterUniverses.addAll(u.splitAfterObservation(position, number)));
        universes = daughterUniverses;
        shrink();

    }

    /**
     * Observe that a particle has been added within area.
     * @param area Area wherein a particle has been added.
     */
    public void newParticleWithin(final List<Position> area) {
        for (Universe universe : universes) {
            universe.addDistribution(area);
        }
    }

    /**
     * Observe that a particle has been deleted within area.
     * @param area Area wherein a particle has been deleted.
     */
    public void deletedParticleWithin(final List<Position> area) {
        List<Universe> daughterUniverses = new ArrayList<>();
        universes.stream().forEach(u -> daughterUniverses.addAll(u.splitAfterDeletion(area)));
        universes = daughterUniverses;
        shrink();
    }

    /**
     * Observe a step in time.
     */
    public void observeTimeStep() {
        universes.stream().forEach(u -> u.simulateTimeStep(board));
    }

    /**
     * Reduce the number of Universes to max_universes.
     */
    private void shrink() {
        // Eliminate all uniform Universes (see definition of Universe.equals())
        Set<Universe> representatives = new HashSet<>(universes);
        // Each remaining representative gets the probability of its Universes.
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

    /**
     * Dissolve Universes into existing Universes.
     * @param dissolving The Universes to dissolve.
     */
    private void dissolve(final List<Universe> dissolving) {
        Universe[] mergeInto = new Universe[dissolving.size()];
        for (int i = 0; i < dissolving.size(); i++) {
            mergeInto[i] = mostSimilarUniverse(dissolving.get(i));
        }

        for (int i = 0; i < dissolving.size(); i++) {
            mergeInto[i].mergeIn(dissolving.get(i));
        }

    }

    /**
     * @param other Universe.
     * @return The Universe among universes that is most similar to other.
     */
    private Universe mostSimilarUniverse(final Universe other) {
        Universe mostSimilar = null;
        double highestSimilarity = -1;
        for (Universe universe : universes) {
            double similarity = universe.similarity(universe);
            if (similarity > highestSimilarity) {
                mostSimilar = universe;
                highestSimilarity = similarity;
            }
        }
        return mostSimilar;
    }

    /**
     * Make sure that universes probabilities sum up to 1.
     */
    private void normalize() {
        double totalProbability = universes.stream().mapToDouble(u -> u.probExistence).sum();
        universes.stream().forEach(u -> u.probExistence /= totalProbability);
    }

}
