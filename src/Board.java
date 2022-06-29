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
    Observer observer;
    Random rand = new Random();
    private static int height;
    private static int width;
    private int[][] particles;
    private Set<Pair<Integer, Integer>> rocks;
    private Set<Pair<Integer, Integer>> visible;


    public Board(int height, int width){
        this.height = height;
        this.width = width;
        particles = new int[height][width];
        rocks = new HashSet<>();
        visible = new HashSet<>();
    }

    public void connect_observer(Observer observer){
        this.observer = observer;
    }

    // Connect to observer and then put the board into initial state.
    public void initialize(Observer observer, int[][] encoded_board){
        connect_observer(observer);
        IntStream.range(0, this.height).forEach(i -> IntStream.range(0, this.width).forEach(j ->
        {
            switch(encoded_board[i][j]){
                case 1: switch_particle(new Pair(i, j)); break;
                case 2: switch_rock(new Pair(i, j)); break;
                case 3: switch_visible(new Pair(i, j));
            }
        }));
    }

    // copy constructor creates deep copy
    public Board(Board other){
        this.rand = other.rand;
        this.height = other.height;
        this.width = other.width;
        this.particles = other.copy_particles();
        this.rocks = other.rocks.stream().map(p ->
        new Pair<Integer, Integer>(p.first, p.second)).collect(Collectors.toSet());
        this.visible = other.visible.stream().map(p ->
        new Pair<Integer, Integer>(p.first, p.second)).collect(Collectors.toSet());

    }

    @Override
    public Board clone(){
        return new Board(this);
    }


    // Is the position within the borders of the board?
    private boolean position_within_borders(Pair<Integer, Integer> position){
        return     position.first >= 0
                && position.second >= 0
                && position.first < height
                && position.second < width;
    }

    // Can a particle step on this position?
    private boolean position_possible(Pair<Integer, Integer> position){
        return position_within_borders(position) && !is_rock(position);
    }

    // Adds or deletes a particle at position. Informs the Observer.
    public boolean switch_particle(Pair<Integer, Integer> position){
        if (!position_possible(position)){return false;}

        List<Pair<Integer, Integer>> area;
        if (is_visible(position)){
            area = new ArrayList<>(1);
            area.add(position);
        } else {
            area = new ArrayList<>(height * width - (rocks.size() + visible.size()));
            IntStream.range(0, height).forEach(i -> IntStream.range(0, width).forEach(j ->
            {if (!is_visible(i, j) && !is_rock(i, j)){area.add(new Pair(i, j));}}));
        }
        if (is_particle(position)){
            particles[position.first][position.second] -= 1;
            observer.deleted_particle_within(area);
        } else {
            particles[position.first][position.second] = 1;
            observer.new_particle_within(area);
        }
        return true;
    }

    // Removes or add rock if possible. Informs the Observer.
    public boolean switch_rock(Pair<Integer, Integer> position){
        if (is_particle(position)){return false;}
        if (is_rock(position)){rocks.remove(position);}
        else{
            rocks.add(position);
            observer.observe_field(position, 0);
            }
        return true;
    }

    // Makes field visible or invisible. Informs the Observer.
    public void switch_visible(Pair<Integer, Integer> position){
        if (is_visible(position)){
            visible.remove(position);
            return;
        }
        visible.add(position);
        observer.observe_field(position, particles[position.first][position.second]);
    }

    // Particles move randomly within possible fields.
    // Particles can stay at their position and do not interfere.
    public void step_in_time() {
        observer.observe_timeStep();
        int[][] old_particles = copy_particles();
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++){
                for(int n = 0; n < old_particles[i][j]; n++){
                    List<Pair<Integer, Integer>> possible_steps = possible_steps(new Pair(i, j));
                    int step = rand.nextInt(4);
                    if (step > possible_steps.size() - 1){continue;}
                    Pair<Integer, Integer> step_to = possible_steps.get(step);
                    particles[i][j] -= 1;
                    particles[step_to.first][step_to.second] += 1;
                }
            }
        }
        // Inform the Observer.
        visible.stream().forEach(pos -> observer.observe_field(pos, particles[pos.first][pos.second]));
    }

    // List of positions on a board with certain height and width.
    public static List<Pair<Integer, Integer>> all_positions(int height, int width){
        List<Pair<Integer, Integer>> all_positions = new ArrayList<>(height * width);
        IntStream.range(0, height).forEach(i ->
                IntStream.range(0, width).forEach(j ->
                        all_positions.add(new Pair(i, j))));
        return all_positions;
    }

    // returns possible positions that can be reached from a starting position
    public List<Pair<Integer, Integer>> possible_steps(Pair<Integer, Integer> from){
        int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        return Arrays.stream(directions).map(dir -> new Pair<>(from.first + dir[0], from.second + dir[1])).
                filter(pos -> position_possible(pos)).collect(Collectors.toList());
    }

    // make a copy of the field
    private int[][] copy_particles(){
        int[][] new_field = new int[height][width];
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++){
                new_field[i][j] = particles[i][j];
            }
        }
        return new_field;
    }


    public boolean is_rock(Pair<Integer, Integer> position){ return rocks.contains(position); }
    public boolean is_particle(Pair<Integer, Integer> position){ return particles[position.first][position.second] > 0;}
    public boolean is_visible(Pair<Integer, Integer> position){ return visible.contains(position);}


    // Alternative signature for some public functions
    public boolean is_rock(int x, int y){ return is_rock(new Pair(x, y)); }
    public boolean is_particle(int x, int y){ return is_particle(new Pair(x, y));}
    public boolean is_visible(int x, int y){ return is_visible(new Pair(x, y));}
    public boolean switch_particle(int x, int y){return switch_particle(new Pair(x, y));}
    public boolean switch_rock(int x, int y){return switch_rock(new Pair(x, y));}
    public void switch_visible(int x, int y){switch_visible(new Pair(x, y));}

}
