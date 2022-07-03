import java.util.LinkedList;
import java.util.List;

/*
The Memory remembers previous states as Snapshots.
A Snapshot consists of (copied) Board and Observer.
 */
public class Memory {
    /**
     * The number of time steps to be memorized.
     */
    static final int NUM_STEPS = 20;
    /**
     * The memorized Snapshots in chronological order.
     */
    private List<Snapshot> history;
    /**
     * Number of the active Snapshot.
     */
    private int moment = 0;

    /**
     * @param initialBoard Board for the initial Snapshot.
     * @param initialObserver Observer for the initial Snapshot.
     */
    public Memory(final Board initialBoard, final Observer initialObserver) {
        history = new LinkedList<>();
        log(initialBoard, initialObserver);
    }

    /**
     * @return active Board.
     */
    public Board getBoard() {
        return history.get(accessAt()).getBoard();
    }

    /**
     * @return active Observer.
     */
    public Observer getObserver() {
        return history.get(accessAt()).getObserver();
    }

    /**
     * Extends the memory with new time step.
     * @param board Board for the new Snapshot.
     * @param observer Observer for the new Snapshot.
     */
    public void log(final Board board, final Observer observer) {
        history.add(new Snapshot(board, observer));
        if (history.size() > NUM_STEPS) {
            history.remove(0);
        }
        moment = 0;
    }

    /**
     * Step forward in time if later Snapshot in memory.
     * @return true iff step forward could be done.
     */
    public boolean stepForward() {
        if (moment < 0) {
            moment++;
            return true;
        }
        return false;
    }

    /**
     * Step backward in time if earlier Snapshot in memory.
     * @return true iff step backward could be done.
     */
    public boolean stepBackward() {
        if (accessAt() > 0) {
            moment--;
            return true;
        }
        return false;
    }

    /**
     * Call when active state has been manipulated.
     * Throws away the future after active state.
     */
    public void futureChanged() {
        history.subList(accessAt() + 1, history.size()).clear();
        moment = 0;
    }

    // Translates moment in time into list index.

    /**
     * Transforms the moment (<= 0) into list index.
     * @return Index of the active Snapshot.
     */
    private int accessAt() {
        return history.size() + moment - 1;
    }


    // Snapshot of board and observer
    private class Snapshot {
        private Board board;
        private Observer observer;
        public Snapshot(final Board board, final Observer observer) {
            this.board = board;
            this.observer = observer;
        }
        public Board getBoard() {
            return board;
        }
        public Observer getObserver() {
            return observer;
        }
    }
}
