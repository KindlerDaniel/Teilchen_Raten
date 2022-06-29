import org.json.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;


public class Main_class {

    // Read the initial state of the Board from JSON file.
    // Instantiate and connect Board and Observer and GUI.
    public static void main(String[] args){
        int[][] initial_board = read_world("world_0");
        int height = initial_board.length;
        int width = initial_board[0].length;
        Board board = new Board(height, width);
        Observer observer = new Observer(height, width);
        observer.connect_board(board);
        board.initialize(observer, initial_board);
        new GUI(height, width, board, observer);
    }

    // Reads from a JSON file named worlds within the same directory.
    // Outputs a matrix representation of encoded board.
    public static int[][] read_world(String world_name){
        String raw = "";
        Path path = Path.of(System.getProperty("user.dir") + "\\worlds");
        try{
            raw = Files.readString(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject worlds = new JSONObject(raw);
        JSONArray world = worlds.getJSONArray(world_name);
        JSONArray shape = world.getJSONArray(0);
        JSONArray triples = world.getJSONArray(1);
        int[][] matrix = new int[shape.getInt(0)][shape.getInt(1)];
        IntStream.range(0, triples.length()).mapToObj(i -> triples.getJSONArray(i)).
        forEach(t -> matrix[t.getInt(1)][t.getInt(0)] = t.getInt(2));

        return matrix;
    }
}
