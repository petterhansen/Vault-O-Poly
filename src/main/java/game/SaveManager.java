package game;

import util.SecurityUtil;
import ui.GameWindow; // For accessing the main window frame if needed
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.*;
import java.nio.file.Files;

public class SaveManager {

    private static final String EXTENSION = "vop"; // .vop (Vault-O-Poly)
    private static final String DESCRIPTION = "Vault-O-Poly Save Files (*.vop)";

    /**
     * Opens a File Chooser dialog to let the user select where to save.
     * @param parent The parent component for the dialog.
     * @return The selected File, or null if cancelled.
     */
    public static File chooseFileToSave(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Game State");
        chooser.setFileFilter(new FileNameExtensionFilter(DESCRIPTION, EXTENSION));
        chooser.setCurrentDirectory(new File(".")); // Start in game folder

        int userSelection = chooser.showSaveDialog(parent);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            // Ensure extension
            if (!file.getName().toLowerCase().endsWith("." + EXTENSION)) {
                file = new File(file.getParentFile(), file.getName() + "." + EXTENSION);
            }
            return file;
        }
        return null;
    }

    /**
     * Opens a File Chooser dialog to let the user select a file to load.
     * @param parent The parent component for the dialog.
     * @return The selected File, or null if cancelled.
     */
    public static File chooseFileToLoad(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Game State");
        chooser.setFileFilter(new FileNameExtensionFilter(DESCRIPTION, EXTENSION));
        chooser.setCurrentDirectory(new File("."));

        int userSelection = chooser.showOpenDialog(parent);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    public static void saveGame(GameController controller, File file) throws Exception {
        // 1. Wrap state in DTO
        GameSaveState state = new GameSaveState(
                controller.getPlayers(),
                controller.getBoard().getFields(),
                controller.getCurrentPlayerIndex(),
                controller.getCasinoConfig()
        );

        // 2. Serialize to Byte Array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(state);
        oos.flush();
        byte[] rawData = bos.toByteArray();

        // 3. Encrypt
        String encryptedContent = SecurityUtil.encrypt(rawData);

        // 4. Write to Disk
        Files.write(file.toPath(), encryptedContent.getBytes());
    }

    public static GameSaveState loadGame(File file) throws Exception {
        if (!file.exists()) return null;

        // 1. Read from Disk
        String encryptedContent = new String(Files.readAllBytes(file.toPath()));

        // 2. Decrypt
        byte[] rawData = SecurityUtil.decrypt(encryptedContent);

        // 3. Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(rawData);
        ObjectInputStream ois = new ObjectInputStream(bis);

        return (GameSaveState) ois.readObject();
    }
}