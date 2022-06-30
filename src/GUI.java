import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/*
Visualizes the Board and the Observer's probability distribution.
Enables user interaction.
 */
public class GUI {
    private Board board;
    private Observer observer;
    private Memory memory;

    private Font font = new Font("Arial", Font.PLAIN, 20);
    // Used colors
    private static final Color COLOR_GRID = new Color(203, 255, 231);
    private static final Color COLOR_PARTICLE = new Color(110, 125, 255);
    private static final Color COLOR_ROCK = new Color(90, 50, 60);
    private static final Color COLOR_VISIBLE = new Color(255, 200, 100);
    private static final Color COLOR_INACTIVE_BUTTON = new Color(255, 255, 255);

    private static final Color COLOR_ABSENCE = new Color(255, 255, 255);

    private static final Color COLOR_PRESENCE = new Color(140, 40, 50);
    private static final Color COLOR_BACKWARD = new Color(49, 160, 150);
    private static final Color COLOR_FORWARD = new Color(142, 242, 196);

    // general structure
    private JFrame frame = new JFrame();
    private JPanel grid = new JPanel();
    private JPanel controlPanel = new JPanel();

    // buttons controlling GUI-states
    private static GUI.PlaceItem placeItem = GUI.placeItem.Particle;
    private JButton buttonParticle;
    private JButton buttonRock;
    private JButton buttonVisible;

    // More buttons
    private JButton buttonBackward;
    private JButton buttonForward;
    private JButton[][] gridButtons;

    enum PlaceItem {Particle, Rock, Visible};

    public GUI(final int rows, final int columns, final Board board, final Observer observer) {
        this.board = board;
        this.observer = observer;
        memory = new Memory(board, observer);


        // Create buttons and assign their functionality
        buttonParticle = new JButton("Teilchen");
        buttonParticle.setFont(font);
        buttonParticle.setBackground(COLOR_PARTICLE);
        buttonParticle.addActionListener(new SwitchParticleMode(buttonParticle));

        buttonRock = new JButton("Barriere");
        buttonRock.setFont(font);
        buttonRock.setBackground(COLOR_INACTIVE_BUTTON);
        buttonRock.addActionListener(new SwitchRockMode(buttonRock));

        buttonVisible = new JButton("Sichtbarkeit");
        buttonVisible.setFont(font);
        buttonVisible.setBackground(COLOR_INACTIVE_BUTTON);
        buttonVisible.addActionListener(new SwitchVisibleMode(buttonVisible));

        buttonBackward = new JButton("Zurück");
        buttonBackward.setFont(font);
        buttonBackward.setBackground(COLOR_BACKWARD);
        buttonBackward.addActionListener(new StepBackward());

        buttonForward = new JButton("Vor");
        buttonForward.setFont(font);
        buttonForward.setBackground(COLOR_FORWARD);
        buttonForward.addActionListener(new StepForward());

        gridButtons = new JButton[rows][columns];
        // Add buttons to grid
        grid.setLayout(new GridLayout(rows, columns));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                gridButtons[i][j] = new JButton("");
                gridButtons[i][j].setBackground(COLOR_GRID);
                gridButtons[i][j].setBackground(COLOR_GRID);
                gridButtons[i][j].addActionListener(new GridListener(i, j, gridButtons[i][j]));
                grid.add(gridButtons[i][j]);
            }
        }

        // Find size for the frame and grid
        int screenX = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenY = Toolkit.getDefaultToolkit().getScreenSize().height;
        final int frameSize = 4;
        final int border = 10;
        final double fillScreen = 0.95;
        int shrinkToSquare = screenX / frameSize;
        frame.setSize(new Dimension((int) (fillScreen * screenX), (int) (fillScreen * screenY)));
        grid.setBorder(BorderFactory.createEmptyBorder(border, shrinkToSquare, border, shrinkToSquare));

        int distY = screenY / 15;
        int distx = screenX / 4;
        controlPanel.setBorder(BorderFactory.createEmptyBorder(distY, distx, 2 * distY, distx));
        controlPanel.setLayout(new GridLayout(1, 1));

        controlPanel.add(buttonRock);
        controlPanel.add(buttonParticle);
        controlPanel.add(buttonVisible);
        controlPanel.add(buttonBackward);
        controlPanel.add(buttonForward);

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
        frame.add(controlPanel, constr);

        // final settings for the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("particles with probabilities");
        frame.setVisible(true);

        updateGUI();
    }


    // Enables user to manipulate the board by clicking a field.
    class GridListener implements ActionListener {
        private int i, j;
        private JButton myButton;
        GridListener(final int i, final int j, final JButton button) {
            this.i = i;
            this.j = j;
            this.myButton = button;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            boolean changed = true;
            switch(placeItem) {
                case Rock:
                    if (!board.switch_rock(i, j)) {
                        changed = false;
                    }
                    break;
                case Particle:
                    if (!board.switch_particle(i, j)) {
                        changed = false;
                    }
                    break;
                case Visible:
                    board.switch_visible(i, j);
                default:
            }
            if (changed) {
                memory.future_changed();
            }
            updateGUI();
        }
    }

    // Enables particle_mode. Allows user to add and delete particles.
    class SwitchParticleMode implements ActionListener {
        private JButton button;
        SwitchParticleMode(final JButton button) {
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            placeItem = GUI.placeItem.Particle;
            buttonParticle.setBackground(COLOR_PARTICLE);
            buttonRock.setBackground(COLOR_INACTIVE_BUTTON);
            buttonVisible.setBackground(COLOR_INACTIVE_BUTTON);
            updateGUI();
        }
    }

    // Enables rock_mode. Allows user to add and delete rocks.
    class SwitchRockMode implements ActionListener {
        private JButton button;
        SwitchRockMode(final JButton button) {
            this.button = button;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            placeItem = GUI.placeItem.Rock;
            buttonParticle.setBackground(COLOR_INACTIVE_BUTTON);
            buttonRock.setBackground(COLOR_ROCK);
            buttonVisible.setBackground(COLOR_INACTIVE_BUTTON);
            updateGUI();
        }
    }

    // Enables visible_mode. Allows user to make fields visible or invisible.
    class SwitchVisibleMode implements ActionListener {
        private JButton button;
        SwitchVisibleMode(final JButton button) {
            this.button = button;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            placeItem = GUI.placeItem.Visible;
            buttonParticle.setBackground(COLOR_INACTIVE_BUTTON);
            buttonRock.setBackground(COLOR_INACTIVE_BUTTON);
            buttonVisible.setBackground(COLOR_VISIBLE);
            updateGUI();
        }
    }

    // Step forward in time. Take either from memory or compute new step.
    class StepForward implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (!memory.step_forward()) {
                Board clonedBoard = board.clone();
                Observer clonedObserver = observer.clone();
                clonedBoard.connect_observer(clonedObserver);
                clonedObserver.connect_board(clonedBoard);
                clonedBoard.step_in_time();
                memory.log(clonedBoard, clonedObserver);
            }
            board = memory.get_board();
            observer = memory.get_observer();
            updateGUI();
        }
    }

    // Step backward in time if possible with memory.
    class StepBackward implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            memory.step_backward();
            board = memory.get_board();
            observer = memory.get_observer();
            updateGUI();
        }
    }

    // Update the color of each field
    public void updateGUI() {
        for (int i = 0; i < gridButtons.length; i++) {
            for (int j = 0; j < gridButtons[i].length; j++) {

                JButton button = gridButtons[i][j];
                Color presence = range(observer.probability(new Pair<>(i, j)));
                button.setBackground(presence);

                if (board.is_rock(i, j)) {
                    button.setBackground(COLOR_ROCK);
                    continue;
                }

                if (board.is_particle(i, j)) {
                    button.setBorder(BorderFactory.createLineBorder(presence, 6));
                    button.setBackground(COLOR_PARTICLE);
                    continue;
                }

                if (board.is_visible(i, j)) {
                    button.setBorder(BorderFactory.createLineBorder(COLOR_VISIBLE, 5));
                    button.setBackground(presence);
                } else {
                    button.setBorder(BorderFactory.createEmptyBorder());
                    button.setBackground(presence);
                }
            }
        }
    }

    // Translates probability into color.
    public Color range(final double prob) {
        final double power = 0.5;
        double mix = Math.pow(prob, power);
        int cR = (int) (COLOR_ABSENCE.getRed() * (1 - mix) + COLOR_PRESENCE.getRed() * mix);
        int cG = (int) (COLOR_ABSENCE.getGreen() * (1 - mix) + COLOR_PRESENCE.getGreen() * mix);
        int cB = (int) (COLOR_ABSENCE.getBlue() * (1 - mix) + COLOR_PRESENCE.getBlue() * mix);
        return new Color(cR, cG, cB);
    }
}
