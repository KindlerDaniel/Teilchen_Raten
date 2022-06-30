/*
The Board is the reality the observer can observe.
The Board is a grid where each square can have a particle or rock.
 */

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
Has height * width fields. Particles move randomly over the field with every step in time.
A field can be visible: The Observer is informed how many particles are present on the field.
A field can be a rock: A particle cannot step on the field. Rocks are known to the Observer.
 */
public class Board implements Cloneable {
    private Observer observer;
    private Random rand = new Random();
    private static int height;
    private static int width;
    private int[][] particles;
    private Set<Pair<Integer, Integer>> rocks;
    private Set<Pair<Integer, Integer>> visible;


    public Board(final int height, final int width) {
        this.height = height;
        this.width = width;
        particles = new int[height][width];
        rocks = new HashSet<>();
        visible = new HashSet<>();
    }

    public void connectObserver(final Observer observer) {
        this.observer = observer;
    }

    // Connect to observer and then put the board into initial state.
    public void initialize(final Observer observer, final int[][] encoded_board) {
        connectObserver(observer);
        IntStream.range(0, this.height).forEach(i -> IntStream.range(0, this.width).forEach(j -> {
            switch(encoded_board[i][j]) {
                case 1: switchParticle(new Pair(i, j)); break;
                case 2: switchRock(new Pair(i, j)); break;
                case 3: switchVisible(new Pair(i, j));
                default:
            }
        }));
    }

    // copy constructor creates deep copy
    public Board(final Board other) {
        this.rand = other.rand;
        this.height = other.height;
        this.width = other.width;
        this.particles = other.copyParticles();
        this.rocks = other.rocks.stream().map(p ->
        new Pair<Integer, Integer>(p.first, p.second)).collect(Collectors.toSet());
        this.visible = other.visible.stream().map(p ->
        new Pair<Integer, Integer>(p.first, p.second)).collect(Collectors.toSet());

    }

    @Override
    public Board clone() {
        return new Board(this);
    }


    // Is the position within the borders of the board?
    private boolean positionWithinBorders(final Pair<Integer, Integer> position) {
        return     position.first >= 0
                && position.second >= 0
                && position.first < height
                && position.second < width;
    }

    // Can a particle step on this position?
    private boolean positionPossible(final Pair<Integer, Integer> position) {
        return positionWithinBorders(position) && !isRock(position);
    }

    // Adds or deletes a particle at position. Informs the Observer.
    public boolean switchParticle(final Pair<Integer, Integer> position) {
        if (!positionPossible(position)) {
            return false;
        }
        List<Pair<Integer, Integer>> area;
        if (isVisible(position)) {
            area = new ArrayList<>(1);
            area.add(position);
        } else {
            area = new ArrayList<>(height * width - (rocks.size() + visible.size()));
            IntStream.range(0, height).forEach(i -> IntStream.range(0, width).forEach(j -> {
                if (!isVisible(i, j) && !isRock(i, j)) {
                    area.add(new Pair(i, j));
                }
            }));
        }
        if (isParticle(position)) {
            particles[position.first][position.second] -= 1;
            observer.deletedParticleWithin(area);
        } else {
            particles[position.first][position.second] = 1;
            observer.newParticleWithin(area);
        }
        return true;
    }

    // Removes or add rock if possible. Informs the Observer.
    public boolean switchRock(final Pair<Integer, Integer> position) {
        if (isParticle(position)) {
            return false;
        }
        if (isRock(position)) {
            rocks.remove(position);
        } else {
            rocks.add(position);
            observer.observeField(position, 0);
        }
        return true;
    }

    // Makes field visible or invisible. Informs the Observer.
    public void switchVisible(final Pair<Integer, Integer> position) {
        if (isVisible(position)) {
            visible.remove(position);
            return;
        }
        visible.add(position);
        observer.observeField(position, particles[position.first][position.second]);
    }

    // Particles move randomly within possible fields.
    // Particles can stay at their position and do not interfere.
    public void stepInTime() {
        observer.observeTimeStep();
        int[][] oldParticles = copyParticles();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                for (int n = 0; n < oldParticles[i][j]; n++) {
                    List<Pair<Integer, Integer>> possibleSteps = possibleSteps(new Pair(i, j));
                    int step = rand.nextInt(4);
                    if (step > possibleSteps.size() - 1) {
                        continue;
                    }
                    Pair<Integer, Integer> stepTo = possibleSteps.get(step);
                    particles[i][j] -= 1;
                    particles[stepTo.first][stepTo.second] += 1;
                }
            }
        }
        // Inform the Observer.
        visible.stream().forEach(pos -> observer.observeField(pos, particles[pos.first][pos.second]));
    }

    // List of positions on a board with certain height and width.
    public static List<Pair<Integer, Integer>> allPositions(final int height, final int width) {
        List<Pair<Integer, Integer>> allPositions = new ArrayList<>(height * width);
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        allPositions.add(new Pair(i, j))));
        return allPositions;
    }

    // returns possible positions that can be reached from a starting position
    public List<Pair<Integer, Integer>> possibleSteps(final Pair<Integer, Integer> from) {
        int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        return Arrays.stream(directions).map(dir -> new Pair<>(from.first + dir[0], from.second + dir[1])).
                filter(pos -> positionPossible(pos)).collect(Collectors.toList());
    }

    // make a copy of the field
    private int[][] copyParticles() {
        int[][] newField = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                newField[i][j] = particles[i][j];
            }
        }
        return newField;
    }


    public boolean isRock(final Pair<Integer, Integer> position) {
        return rocks.contains(position);
    }
    public boolean isParticle(final Pair<Integer, Integer> position) {
        return particles[position.first][position.second] > 0;
    }
    public boolean isVisible(final Pair<Integer, Integer> position) {
        return visible.contains(position);
    }


    // Alternative signature for some public functions
    public boolean isRock(final int x, final int y) {
        return isRock(new Pair(x, y));
    }
    public boolean isParticle(final int x, final int y) {
        return isParticle(new Pair(x, y));
    }
    public boolean isVisible(final int x, final int y) {
        return isVisible(new Pair(x, y));
    }
    public boolean switchParticle(final int x, final int y) {
        return switchParticle(new Pair(x, y));
    }
    public boolean switchRock(final int x, final int y) {
        return switchRock(new Pair(x, y));
    }
    public void switchVisible(final int x, final int y) {
        switchVisible(new Pair(x, y));
    }

}
