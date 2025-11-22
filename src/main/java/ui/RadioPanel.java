package ui;

import radio.RadioController;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RadioPanel extends JPanel {

    private RadioController radio;
    private JLabel stationLabel;
    private JLabel trackLabel;
    private JSlider volumeSlider;
    private JProgressBar signalStrength;
    private JPanel screenPanel;
    private Timer visualizerTimer;

    public RadioPanel() {
        this.radio = new RadioController();

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(40, 40, 40)); // Dark casing color

        // Outer casing border
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.RAISED, new Color(80, 80, 80), new Color(20, 20, 20)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // --- Top: CRT Screen Area ---
        screenPanel = new JPanel();
        screenPanel.setLayout(new BoxLayout(screenPanel, BoxLayout.Y_AXIS));
        screenPanel.setBackground(Color.BLACK); // Screen background

        // Screen Bezel
        screenPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 70, 65), 3),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        stationLabel = new JLabel(radio.getCurrentStationName());
        stationLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(16f));
        stationLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        stationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        stationLabel.setHorizontalAlignment(SwingConstants.CENTER);

        trackLabel = new JLabel("---");
        trackLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(12f));
        trackLabel.setForeground(GameWindow.FALLOUT_GREEN);
        trackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        trackLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Fake Signal Visualizer
        signalStrength = new JProgressBar(0, 100);
        signalStrength.setPreferredSize(new Dimension(200, 6));
        signalStrength.setForeground(GameWindow.FALLOUT_GREEN);
        signalStrength.setBackground(new Color(20, 30, 20)); // Dark green-black
        signalStrength.setBorder(null);
        signalStrength.setAlignmentX(Component.CENTER_ALIGNMENT);
        signalStrength.setValue(0);

        screenPanel.add(stationLabel);
        screenPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        screenPanel.add(trackLabel);
        screenPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        screenPanel.add(signalStrength);

        add(screenPanel, BorderLayout.CENTER);

        // --- Bottom: Controls ---
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBackground(new Color(40, 40, 40));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Tuning Buttons
        JPanel buttonBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonBox.setBackground(new Color(40, 40, 40));

        JButton pwrBtn = createKnobButton("PWR", new Color(255, 80, 80));
        pwrBtn.addActionListener(e -> toggleRadioPower());

        JButton tuneBtn = createKnobButton("TUNE", GameWindow.NEW_VEGAS_ORANGE);
        tuneBtn.addActionListener(e -> {
            radio.nextStation();
            updateScreenState();
        });

        buttonBox.add(pwrBtn);
        buttonBox.add(tuneBtn);
        controlPanel.add(buttonBox, BorderLayout.NORTH);

        // Volume Slider
        JPanel volPanel = new JPanel(new BorderLayout(5, 0));
        volPanel.setBackground(new Color(40, 40, 40));

        JLabel volIcon = new JLabel("VOL ");
        volIcon.setFont(GameWindow.FALLOUT_FONT.deriveFont(10f));
        volIcon.setForeground(Color.LIGHT_GRAY);

        volumeSlider = new JSlider(0, 100, 100);
        volumeSlider.setBackground(new Color(40, 40, 40));
        volumeSlider.setForeground(Color.GRAY);
        volumeSlider.setFocusable(false);
        volumeSlider.addChangeListener(e -> radio.setVolume(volumeSlider.getValue() / 100f));

        volPanel.add(volIcon, BorderLayout.WEST);
        volPanel.add(volumeSlider, BorderLayout.CENTER);

        controlPanel.add(volPanel, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.SOUTH);

        // --- Setup Listeners ---
        radio.setListener(new RadioController.RadioUpdateListener() {
            @Override
            public void onTrackChanged(String title) {
                SwingUtilities.invokeLater(() -> {
                    trackLabel.setText(title);
                    updateScreenState();
                });
            }
            @Override
            public void onStationChanged(String stationName) {
                SwingUtilities.invokeLater(() -> stationLabel.setText(stationName));
            }
        });

        // Animation Timer for the "Signal" bar
        visualizerTimer = new Timer(50, e -> { // Faster refresh (50ms) for smoothness
            if (trackLabel.getText().equals("OFFLINE")) {
                signalStrength.setValue(0);
            } else {
                // --- NEW: Get REAL Audio Level ---
                float level = radio.getCurrentLevel();

                // Convert 0.0-1.0 to 0-100
                int barValue = (int) (level * 100);

                // Add a tiny bit of random noise for that "analog radio" feel
                if (barValue > 5) {
                    barValue += (Math.random() * 10) - 5;
                }

                signalStrength.setValue(Math.max(0, Math.min(100, barValue)));
            }
        });

        updateScreenState();
    }

    private void toggleRadioPower() {
        radio.togglePower();
        updateScreenState();
    }

    private void updateScreenState() {
        boolean isOffline = trackLabel.getText().equals("OFFLINE");

        if (isOffline) {
            visualizerTimer.stop();
            signalStrength.setValue(0);
            stationLabel.setForeground(Color.DARK_GRAY);
            trackLabel.setForeground(Color.GRAY);
        } else {
            if (!visualizerTimer.isRunning()) visualizerTimer.start();
            stationLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
            trackLabel.setForeground(GameWindow.FALLOUT_GREEN);
        }
    }

    /**
     * Creates a button styled to look like a physical plastic button/knob.
     */
    private JButton createKnobButton(String text, Color textColor) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(70, 35));
        btn.setFont(GameWindow.FALLOUT_FONT.deriveFont(12f));
        btn.setBackground(new Color(60, 60, 60));
        btn.setForeground(textColor);

        // 3D Bevel Border
        btn.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        btn.setFocusPainted(false);

        // "Press" effect
        btn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                try {
                    // Play click sound if file exists
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/click.wav").toString());
                } catch (Exception ex) {}

                btn.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
                btn.setBackground(new Color(40, 40, 40));
            }
            public void mouseReleased(MouseEvent e) {
                btn.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
                btn.setBackground(new Color(60, 60, 60));
            }
        });

        return btn;
    }

    public void turnOffRadio() {
        if (radio != null) {
            radio.turnOff();
            visualizerTimer.stop();
            signalStrength.setValue(0);
        }
    }
}