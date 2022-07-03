import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Visualizes the Board and the Observer's probability distribution.
 * Enables user interaction.
 */
public class GUI {
    /**
     * The Board visible to and manipulated by user.
     */
    private Board board;
    /**
     * The Observer that watches the board.
     */
    private Observer observer;
    /**
     * Memory to save steps in the future and past.
     */
    private Memory memory;

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

    /**
     * The overall window that contains everything.
     */
    private JFrame frame = new JFrame();
    /**
     * Will contain all fields of the board as buttons.
     */
    private JPanel gridPanel = new JPanel();
    /**
     * Will contain control buttons.
     */
    private JPanel controlPanel = new JPanel();

    /**
     * These items can be placed on the grid.
     */
    enum Item { PARTICLE, ROCK, VISIBLE};
    /**
     * Item placed when clicking on the grid.
     * Initialized with PARTICLE.
     */
    private static GUI.Item placeItem = GUI.placeItem.PARTICLE;

    /**
     * Button sets placeItem to PARTICLE.
     */
    private JButton buttonParticle;
    /**
     * Button sets placeItem to ROCK.
     */
    private JButton buttonRock;
    /**
     * Button sets placeItem to VISIBLE.
     */
    private JButton buttonVisible;
    /**
     * Button to go step forward in time.
     */
    private JButton buttonForward;
    /**
     * Button to go step backward in time.
     */
    private JButton buttonBackward;
    /**
     * Grid of Buttons represents the board.
     */
    private JButton[][] gridButtons;
    /**
     * Instantiate buttons and
     * @param rows rows of board.
     * @param columns columns of board.
     * @param board initial Board.
     * @param observer initial Observer.
     */
    public GUI(final int rows, final int columns, final Board board, final Observer observer) {
        this.board = board;
        this.observer = observer;
        memory = new Memory(board, observer);

        // settings for the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Guessing Particles");
        frame.setVisible(true);

        // Instantiate grid buttons and assign their functionality.
        // Then add them to gridPanel
        gridButtons = new JButton[rows][columns];
        gridPanel.setLayout(new GridLayout(rows, columns));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                gridButtons[i][j] = new JButton("");
                gridButtons[i][j].setBackground(COLOR_GRID);
                gridButtons[i][j].setBackground(COLOR_GRID);
                gridButtons[i][j].addActionListener(new GridListener(i, j, gridButtons[i][j]));
                gridPanel.add(gridButtons[i][j]);
            }
        }

        // Define the size of text.
        final int fontSize = 25;
        Font font = new Font("Arial", Font.PLAIN, fontSize);

        // instantiate control buttons and assign their functionality.
        buttonParticle = new JButton("Particle");
        buttonParticle.setFont(font);
        buttonParticle.setBackground(COLOR_PARTICLE);
        buttonParticle.addActionListener(new SwitchParticleMode(buttonParticle));

        buttonRock = new JButton("Barrier");
        buttonRock.setFont(font);
        buttonRock.setBackground(COLOR_INACTIVE_BUTTON);
        buttonRock.addActionListener(new SwitchRockMode(buttonRock));

        buttonVisible = new JButton("Visibility");
        buttonVisible.setFont(font);
        buttonVisible.setBackground(COLOR_INACTIVE_BUTTON);
        buttonVisible.addActionListener(new SwitchVisibleMode(buttonVisible));

        buttonBackward = new JButton("Back");
        buttonBackward.setFont(font);
        buttonBackward.setBackground(COLOR_BACKWARD);
        buttonBackward.addActionListener(new StepBackward());

        buttonForward = new JButton("Next");
        buttonForward.setFont(font);
        buttonForward.setBackground(COLOR_FORWARD);
        buttonForward.addActionListener(new StepForward());

        // Add control buttons to controlPanel.
        controlPanel.add(buttonRock);
        controlPanel.add(buttonParticle);
        controlPanel.add(buttonVisible);
        controlPanel.add(buttonBackward);
        controlPanel.add(buttonForward);


        // Set size and Layout of the frame.
        int screenX = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenY = Toolkit.getDefaultToolkit().getScreenSize().height;
        final int proportionScreen = 4;
        final double fillScreen = 0.95;
        int shrinkToSquare = screenX / proportionScreen;
        frame.setSize(new Dimension((int) (fillScreen * screenX), (int) (fillScreen * screenY)));
        frame.setLayout(new GridBagLayout());


        // Set size of the gridPanel.
        final int proportionBorder = 50;
        final int border = screenX / proportionBorder;
        gridPanel.setBorder(BorderFactory.createEmptyBorder(border, shrinkToSquare, border, shrinkToSquare));

        // Set size and Layout of the controlPanel.
        int distY = screenY / proportionBorder;
        controlPanel.setBorder(BorderFactory.createEmptyBorder(distY, shrinkToSquare, 2 * distY, shrinkToSquare));
        controlPanel.setLayout(new GridLayout(1, 1));


        // Add both panels to frame
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 4;
        constraints.gridx = 0;
        constraints.gridy = 0;
        frame.add(gridPanel, constraints);
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        frame.add(controlPanel, constraints);

        updateGUI();
    }

    /**
     * Clicking on a grid button.
     */
    class GridListener implements ActionListener {
        /**
         * row on the board.
         */
        private int i;
        /**
         * column on the board.
         */
        private int j;
        /**
         * The JButton listened to.
         */
        private JButton myButton;
        GridListener(final int row, final int column, final JButton button) {
            this.i = row;
            this.j = column;
            this.myButton = button;
        }
        @Override
        public void actionPerformed(final ActionEvent e) {
            boolean changed = false;
            switch (placeItem) {
                case ROCK:
                    changed = board.switchRock(i, j);
                    break;
                case PARTICLE:
                    changed = board.switchParticle(i, j);
                    break;
                case VISIBLE:
                    changed = board.switchVisibility(i, j);
                default:
            }
            if (changed) {
                memory.futureChanged();
            }
            updateGUI();
        }
    }

    /**
     * Clicking on the particles Button sets placeItem to PARTICLE.
     */
    class SwitchParticleMode implements ActionListener {
        /**
         * The JButton listened to.
         */
        private JButton myButton;
        SwitchParticleMode(final JButton button) {
            this.myButton = button;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            placeItem = GUI.placeItem.PARTICLE;
            buttonParticle.setBackground(COLOR_PARTICLE);
            buttonRock.setBackground(COLOR_INACTIVE_BUTTON);
            buttonVisible.setBackground(COLOR_INACTIVE_BUTTON);
            updateGUI();
        }
    }

    /**
     * Clicking on the rock Button sets placeItem to ROCK.
     */
    class SwitchRockMode implements ActionListener {
        /**
         * The JButton listened to.
         */
        private JButton myButton;
        SwitchRockMode(final JButton button) {
            this.myButton = button;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            placeItem = GUI.placeItem.ROCK;
            buttonParticle.setBackground(COLOR_INACTIVE_BUTTON);
            buttonRock.setBackground(COLOR_ROCK);
            buttonVisible.setBackground(COLOR_INACTIVE_BUTTON);
            updateGUI();
        }
    }

    /**
     * Clicking on the visibility Button sets placeItem to VISIBLE.
     */
    class SwitchVisibleMode implements ActionListener {
        /**
         * The JButton listened to.
         */
        private JButton myButton;
        SwitchVisibleMode(final JButton button) {
            this.myButton = button;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            placeItem = GUI.placeItem.VISIBLE;
            buttonParticle.setBackground(COLOR_INACTIVE_BUTTON);
            buttonRock.setBackground(COLOR_INACTIVE_BUTTON);
            buttonVisible.setBackground(COLOR_VISIBLE);
            updateGUI();
        }
    }

    /**
     * Clicking on the forward button tells board to step forward in time.
     */
    class StepForward implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            if (!memory.stepForward()) {
                Board clonedBoard = board.clone();
                Observer clonedObserver = observer.clone();
                clonedBoard.connectObserver(clonedObserver);
                clonedObserver.connectBoard(clonedBoard);
                clonedBoard.stepInTime();
                memory.log(clonedBoard, clonedObserver);
            }
            board = memory.getBoard();
            observer = memory.getObserver();
            updateGUI();
        }
    }

    /**
     * Clicking on the backward button tells board to step back in time.
     */
    class StepBackward implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            memory.stepBackward();
            board = memory.getBoard();
            observer = memory.getObserver();
            updateGUI();
        }
    }

    /**
     * Update the color of each grid button.
     */
    public void updateGUI() {
        for (int row = 0; row < gridButtons.length; row++) {
            for (int column = 0; column < gridButtons[row].length; column++) {
                // Button to update
                JButton button = gridButtons[row][column];
                // Set button's color according to probability of particle presence.
                Color presence = range(observer.probability(new Position(row, column)));
                button.setBackground(presence);
                // A rock looks always the same.
                if (board.isRock(row, column)) {
                    button.setBackground(COLOR_ROCK);
                    continue;
                }
                // A field with particle has a border that shows its background.
                if (board.isParticle(row, column)) {
                    button.setBorder(BorderFactory.createLineBorder(presence, 6));
                    button.setBackground(COLOR_PARTICLE);
                    continue;
                }
                // Visible (empty) fields are marked with a yellow border.
                if (board.isVisible(row, column)) {
                    button.setBorder(BorderFactory.createLineBorder(COLOR_VISIBLE, 5));
                } else {
                    button.setBorder(BorderFactory.createEmptyBorder());
                }
            }
        }
    }

    /**
     * Translate probability into color.
     * @param prob probability that a particle is present.
     * @return color that represents probability.
     */
    public Color range(final double prob) {
        final double power = 0.4;
        double mix = Math.pow(prob, power);
        int cR = (int) (COLOR_ABSENCE.getRed() * (1 - mix) + COLOR_PRESENCE.getRed() * mix);
        int cG = (int) (COLOR_ABSENCE.getGreen() * (1 - mix) + COLOR_PRESENCE.getGreen() * mix);
        int cB = (int) (COLOR_ABSENCE.getBlue() * (1 - mix) + COLOR_PRESENCE.getBlue() * mix);
        return new Color(cR, cG, cB);
    }
}
