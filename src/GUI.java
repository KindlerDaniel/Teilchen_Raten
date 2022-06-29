/*
Updated whenever the Board or the Observer change state.
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/*
Visualizes the Board and the Observer's probability distribution.
Enables user interaction.
 */
public class GUI {
    Board board;
    Observer observer;
    Memory memory;

    Font font = new Font("Arial", Font.PLAIN, 20);
    // Used colors
    private static final Color color_grid = new Color(203, 255, 231);
    private static final Color color_particle = new Color(110, 125, 255);
    private static final Color color_rock = new Color(90, 50, 60);
    private static final Color color_visible = new Color(255, 200, 100);
    private static final Color color_inactive_button = new Color(255, 255, 255);
    private static final Color absence_color = new Color(255, 255, 255);
    private static final Color presence_color = new Color(140, 40, 50);
    private static final Color color_backward = new Color(49, 160, 150);
    private static final Color color_forward = new Color(142, 242, 196);

    // general structure
    private JFrame frame = new JFrame();
    private JPanel grid = new JPanel();
    JPanel control_panel = new JPanel();

    // buttons controlling GUI-states
    Place_item place_item = Place_item.Particle;
    JButton button_particle;
    JButton button_rock;
    JButton button_visible;

    // More buttons
    JButton button_backward;
    JButton button_forward;
    private JButton[][] grid_buttons;

    enum Place_item {Particle, Rock, Visible};

    public GUI(int rows, int columns, Board board, Observer observer) {
        this.board = board;
        this.observer = observer;
        memory = new Memory(board, observer);


        // Create buttons and assign their functionality
        button_particle = new JButton("Teilchen");
        button_particle.setFont(font);
        button_particle.setBackground(color_particle);
        button_particle.addActionListener(new Switch_particle_mode(button_particle));

        button_rock = new JButton("Barriere");
        button_rock.setFont(font);
        button_rock.setBackground(color_inactive_button);
        button_rock.addActionListener(new Switch_rock_mode(button_rock));

        button_visible = new JButton("Sichtbarkeit");
        button_visible.setFont(font);
        button_visible.setBackground(color_inactive_button);
        button_visible.addActionListener(new Switch_visible_mode(button_visible));

        button_backward = new JButton("Zurück");
        button_backward.setFont(font);
        button_backward.setBackground(color_backward);
        button_backward.addActionListener(new Step_backward());

        button_forward = new JButton("Vor");
        button_forward.setFont(font);
        button_forward.setBackground(color_forward);
        button_forward.addActionListener(new Step_forward());

        grid_buttons = new JButton[rows][columns];
        // Add buttons to grid
        grid.setLayout(new GridLayout(rows, columns));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                grid_buttons[i][j] = new JButton("");
                grid_buttons[i][j].setBackground(color_grid);
                grid_buttons[i][j].setBackground(color_grid);
                grid_buttons[i][j].addActionListener(new Grid_listener(i, j, grid_buttons[i][j]));
                grid.add(grid_buttons[i][j]);
            }
        }

        // Find size for the frame and grid
        int screen_x = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_y = Toolkit.getDefaultToolkit().getScreenSize().height;
        int shrink_to_square = screen_x / 4;
        frame.setSize(new Dimension((int) (0.95 * screen_x), (int) (0.95 * screen_y)));
        grid.setBorder(BorderFactory.createEmptyBorder(10, shrink_to_square, 10, shrink_to_square));

        int dist_y = screen_y / 15;
        int dist_x = screen_x / 4;
        control_panel.setBorder(BorderFactory.createEmptyBorder(dist_y, dist_x, 2 * dist_y, dist_x));
        control_panel.setLayout(new GridLayout(1, 1));

        control_panel.add(button_rock);
        control_panel.add(button_particle);
        control_panel.add(button_visible);
        control_panel.add(button_backward);
        control_panel.add(button_forward);

        // Add both panels to frame
        frame.setLayout(new GridBagLayout());
        GridBagConstraints constr = new GridBagConstraints();
        constr.fill = GridBagConstraints.BOTH;
        constr.weightx = 1;
        constr.weighty = 4;
        constr.gridx = 0;
        constr.gridy = 0;
        frame.add(grid, constr);
        constr.weightx = 1;
        constr.weighty = 1;
        constr.gridx = 0;
        constr.gridy = 1;
        frame.add(control_panel, constr);

        // final settings for the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("particles with probabilities");
        frame.setVisible(true);

        update_GUI();
    }


    // Enables user to manipulate the board by clicking a field.
    class Grid_listener implements ActionListener {
        private int i, j;
        private JButton myButton;
        public Grid_listener(int i, int j, JButton button){
            this.i = i;
            this.j = j;
            this.myButton = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean changed = true;
            switch(place_item){
                case Rock:
                    if (!board.switch_rock(i, j)){
                        changed = false;
                    }
                    break;
                case Particle:
                    if (!board.switch_particle(i, j)){
                        changed = false;
                    }
                    break;
                case Visible:
                    board.switch_visible(i, j);
            }
            if (changed){
                memory.future_changed();
            }
            update_GUI();
        }
    }

    // Enables particle_mode. Allows user to add and delete particles.
    class Switch_particle_mode implements ActionListener {
        JButton button;
        public Switch_particle_mode(JButton button){
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            place_item = Place_item.Particle;
            button_particle.setBackground(color_particle);
            button_rock.setBackground(color_inactive_button);
            button_visible.setBackground(color_inactive_button);
            update_GUI();
        }
    }

    // Enables rock_mode. Allows user to add and delete rocks.
    class Switch_rock_mode implements ActionListener {
        JButton button;
        public Switch_rock_mode(JButton button){
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            place_item = Place_item.Rock;
            button_particle.setBackground(color_inactive_button);
            button_rock.setBackground(color_rock);
            button_visible.setBackground(color_inactive_button);
            update_GUI();
        }
    }

    // Enables visible_mode. Allows user to make fields visible or invisible.
    class Switch_visible_mode implements ActionListener {
        JButton button;
        public Switch_visible_mode(JButton button){
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            place_item = Place_item.Visible;
            button_particle.setBackground(color_inactive_button);
            button_rock.setBackground(color_inactive_button);
            button_visible.setBackground(color_visible);
            update_GUI();
        }
    }

    // Step forward in time. Take either from memory or compute new step.
    class Step_forward implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!memory.step_forward()){
                Board cloned_board = board.clone();
                Observer cloned_observer = observer.clone();
                cloned_board.connect_observer(cloned_observer);
                cloned_observer.connect_board(cloned_board);
                cloned_board.step_in_time();
                memory.log(cloned_board, cloned_observer);
            }
            board = memory.get_board();
            observer = memory.get_observer();
            update_GUI();
        }
    }

    // Step backward in time if possible with memory.
    class Step_backward implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            memory.step_backward();
            board = memory.get_board();
            observer = memory.get_observer();
            update_GUI();
        }
    }

    // Update the color of each field
    public void update_GUI(){
        for (int i = 0; i < grid_buttons.length; i++){
            for (int j = 0; j < grid_buttons[i].length; j++){

                JButton button = grid_buttons[i][j];
                Color presence = range(observer.probability(new Pair<>(i, j)));
                button.setBackground(presence);

                if (board.is_rock(i, j)){
                    button.setBackground(color_rock);
                    continue;
                }

                if (board.is_particle(i, j)){
                    button.setBorder(BorderFactory.createLineBorder(presence, 6));
                    button.setBackground(color_particle);
                    continue;
                }

                if (board.is_visible(i, j)) {
                    button.setBorder(BorderFactory.createLineBorder(color_visible, 5));
                    button.setBackground(presence);
                } else {
                    button.setBorder(BorderFactory.createEmptyBorder());
                    button.setBackground(presence);
                }
            }
        }
    }

    // Translates probability into color.
    public Color range(double i){
        i = Math.pow(i, 0.5);
        int cR = (int)(absence_color.getRed()*(1-i) + presence_color.getRed()*i);
        int cG = (int)(absence_color.getGreen()*(1-i) + presence_color.getGreen()*i);
        int cB = (int)(absence_color.getBlue()*(1-i) + presence_color.getBlue()*i);
        return new Color(cR, cG, cB);
    }
}
