package tools;

// We MUST import the game classes because the serialized data
// is specifically "game.GameSaveState", "players.Player", etc.
// There is no way to deserialize them without these classes present.
import game.GameSaveState;
import players.Player;
import board.fields.BoardField;
import board.fields.PropertyField;
import mechanics.CasinoConfiguration;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * A standalone utility to decrypt and view Vault-O-Poly save files.
 * Includes internal decryption logic so it doesn't depend on util.SecurityUtil.
 */
public class SaveViewer extends JFrame {

    private JTextArea outputArea;

    // --- Internal Security Constants (Copied from SecurityUtil) ---
    private static final String AES_MARKER = "AES";
    private static final String DEFAULT_SEED = "VAULT-TEC-TOP-SECRET-CLEARANCE";

    public SaveViewer() {
        setTitle("Vault-O-Poly Save Inspector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Main Layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(40, 40, 40)); // Dark Gray

        // Header
        JLabel header = new JLabel("Vault-Tec Save Inspector");
        header.setForeground(new Color(255, 255, 255));
        header.setFont(new Font("Monospaced", Font.BOLD, 18));
        header.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(header, BorderLayout.NORTH);

        // Output Area
        outputArea = new JTextArea("Select a .vop file to decrypt...");
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(30, 30, 30));
        outputArea.setForeground(new Color(255, 255, 255));
        outputArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(40, 40, 40));

        JButton openButton = new JButton("Open Save File");
        openButton.setBackground(new Color(60, 60, 60));
        openButton.setForeground(Color.BLACK);
        openButton.setFocusPainted(false);
        openButton.addActionListener(e -> openFile());
        buttonPanel.add(openButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("Vault-O-Poly Saves (*.vop)", "vop"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            decryptAndShow(file);
        }
    }

    private void decryptAndShow(File file) {
        try {
            outputArea.setText("Decrypting " + file.getName() + "...\n");

            // 1. Read Bytes
            String encryptedContent = new String(Files.readAllBytes(file.toPath()));

            // 2. Decrypt (Using internal method)
            byte[] rawData = internalDecrypt(encryptedContent);

            // 3. Deserialize
            ByteArrayInputStream bis = new ByteArrayInputStream(rawData);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object obj = ois.readObject();

            if (obj instanceof GameSaveState) {
                GameSaveState state = (GameSaveState) obj;
                displayState(state, file.getName());
            } else {
                outputArea.append("\nError: File decrypted, but content is not a valid GameSaveState.\n");
                outputArea.append("Found class: " + obj.getClass().getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
            outputArea.append("\n[CRITICAL ERROR]\n" + e.toString());
            for (StackTraceElement el : e.getStackTrace()) {
                outputArea.append("\n  at " + el.toString());
            }
        }
    }

    // --- INTERNAL DECRYPTION LOGIC (No external dependencies) ---
    private byte[] internalDecrypt(String encryptedData) throws Exception {
        String[] parts = encryptedData.split("\\|", 2);
        if (parts.length < 2 || !parts[1].equals(AES_MARKER)) {
            throw new IllegalArgumentException("Invalid save file format (Missing AES marker).");
        }

        byte[] all = Base64.getDecoder().decode(parts[0]);
        byte[] iv = new byte[16];
        System.arraycopy(all, 0, iv, 0, 16);
        byte[] cipherBytes = new byte[all.length - 16];
        System.arraycopy(all, 16, cipherBytes, 0, cipherBytes.length);

        byte[] key = internalDeriveKey(DEFAULT_SEED);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

        return cipher.doFinal(cipherBytes);
    }

    private byte[] internalDeriveKey(String seed) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha.digest(seed.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(digest, 0, key, 0, 16);
        return key;
    }

    private void displayState(GameSaveState state, String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== VAULT-O-POLY SAVE FILE REPORT ===\n");
        sb.append("File: ").append(filename).append("\n");
        sb.append("Turn Index: ").append(state.currentPlayerIndex).append(" (Player ").append(state.currentPlayerIndex + 1).append(")\n");

        if (state.casinoConfig != null) {
            sb.append("\n--- CASINO CONFIGURATION ---\n");
            sb.append("Coinflip: ").append(state.casinoConfig.coinflipCurrency).append("\n");
            sb.append("Blackjack: ").append(state.casinoConfig.blackjackCurrency).append("\n");
            sb.append("Baccarat: ").append(state.casinoConfig.baccaratCurrency).append("\n");
            sb.append("Dice: ").append(state.casinoConfig.diceCurrency).append("\n");
        }

        sb.append("\n--- PLAYER ROSTER (" ).append(state.players.size()).append(") ---\n");

        for (Player p : state.players) {
            sb.append(String.format("\n> %s (%s)\n", p.getName(), p.getToken()));
            sb.append("   Status: ").append(p.isInJail() ? "IN JAIL (Turn " + p.getJailTurns() + ")" : "Active").append("\n");
            sb.append("   Position: ").append(p.getPosition()).append("\n");
            sb.append("   Caps: ").append(p.getBottleCaps()).append("\n");

            sb.append("   Resources: ");
            if (p.getResources().isEmpty()) sb.append("None");
            else {
                p.getResources().forEach((k, v) -> {
                    if (v > 0) sb.append(k).append("(").append(v).append(") ");
                });
            }
            sb.append("\n");

            List<board.fields.PropertyField> props = p.getOwnedProperties();
            sb.append("   Properties (" ).append(props.size()).append("): ");
            if (props.isEmpty()) sb.append("None");
            else {
                sb.append("\n");
                for (board.fields.PropertyField prop : props) {
                    sb.append("     - ").append(prop.getName())
                            .append(" (Lvl ").append(prop.getCurrentImprovementLevel()).append(")")
                            .append("\n");
                }
            }
        }

        sb.append("\n--- GLOBAL BOARD STATE ---\n");
        int ownedCount = 0;
        for (BoardField f : state.fields) {
            if (f instanceof PropertyField && ((PropertyField)f).getOwner() != null) {
                ownedCount++;
            }
        }
        sb.append("Total Fields: ").append(state.fields.size()).append("\n");
        sb.append("Owned Properties: ").append(ownedCount).append("\n");

        outputArea.setText(sb.toString());
        outputArea.setCaretPosition(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SaveViewer().setVisible(true));
    }
}