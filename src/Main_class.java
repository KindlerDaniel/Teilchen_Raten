import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;


public class MainClass {

    // Read the initial state of the Board from JSON file.
    // Instantiate and connect Board and Observer and GUI.
    public static void main(final String[] args) throws JSONException {
        int[][] initialBoard = read_world("world_0");
        int height = initialBoard.length;
        int width = initialBoard[0].length;
        Board board = new Board(height, width);
        Observer observer = new Observer(height, width);
        observer.connect_board(board);
        board.initialize(observer, initialBoard);
        new GUI(height, width, board, observer);
    }

    // Reads from a JSON file named worlds within the same directory.
    // Outputs a matrix representation of encoded board.
    public static int[][] read_world(final String worldName) throws JSONException {
        String raw = "";
        Path path = Path.of(System.getProperty("user.dir") + "/worlds");
        try {
            raw = Files.readString(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject worlds = new JSONObject(raw);
        JSONArray world = worlds.getJSONArray(worldName);
        JSONArray shape = world.getJSONArray(0);
        JSONArray triples = world.getJSONArray(1);
        int[][] matrix = new int[shape.getInt(0)][shape.getInt(1)];
        IntStream.range(0, triples.length()).mapToObj(i -> {
                    try {
                        return triples.getJSONArray(i);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }).
                forEach(t -> {
                    try {
                        matrix[t.getInt(1)][t.getInt(0)] = t.getInt(2);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
        return matrix;
    }
}
