import java.util.LinkedList;
import java.util.List;

/*
The Memory remembers previous states as Snapshots.
A Snapshot consists of (copied) Board and Observer.
 */
public class Memory {
    static final int num_steps = 20;
    List<Snapshot> history;
    int moment = 0;

    public Memory(Board initial_board, Observer initial_observer){
        history = new LinkedList<>();
        log(initial_board, initial_observer);
    }

    // returns Board at the moment
    public Board get_board(){
        return history.get(access_at()).get_board();
    }

    // returns Observer at the moment
    public Observer get_observer(){
        return history.get(access_at()).get_observer();
    }

    // Extends history
    public void log(Board board, Observer observer){
        history.add(new Snapshot(board, observer));
        if (history.size() > num_steps){
            history.remove(0);
        }
        moment = 0;
    }

    // step forward in time
    public boolean step_forward(){
        if (moment < 0){
            moment ++;
            return true;
        }
        return false;
    }

    // step backward in time
    public boolean step_backward(){
        if (access_at() > 0){
            moment --;
            return true;
        }
        return false;
    }


    // Through away future after the moment
    public void future_changed(){
        history.subList(access_at() + 1, history.size()).clear();
        moment = 0;
    }

    // Translates moment in time into list index.
    private int access_at(){return history.size() + moment - 1;}


    // Snapshot of board and observer
    private class Snapshot{
        Board board;
        Observer observer;
        public Snapshot(Board board, Observer observer){
            this.board = board;
            this.observer = observer;
        }
        public Board get_board(){
            return board;
        }
        public Observer get_observer(){
            return observer;
        }
    }

}
