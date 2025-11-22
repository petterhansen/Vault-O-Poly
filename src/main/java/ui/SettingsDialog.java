package ui;

import util.WebAudioPlayer;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SettingsDialog extends JDialog {

    public SettingsDialog(Frame owner) {
        super(owner, "Settings", true);

        getContentPane().setBackground(GameWindow.DARK_BACKGROUND);
        setLayout(new BorderLayout(10, 10));
        setSize(450, 420);
        setLocationRelativeTo(owner);

        JLabel title = new JLabel("VAULT-O-POLY SETTINGS", SwingConstants.CENTER);
        title.setFont(GameWindow.FALLOUT_FONT.deriveFont(16f));
        title.setForeground(GameWindow.FALLOUT_GREEN);
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(GameWindow.DARK_BACKGROUND);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // Volume Panel
        JPanel volumePanel = new JPanel(new GridLayout(3, 1, 10, 5));
        volumePanel.setBackground(GameWindow.DARK_BACKGROUND);

        JLabel sfxLabel = new JLabel("Sound Effects Volume");
        sfxLabel.setFont(GameWindow.FALLOUT_FONT);
        sfxLabel.setForeground(Color.WHITE);
        volumePanel.add(sfxLabel);

        int currentVol = (int) (WebAudioPlayer.getVolume() * 100);
        JSlider sfxSlider = new JSlider(0, 100, currentVol);
        sfxSlider.setBackground(GameWindow.DARK_BACKGROUND);
        sfxSlider.setForeground(GameWindow.FALLOUT_GREEN);
        sfxSlider.addChangeListener(e -> {
            float vol = sfxSlider.getValue() / 100f;
            WebAudioPlayer.setVolume(vol);
        });
        volumePanel.add(sfxSlider);

        // CHANGED: Use UIHelper
        JButton testBtn = UIHelper.createFalloutButton("Test Sound");
        testBtn.addActionListener(e -> {
            try {
                util.WebAudioPlayer.play(getClass().getResource("/sounds/click.wav").toString());
            } catch (Exception ex) {}
        });
        volumePanel.add(testBtn);
        contentPanel.add(volumePanel, BorderLayout.NORTH);

        // Cache Maintenance Panel
        JPanel cacheMainPanel = new JPanel();
        cacheMainPanel.setLayout(new BoxLayout(cacheMainPanel, BoxLayout.Y_AXIS));
        cacheMainPanel.setBackground(GameWindow.DARK_BACKGROUND);

        cacheMainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel cacheLabel = new JLabel("Cache Maintenance");
        cacheLabel.setFont(GameWindow.FALLOUT_FONT);
        cacheLabel.setForeground(Color.WHITE);
        cacheMainPanel.add(cacheLabel);
        cacheMainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        cacheMainPanel.add(createCacheRow("Video/Temp Cache (Raw)", CacheType.VIDEO_TEMP));
        cacheMainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        cacheMainPanel.add(createCacheRow("GIF Cache (Processed)", CacheType.GIF_PERSISTENT));

        contentPanel.add(cacheMainPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(GameWindow.DARK_BACKGROUND);

        // CHANGED: Use UIHelper
        JButton closeBtn = UIHelper.createFalloutButton("Close");
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::revalidate);
    }

    // --- New/Modified Helper Methods ---

    /**
     * Helper method to format bytes into KB/MB/Bytes string.
     */
    private String sizeToDisplay(long bytes) {
        if (bytes == 0) return "0 B";
        if (bytes < 1024) return bytes + " B";

        double size = (double) bytes;

        if (size >= 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
        return String.format("%.2f KB", size / 1024.0);
    }

    /**
     * Helper to recursively calculate size without deletion.
     * Returns: [deleted count, deleted size in bytes]
     */
    private long[] getFolderContentsInfo(File directory, String prefix) {
        if (!directory.exists()) {
            return new long[]{0, 0};
        }

        long count = 0;
        long size = 0;

        File[] files = directory.listFiles();

        if (files != null) {
            for (File f : files) {
                boolean matchesPrefix = prefix.isEmpty() || f.getName().startsWith(prefix);

                if (f.isDirectory()) {
                    long[] subdirResults = getFolderContentsInfo(f, "");
                    count += subdirResults[0];
                    size += subdirResults[1];
                    count++; // Count the directory itself
                } else if (matchesPrefix) {
                    size += f.length();
                    count++;
                }
            }
        }
        return new long[]{count, size};
    }

    /**
     * Creates a cache row including the dynamically calculated size.
     */
    private JPanel createCacheRow(String labelText, CacheType type) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setBackground(GameWindow.DARK_BACKGROUND);

        long[] info = getCacheInfo(type);
        String sizeText = sizeToDisplay(info[1]);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        infoPanel.setBackground(GameWindow.DARK_BACKGROUND);

        JLabel label = new JLabel(labelText);
        label.setFont(GameWindow.FALLOUT_FONT.deriveFont(12f));
        label.setForeground(Color.WHITE);
        infoPanel.add(label);

        JLabel sizeLabel = new JLabel("Size: " + sizeText + " (" + info[0] + " items)");
        sizeLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(10f));
        sizeLabel.setForeground(GameWindow.FALLOUT_GREEN.darker());
        infoPanel.add(sizeLabel);

        row.add(infoPanel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(GameWindow.DARK_BACKGROUND);

        Dimension smallButtonSize = new Dimension(85, 25);

        // CHANGED: Use UIHelper
        JButton folderBtn = UIHelper.createFalloutButton("To Folder");
        folderBtn.setFont(GameWindow.FALLOUT_FONT.deriveFont(11f));
        folderBtn.setPreferredSize(smallButtonSize);
        folderBtn.addActionListener(e -> showFolder(type));
        buttonPanel.add(folderBtn);

        JButton clearBtn = UIHelper.createFalloutButton("Clear");
        clearBtn.setFont(GameWindow.FALLOUT_FONT.deriveFont(11f));
        clearBtn.setPreferredSize(smallButtonSize);
        clearBtn.addActionListener(e -> {
            clearCache(type);
            SwingUtilities.invokeLater(this::revalidate);
            SwingUtilities.invokeLater(this::repaint);
        });
        buttonPanel.add(clearBtn);

        row.add(buttonPanel, BorderLayout.EAST);
        row.setPreferredSize(new Dimension(getWidth(), 50));

        return row;
    }

    // --- Cache Logic and Helpers ---
    private enum CacheType {
        VIDEO_TEMP,
        GIF_PERSISTENT
    }

    private long[] getCacheInfo(CacheType type) {
        File directory = null;
        String prefix = "";

        switch (type) {
            case VIDEO_TEMP:
                directory = new File(System.getProperty("java.io.tmpdir"));
                prefix = "vop_";
                break;
            case GIF_PERSISTENT:
                directory = getPersistentCacheDir();
                prefix = "";
                break;
            default:
                return new long[]{0, 0};
        }
        return getFolderContentsInfo(directory, prefix);
    }

    private File getPersistentCacheDir() {
        String path = System.getProperty("user.home") + File.separator + "VaultOPolyCache" + File.separator + "gifs";
        return new File(path);
    }

    private long[] deleteCacheFiles(File directory, String prefix) {
        if (!directory.exists()) {
            return new long[]{0, 0};
        }

        long count = 0;
        long size = 0;

        File[] files = directory.listFiles();

        if (files != null) {
            for (File f : files) {
                boolean matchesPrefix = prefix.isEmpty() || f.getName().startsWith(prefix);

                if (f.isDirectory()) {
                    long[] subdirResults = deleteCacheFiles(f, "");
                    count += subdirResults[0];
                    size += subdirResults[1];
                    if (f.delete()) count++;

                } else if (matchesPrefix) {
                    size += f.length();
                    if (f.delete()) {
                        count++;
                    }
                }
            }
        }
        return new long[]{count, size};
    }


    private void clearCache(CacheType type) {
        long[] results;
        String name;

        switch (type) {
            case VIDEO_TEMP:
                name = "Video/Temp Cache";
                results = deleteCacheFiles(new File(System.getProperty("java.io.tmpdir")), "vop_");
                break;
            case GIF_PERSISTENT:
                name = "GIF Cache";
                results = deleteCacheFiles(getPersistentCacheDir(), "");
                break;
            default:
                return;
        }

        long totalCount = results[0];
        long totalSize = results[1];

        String sizeMsg;
        if (totalSize > 1024 * 1024) {
            sizeMsg = String.format("%.2f MB", totalSize / (1024.0 * 1024.0));
        } else if (totalSize > 1024) {
            sizeMsg = String.format("%.2f KB", totalSize / 1024.0);
        } else {
            sizeMsg = totalSize + " Bytes";
        }

        JOptionPane.showMessageDialog(this,
                String.format("%s cleared successfully:\n- Deleted %d files/folders.\n- Reclaimed: %s", name, totalCount, sizeMsg),
                "Cache Cleared",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFolder(CacheType type) {
        File targetDir = null;

        switch (type) {
            case VIDEO_TEMP:
                targetDir = new File(System.getProperty("java.io.tmpdir"));
                break;
            case GIF_PERSISTENT:
                targetDir = getPersistentCacheDir();
                // Ensure the persistent folder exists before trying to open it
                if (!targetDir.exists()) targetDir.mkdirs();
                break;
        }

        if (targetDir != null && targetDir.exists()) {
            try {
                // Uses the operating system's default file explorer
                Desktop.getDesktop().open(targetDir);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to open directory. Path: " + targetDir.getAbsolutePath(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Cache directory does not exist yet.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void styleButton(JButton btn) {
        btn.setFont(GameWindow.FALLOUT_FONT);
        btn.setBackground(new Color(30, 50, 35));
        btn.setForeground(GameWindow.FALLOUT_GREEN);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(GameWindow.FALLOUT_GREEN));
    }
}