import java.util.LinkedList;
import java.util.List;

/*
The Memory remembers previous states as Snapshots.
A Snapshot consists of (copied) Board and Observer.
 */
public class Memory {
    static final int NUM_STEPS = 20;
    private List<Snapshot> history;
    private int moment = 0;

    public Memory(final Board initialBoard, final Observer initialObserver) {
        history = new LinkedList<>();
        log(initialBoard, initialObserver);
    }

    // returns Board at the moment
    public Board getBoard() {
        return history.get(accessAt()).getBoard();
    }

    // returns Observer at the moment
    public Observer getObserver() {
        return history.get(accessAt()).getObserver();
    }

    // Extends history
    public void log(final Board board, final Observer observer) {
        history.add(new Snapshot(board, observer));
        if (history.size() > NUM_STEPS) {
            history.remove(0);
        }
        moment = 0;
    }

    // step forward in time
    public boolean stepForward() {
        if (moment < 0) {
            moment++;
            return true;
        }
        return false;
    }

    // step backward in time
    public boolean stepBackward() {
        if (accessAt() > 0) {
            moment--;
            return true;
        }
        return false;
    }


    // Through away future after the moment
    public void futureChanged() {
        history.subList(accessAt() + 1, history.size()).clear();
        moment = 0;
    }

    // Translates moment in time into list index.
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
