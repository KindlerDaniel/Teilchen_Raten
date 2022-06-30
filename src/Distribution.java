import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/*
Represents a probability distribution of a particle.
It has always a total probability of 1 and diffuses over time.
It concentrates again when the particle is observed.
*/
public class Distribution implements Cloneable, Comparable<Distribution> {
    private static int height;
    private static int width;
    private double[][] field;
    private Universe universe;
    private final double tol = 1e15;
    private double information;
    private boolean informationChanged;

    public Distribution(final int height, final int width, final double[][] field, final Universe universe) {
        this.height = height;
        this.width = width;
        this.field = field;
        this.universe = universe;
        informationChanged = true;
    }

    public Distribution(final int height, final int width, final List<Pair<Integer, Integer>> area, final Universe universe) {
        this(height, width, equal_distr(height, width, area), universe);

    }

    @Override
    public Distribution clone() {
        Distribution clone = new Distribution(height, width, copyDistribution(field), universe);
        clone.information = information;
        clone.informationChanged = informationChanged;
        return clone;
    }

    // get the probability at position
    public double probability(final Pair<Integer, Integer> position) {
        return field[position.first][position.second];
    }


    @Override
    public int hashCode() {
        double[] numbers = new double[height * width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                numbers[i * width + j] = Math.round(probability(new Pair<>(i, j)) * tol) / tol;
            }
        }
        return Arrays.hashCode(numbers);
    }

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
                double thisProb = Math.round(probability(new Pair<>(i, j)) * tol) / tol;
                double otherProb = Math.round(u.probability((new Pair<>(i, j))) * tol) / tol;
                if (thisProb != otherProb) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int compareTo(final Distribution distribution) {
        return Double.compare(distribution.information(), this.information());
    }

    // The Bhattacharyya distance
    public double similarity(final Distribution distribution) {
        return Board.all_positions(height, width).stream().
        mapToDouble(pos -> Math.sqrt(probability(pos) * distribution.probability(pos))).sum();
    }

    // diffuse the probability mass in a way particles could move
    public void diffuse(final Board board) {
        double[][] oldField = copyDistribution(field);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (oldField[i][j] == 0) {
                    continue;
                }
                List<Pair<Integer, Integer>> possibleSteps = board.possible_steps(new Pair(i, j));
                for (Pair<Integer, Integer> pos : possibleSteps) {
                    double movingMass = 0.25 * oldField[i][j];
                    field[i][j] -= movingMass;
                    field[pos.first][pos.second] += movingMass;
                }
            }
        }
    }


    // create field that is equally distributed within the area
    private static double[][] equal_distr(final int height, final int width, final List<Pair<Integer, Integer>> area) {
        double[][] equalField = new double[height][width];
        double averageProb = 1.0 / area.size();
        area.stream().forEach(pair -> {
            equalField[pair.first][pair.second] = averageProb; });
        return equalField;
    }

    // concentrate all probability at one position
    public void concentrateAt(final Pair<Integer, Integer> at) {
        field = new double[height][width];
        field[at.first][at.second] = 1;
        informationChanged = true;

    }

    // retract the probability from a position
    public void vanishFrom(final Pair<Integer, Integer> at) {
        double rescalingFactor = 1 / (1 - field[at.first][at.second]);
        IntStream.range(0, height).forEach(i -> IntStream.range(0, width).forEach(j
                -> field[i][j] *= rescalingFactor));
        field[at.first][at.second] = 0;
        informationChanged = true;
    }


    // merge another field into own field.
    public void mergeIn(final Distribution other, final double prop) {
        Board.all_positions(height, width).stream().forEach(pos ->
        field[pos.first][pos.second] =
        (1 - prop) * field[pos.first][pos.second]
        + prop * other.field[pos.first][pos.second]);
        normalize();
    }


    // The information of a probability distribution is 1 - its entropy
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

    // total probability should always be EXACTLY one.
    // But some operations change it for technical reasons by little amount.
    private double totalProbability() {
        double totalProb = IntStream.range(0, height).mapToDouble(i ->
                IntStream.range(0, width).mapToDouble(j ->
                        field[i][j]).sum()).sum();
        return totalProb;
    }

    public void normalize() {
        // No probabilities smaller than 0 allowed
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        field[i][j] = Double.max(field[i][j], 0)));
        double total = totalProbability();
        // total probability shall be EXACTLY one
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        field[i][j] /= total));
    }

    // There is no pre-build functionality to simply copy a double[][]
    public static double[][] copyDistribution(final double[][] field) {
        int height = field.length;
        int width = field[0].length;
        double[][] newField = new double[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                newField[i][j] = field[i][j];
            }
        }
        return newField;
    }

    public void setUniverse(final Universe universe) {
        this.universe = universe;
    }

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
