import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/*
Represents a probability distribution of a particle.
It has always a total probability of 1 and diffuses over time.
It concentrates again when the particle is observed.
*/
public class Distribution implements Cloneable {
    /**
     * Round probability by this constant before checking equality
     * in hashCode() and equals()
     */
    static final double TOL = 1e15;
    /**
     * Height of field.
     */
    private static int height;
    /**
     * Width of field.
     */
    private static int width;
    /**
     * Probability distribution.
     * 2D array of doubles that sum to 1.
     */
    private double[][] field;
    /**
     * Universe of which this distribution is part of.
     */
    private Universe universe;
    /**
     * information content of field.
     */
    private double information;
    /**
     * Has information changed after it has been computed last time?
     */
    private boolean informationChanged;

    /**
     * @param height height of field.
     * @param width width of field.
     * @param field initial field.
     * @param universe see universe.
     */
    public Distribution(final int height, final int width, final double[][] field, final Universe universe) {
        this.height = height;
        this.width = width;
        this.field = field;
        this.universe = universe;
        informationChanged = true;
    }

    /**
     * @param height height of field.
     * @param width width of field.
     * @param area Area where field must be present.
     * @param universe see universe.
     */
    public Distribution(final int height, final int width, final List<Position> area, final Universe universe) {
        this(height, width, equalDistr(height, width, area), universe);

    }

    @Override
    public Distribution clone() {
        Distribution clone = new Distribution(height, width, copyDistribution(field), universe);
        clone.information = information;
        clone.informationChanged = informationChanged;
        return clone;
    }

    /**
     * @param position position on field.
     * @return probability of field at position.
     */
    public double probability(final Position position) {
        return field[position.first][position.second];
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
     * @return true iff other Distribution's field rounded by tol is same at each position.
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Distribution)) {
            return false;
        }
        Distribution u = (Distribution) o;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Are the distributions the same up to a tolerance tol?
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
     * Compute similarity according to Bhattacharyya distance.
     * @param distribution another Distribution.
     * @return Bhattacharyya distance between the distributions fields.
     */
    public double similarity(final Distribution distribution) {
        return Board.allPositions(height, width).stream().
                mapToDouble(pos -> Math.sqrt(probability(pos) * distribution.probability(pos))).sum();
    }

    /**
     * Diffuse probability mass on field in a way particles would do.
     * @param board The board watched by the Universe of this distribution.
     */
    public void diffuse(final Board board) {
        double[][] oldField = copyDistribution(field);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (oldField[i][j] == 0) {
                    continue;
                }
                List<Position> possibleSteps = board.possibleSteps(new Position(i, j));
                for (Position pos : possibleSteps) {
                    double movingMass = 0.25 * oldField[i][j];
                    field[i][j] -= movingMass;
                    field[pos.first][pos.second] += movingMass;
                }
            }
        }
    }


    /**
     * Creates an equally distributed (within area) probability distribution.
     * @param height height of the probability distribution.
     * @param width width of the probability distribution.
     * @param area Area where field must be present.
     * @return equally distributed (within area) probability distribution.
     */
    private static double[][] equalDistr(final int height, final int width, final List<Position> area) {
        double[][] equalField = new double[height][width];
        double averageProb = 1.0 / area.size();
        area.stream().forEach(pair -> {
            equalField[pair.first][pair.second] = averageProb; });
        return equalField;
    }

    /**
     * Concentrate probability at one position
     * @param at position where probability concentrates.
     */
    public void concentrateAt(final Position at) {
        field = new double[height][width];
        field[at.first][at.second] = 1;
        informationChanged = true;

    }

    /**
     * Retract probability from a position.
     * @param at position where probability vanishes.
     */
    public void vanishFrom(final Position at) {
        double rescalingFactor = 1 / (1 - field[at.first][at.second]);
        IntStream.range(0, height).forEach(i -> IntStream.range(0, width).forEach(j
                -> field[i][j] *= rescalingFactor));
        field[at.first][at.second] = 0;
        informationChanged = true;
    }

    /**
     * Merge field from another Distribution into field.
     * @param other the other Distribution.
     * @param prop Proportion of the other field when mixed.
     */
    public void mergeIn(final Distribution other, final double prop) {
        Board.allPositions(height, width).stream().forEach(pos ->
                field[pos.first][pos.second] =
                        (1 - prop) * field[pos.first][pos.second]
                                + prop * other.field[pos.first][pos.second]);
        normalize();
    }

    /**
     * Compute information content of field as 1 - its entropy.
     * @return information content of field.
     */
    public double information() {
        final double minimalConstant = 1e-60;
        if (informationChanged) {
            int events = height * width;
            double entropy = IntStream.range(0, height).mapToDouble(i ->
                    IntStream.range(0, width).mapToDouble(j -> field[i][j]).
                            map(prob -> -prob * Math.log(prob + minimalConstant) / Math.log(events)).sum()).sum();
            information = 1 - entropy;
            informationChanged = false;
        }
        return information;
    }

    /**
     * Compute exact total probability of field (should be very close to 1).
     * @return total probability of field.
     */
    private double totalProbability() {
        double totalProb = IntStream.range(0, height).mapToDouble(i ->
                IntStream.range(0, width).mapToDouble(j ->
                        field[i][j]).sum()).sum();
        return totalProb;
    }

    /**
     * Guarantees that field has no values < 0.
     * Guarantees that total probability of field is 1.
     * These conditions should always hold mathematically -
     * but for technical reasons lead to small inaccuracies.
     */
    public void normalize() {
        // No probabilities smaller than 0 allowed.
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        field[i][j] = Double.max(field[i][j], 0)));
        double total = totalProbability();
        // set total probability to 1.
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        field[i][j] /= total));
    }


    /**
     * Copy a double[][].
     * @param toCopy array to be copied.
     * @return copy of the array.
     */
    public static double[][] copyDistribution(final double[][] toCopy) {
        int arrayHeight = toCopy.length;
        int arrayWidth = toCopy[0].length;
        double[][] newArray = new double[arrayHeight][arrayWidth];
        for (int i = 0; i < arrayHeight; i++) {
            for (int j = 0; j < arrayWidth; j++) {
                newArray[i][j] = toCopy[i][j];
            }
        }
        return newArray;
    }

    /**
     * Setter for universe.
     * @param universe the new universe.
     */
    public void setUniverse(final Universe universe) {
        this.universe = universe;
    }

    /**
     * @return string that shows the field.
     */
    public String toString() {
        String str = "";
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                str += field[i][j] + " ";
            }
            str += "\n";
        }
        return str;
    }
}
