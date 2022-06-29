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
    private double tol = 1e15;
    private double information;
    private boolean information_changed;

    public Distribution(int height, int width, double[][] field, Universe universe){
        this.height = height;
        this.width = width;
        this.field = field;
        this.universe = universe;
        information_changed = true;
    }

    public Distribution(int height, int width, List<Pair<Integer, Integer>> area, Universe universe){
        this(height, width, equal_distr(height, width, area), universe);

    }

    @Override
    public Distribution clone(){
        Distribution clone = new Distribution(height, width, copy_distribution(field), universe);
        clone.information = information;
        clone.information_changed = information_changed;

        return clone;

    }

    // get the probability at position
    public double probability(Pair<Integer, Integer> position){
        return field[position.first][position.second];
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
        if (!(o instanceof Distribution)) return false;
        Distribution u = (Distribution) o;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Are the distributions the same up to a tolerance tol?
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
    public int compareTo(Distribution distribution) {
        return Double.compare(distribution.information(), this.information());
    }

    // The Bhattacharyya distance
    public double similarity(Distribution distribution){
        return Board.all_positions(height, width).stream().
        mapToDouble(pos -> Math.sqrt(probability(pos) * distribution.probability(pos))).sum();
    }

    // diffuse the probability mass in a way particles could move
    public void diffuse(Board board){
        double[][] old_field = copy_distribution(field);
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++){
                if (old_field[i][j] == 0){continue;}
                List<Pair<Integer, Integer>> possible_steps = board.possible_steps(new Pair(i, j));
                for (Pair<Integer, Integer> pos : possible_steps) {
                    double moving_mass = 0.25 * old_field[i][j];
                    field[i][j] -= moving_mass;
                    field[pos.first][pos.second] += moving_mass;
                }
            }
        }
    }


    // create field that is equally distributed within the area
    private static double[][] equal_distr(int height, int width, List<Pair<Integer, Integer>> area){
        double[][] equal_field = new double[height][width];
        double average_prob = 1.0 / area.size();
        area.stream().forEach(pair -> {equal_field[pair.first][pair.second] = average_prob;});
        return equal_field;
    }

    // concentrate all probability at one position
    public void concentrate_at(Pair<Integer, Integer> at){
        field = new double[height][width];
        field[at.first][at.second] = 1;
        information_changed = true;

    }

    // retract the probability from a position
    public void vanish_from(Pair<Integer, Integer> at){
        double rescaling_factor = 1 / (1 - field[at.first][at.second]);
        IntStream.range(0, height).forEach(i -> IntStream.range(0, width).forEach(j
                -> field[i][j] *= rescaling_factor));
        field[at.first][at.second] = 0;
        information_changed = true;
    }


    // merge another field into own field.
    public void merge_in(Distribution other, double prop){
        Board.all_positions(height, width).stream().forEach(pos ->
        field[pos.first][pos.second] =
        (1 - prop) * field[pos.first][pos.second]
        + prop * other.field[pos.first][pos.second]);
        normalize();
    }


    // The information of a probability distribution is 1 - its entropy
    public double information(){
        if (information_changed){
            int events = height * width;
            double entropy = IntStream.range(0, height).mapToDouble(i ->
                    IntStream.range(0, width).mapToDouble(j -> field[i][j]).
                            map(prob -> - prob * Math.log(prob + 1e-60) / Math.log(events)).sum()).sum();
            information = 1 - entropy;
            information_changed = false;
        }
        return information;
    }

    // total probability should always be EXACTLY one.
    // But some operations change it for technical reasons by little amount.
    private double total_probability(){
        double total_prob = IntStream.range(0, height).mapToDouble(i ->
                IntStream.range(0, width).mapToDouble(j ->
                        field[i][j]).sum()).sum();
        return total_prob;
    }

    public void normalize(){
        // No probabilities smaller than 0 allowed
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        field[i][j] = Double.max(field[i][j], 0)));
        double total = total_probability();
        // total probability shall be EXACTLY one
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        field[i][j] /= total));
    }

    // There is no pre-build functionality to simply copy a double[][]
    public static double[][] copy_distribution(double[][] field){
        int height = field.length;
        int width = field[0].length;
        double[][] new_field = new double[height][width];
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++){
                new_field[i][j] = field[i][j];
            }
        }
        return new_field;
    }

    public void setUniverse(Universe universe){
        this.universe = universe;
    }

    public String toString(){
        String str = "";
        for (int i=0; i<height; i++){
            for (int j=0; j<width; j++){
                str += field[i][j] + " ";
            }
            str += "\n";
        }
        return str;
    }

}
