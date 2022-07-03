import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Random;


/**
 * Has height * width fields. Particles move randomly on the board.
 * A field can be a rock ->
 * A particle cannot step on the field. Rocks are known to the Observer.
 * A field can be visible ->
 * The Observer is informed how many particles are on the field.
 */
public class Board implements Cloneable {
    /**
     * Observer that needs to be informed.
     */
    private Observer observer;
    /**
     * Randomness for particle movement.
     */
    private Random rand = new Random();
    /**
     * Number of vertical fields.
     */
    private static int height;
    /**
     * Number of horizontal fields.
     */
    private static int width;
    /**
     * For each field the number of particles as int.
     */
    private int[][] particles;
    /**
     * Positions that have a rock.
     */
    private Set<Position> rocks;
    /**
     * Positions that are visible.
     */
    private Set<Position> visible;

    /**
     * @param height Number of vertical fields.
     * @param width Number of horizontal fields.
     */
    public Board(final int height, final int width) {
        this.height = height;
        this.width = width;
        particles = new int[height][width];
        rocks = new HashSet<>();
        visible = new HashSet<>();
    }

    /**
     * Connect to Observer.
     * @param observer Observer to connect with.
     */
    public void connectObserver(final Observer observer) {
        this.observer = observer;
    }

    /**
     * Initialize the board (particles, rocks, visibility).
     * @param encodedBoard 1 : particle, 2 : rock, 3 : visible.
     */
    public void initializeBoard(final int[][] encodedBoard) {
        IntStream.range(0, this.height).forEach(i -> IntStream.range(0, this.width).forEach(j -> {
            switch (encodedBoard[i][j]) {
                case 1: switchParticle(new Position(i, j)); break;
                case 2: switchRock(new Position(i, j)); break;
                case 3: switchVisibility(new Position(i, j));
                default:
            }
        }));
    }

    /**
     * Copy constructor to create deep copy.
     * @param board Board to make copy of.
     */
    public Board(final Board board) {
        this.rand = board.rand;
        this.height = board.height;
        this.width = board.width;
        this.particles = board.copyParticles();
        this.rocks = board.rocks.stream().map(p ->
                new Position(p.first, p.second)).collect(Collectors.toSet());
        this.visible = board.visible.stream().map(p ->
                new Position(p.first, p.second)).collect(Collectors.toSet());

    }

    @Override
    public Board clone() {
        return new Board(this);
    }

    /**
     * Check if position is within the board.
     * @param position Position on board.
     * @return true iff position is within the board.
     */
    private boolean isPositionWithinBorders(final Position position) {
        return     position.first >= 0
                && position.second >= 0
                && position.first < height
                && position.second < width;
    }

    /**
     * Check if a particle can move on position.
     * @param position Position on the board.
     * @return true iff a particle can move on position.
     */
    private boolean positionPossible(final Position position) {
        return isPositionWithinBorders(position) && !isRock(position);
    }

    /**
     * Add or delete a particle. Inform the observer.
     * @param position Position on the board.
     * @return true iff particle has been added or deleted.
     */
    public boolean switchParticle(final Position position) {
        if (!positionPossible(position)) {
            return false;
        }
        List<Position> area;
        if (isVisible(position)) {
            area = new ArrayList<>(1);
            area.add(position);
        } else {
            area = new ArrayList<>(height * width - (rocks.size() + visible.size()));
            IntStream.range(0, height).forEach(i -> IntStream.range(0, width).forEach(j -> {
                if (!isVisible(i, j) && !isRock(i, j)) {
                    area.add(new Position(i, j));
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

    /**
     * Add or delete a rock. Inform the observer.
     * @param position Position on the board.
     * @return true iff rock has been added or deleted.
     */
    public boolean switchRock(final Position position) {
        if (isParticle(position)) {
            return false;
        }
        if (isRock(position)) {
            rocks.remove(position);
        } else {
            rocks.add(position);
            observer.observeParticles(position, 0);
        }
        return true;
    }

    /**
     * Switch visibility of field. Inform the observer.
     * @param position Position on the board.
     * @return true iff visibility has been changed.
     */
    public boolean switchVisibility(final Position position) {
        if (isRock(position)) {
            return false;
        }
        if (isVisible(position)) {
            visible.remove(position);
        } else {
            visible.add(position);
        }
        observer.observeParticles(position, particles[position.first][position.second]);
        return true;
    }

    /**
     * Move particles randomly in vertical or horizontal direction.
     * Particles stay at their position if their destination is impossible.
     * Particles do not interfere.
     */
    public void stepInTime() {
        observer.observeTimeStep();
        int[][] oldParticles = copyParticles();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                for (int n = 0; n < oldParticles[i][j]; n++) {
                    List<Position> possibleSteps = possibleSteps(new Position(i, j));
                    int step = rand.nextInt(4);
                    if (step > possibleSteps.size() - 1) {
                        continue;
                    }
                    Position stepTo = possibleSteps.get(step);
                    particles[i][j] -= 1;
                    particles[stepTo.first][stepTo.second] += 1;
                }
            }
        }
        // Inform the Observer.
        visible.stream().forEach(pos -> observer.observeParticles(pos, particles[pos.first][pos.second]));
    }

    /**
     * Compute all positions on a board with given height and width.
     * @param height Vertical range.
     * @param width Horizontal range.
     * @return All positions on a board with given height and width.
     */
    public static List<Position> allPositions(final int height, final int width) {
        List<Position> allPositions = new ArrayList<>(height * width);
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        allPositions.add(new Position(i, j))));
        return allPositions;
    }

    // returns

    /**
     * Compute the positions where a particle can move to.
     * @param from Particle's starting position.
     * @return Positions where the particle can move to.
     */
    public List<Position> possibleSteps(final Position from) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        return Arrays.stream(directions).map(dir -> new Position(from.first + dir[0], from.second + dir[1])).
                filter(pos -> positionPossible(pos)).collect(Collectors.toList());
    }

    /**
     * Copy particles (int[][]).
     * @return A copy of particles.
     */
    private int[][] copyParticles() {
        int[][] newField = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                newField[i][j] = particles[i][j];
            }
        }
        return newField;
    }

    /**
     * Check if position has rock.
     * @param position Position on the board.
     * @return true iff position has rock.
     */
    public boolean isRock(final Position position) {
        return rocks.contains(position);
    }
    /**
     * Check if position has particle.
     * @param position Position on the board.
     * @return true iff position has particle.
     */
    public boolean isParticle(final Position position) {
        return particles[position.first][position.second] > 0;
    }
    /**
     * Check if position is visible.
     * @param position Position on the board.
     * @return true iff position is visible.
     */
    public boolean isVisible(final Position position) {
        return visible.contains(position);
    }
    /**
     * Check if position has rock.
     * @param x X-coordinate on the board.
     * @param y Y-coordinate on the board.
     * @return true iff position has rock.
     */
    public boolean isRock(final int x, final int y) {
        return isRock(new Position(x, y));
    }
    /**
     * Check if position has particle.
     * @param x X-coordinate on the board.
     * @param y Y-coordinate on the board.
     * @return true iff position has particle.
     */
    public boolean isParticle(final int x, final int y) {
        return isParticle(new Position(x, y));
    }
    /**
     * Check if position is visible.
     * @param x X-coordinate on the board.
     * @param y Y-coordinate on the board.
     * @return true iff position is visible.
     */
    public boolean isVisible(final int x, final int y) {
        return isVisible(new Position(x, y));
    }
    /**
     * Add or delete a particle. Inform the observer.
     * @param x X-coordinate on the board.
     * @param y Y-coordinate on the board.
     * @return true iff particle has been added or deleted.
     */
    public boolean switchParticle(final int x, final int y) {
        return switchParticle(new Position(x, y));
    }
    /**
     * Add or delete a rock. Inform the observer.
     * @param x X-coordinate on the board.
     * @param y Y-coordinate on the board.
     * @return true iff rock has been added or deleted.
     */
    public boolean switchRock(final int x, final int y) {
        return switchRock(new Position(x, y));
    }
    /**
     * Switch visibility of field. Inform the observer.
     * @param x X-coordinate on the board.
     * @param y Y-coordinate on the board.
     * @return true iff visibility has been changed.
     */
    public boolean switchVisibility(final int x, final int y) {
        return switchVisibility(new Position(x, y));
    }

}
