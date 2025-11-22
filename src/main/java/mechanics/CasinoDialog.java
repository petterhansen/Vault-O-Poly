package mechanics;

import players.Player;
import resources.ResourceType;
import ui.GameWindow;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.net.URL;
import java.util.Random;

public class CasinoDialog extends JDialog {

    private Player player;
    private CasinoResult result;
    private Random random = new Random();

    // Currencies are now final and passed in
    private final ResourceType coinflipCurrency;
    private final ResourceType blackjackCurrency;
    private final ResourceType baccaratCurrency;
    private final ResourceType diceCurrency;

    // --- UI Components ---
    private JLabel balanceLabel;
    private JTabbedPane tabbedPane;
    private JLabel houseImageLabel; // The Mr. House image

    // --- Coinflip Components ---
    private JTextArea coinflipOutput;
    private JTextField coinflipBetField;
    private JRadioButton headsBtn;
    private JRadioButton tailsBtn;

    // --- Dice Components ---
    private JTextArea diceOutput;
    private JTextField diceBetField;

    // --- Blackjack Components ---
    private JTextArea bjOutput;
    private JTextField bjBetField;
    private JButton bjDealBtn, bjHitBtn, bjStandBtn;
    private java.util.List<Integer> bjPlayerHand;
    private java.util.List<Integer> bjDealerHand;
    private int bjCurrentBet;
    private boolean bjInRound = false;

    // --- Baccarat Components ---
    private JTextArea bacOutput;
    private JTextField bacBetField;
    private JComboBox<String> bacTypeBox;

    public CasinoDialog(Frame owner, Player player, CasinoConfiguration config) {
        super(owner, "The Lucky 38", true);
        this.player = player;
        this.result = new CasinoResult();

        if (config != null) {
            this.coinflipCurrency = config.coinflipCurrency;
            this.blackjackCurrency = config.blackjackCurrency;
            this.baccaratCurrency = config.baccaratCurrency;
            this.diceCurrency = config.diceCurrency;
        } else {
            this.coinflipCurrency = ResourceType.BOTTLECAPS;
            this.blackjackCurrency = ResourceType.BOTTLECAPS;
            this.baccaratCurrency = ResourceType.BOTTLECAPS;
            this.diceCurrency = ResourceType.BOTTLECAPS;
        }

        setSize(850, 550); // Widen to fit Mr. House
        setLocationRelativeTo(owner);
        getContentPane().setBackground(GameWindow.NEW_VEGAS_BACKGROUND); // New Vegas BG
        setLayout(new BorderLayout());

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("THE LUCKY 38 CASINO", SwingConstants.CENTER);
        titleLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(24f));
        titleLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE); // Orange Theme
        topPanel.add(titleLabel, BorderLayout.NORTH);

        balanceLabel = new JLabel();
        balanceLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
        balanceLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE); // Orange Theme
        balanceLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(balanceLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // --- Left Panel (Mr. House) ---
        add(createHousePanel(), BorderLayout.WEST);

        // --- Center Panel (Games) ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(GameWindow.FALLOUT_FONT.deriveFont(16f));
        tabbedPane.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        tabbedPane.setForeground(GameWindow.NEW_VEGAS_ORANGE); // Orange Theme

        // Style the tabbed pane for New Vegas look
        UIManager.put("TabbedPane.selected", GameWindow.NEW_VEGAS_BACKGROUND);
        UIManager.put("TabbedPane.contentAreaColor", GameWindow.NEW_VEGAS_BACKGROUND);
        UIManager.put("TabbedPane.focus", GameWindow.NEW_VEGAS_BACKGROUND);
        UIManager.put("TabbedPane.borderHightlightColor", GameWindow.NEW_VEGAS_ORANGE);
        UIManager.put("TabbedPane.darkShadow", GameWindow.NEW_VEGAS_BACKGROUND);
        UIManager.put("TabbedPane.light", GameWindow.NEW_VEGAS_BACKGROUND);
        UIManager.put("TabbedPane.selectHighlight", GameWindow.NEW_VEGAS_ORANGE);

        tabbedPane.addTab("Coinflip", createCoinflipPanel());
        tabbedPane.addTab("Blackjack", createBlackjackPanel());
        tabbedPane.addTab("Baccarat", createBaccaratPanel());
        tabbedPane.addTab("Dice", createDicePanel());

        add(tabbedPane, BorderLayout.CENTER);

        // --- Bottom Panel ---
        JButton leaveButton = createVegasButton("Cash Out & Leave");
        leaveButton.addActionListener(e -> dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        bottomPanel.add(leaveButton);
        add(bottomPanel, BorderLayout.SOUTH);

        updateBalanceLabel();
    }

    private JPanel createHousePanel() {
        // The main container for the left side
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        mainPanel.setPreferredSize(new Dimension(250, 0));
        // Padding around the whole left column to push it away from the window edges
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. The Image Frame (The "Monitor")
        // This panel gets the border to look like a screen
        JPanel imageFrame = new JPanel(new BorderLayout());
        imageFrame.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        imageFrame.setBorder(new LineBorder(GameWindow.FALLOUT_GREEN, 2));

        houseImageLabel = new JLabel("Loading House...", SwingConstants.CENTER);
        houseImageLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        houseImageLabel.setFont(GameWindow.FALLOUT_FONT);
        // Force size so the border is visible immediately
        houseImageLabel.setPreferredSize(new Dimension(230, 230));

        // Load image in background
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                URL url = new URL("https://images.steamusercontent.com/ugc/1678115504355894577/A84B108035C4DF028DA574DB9F3767ABAC226E43/?imw=512&&ima=fit&impolicy=Letterbox&imcolor=%23000000&letterbox=false");
                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage();
                // Scale to fit inside the 230x230 frame
                Image scaled = img.getScaledInstance(230, 230, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }

            @Override
            protected void done() {
                try {
                    houseImageLabel.setText("");
                    houseImageLabel.setIcon(get());
                } catch (Exception e) {
                    houseImageLabel.setText("[CONNECTION LOST]");
                }
            }
        };
        worker.execute();

        imageFrame.add(houseImageLabel, BorderLayout.CENTER);

        // 2. The Text Area (Dialogue)
        // This sits outside the border, looking like subtitles or a log
        JTextArea quoteArea = new JTextArea("\n\"The House always wins.\"\n\nMake your bets, " + player.getName() + ". My calculations predict a 98% probability of your caps becoming mine.");
        quoteArea.setWrapStyleWord(true);
        quoteArea.setLineWrap(true);
        quoteArea.setEditable(false);
        quoteArea.setFont(GameWindow.FALLOUT_FONT.deriveFont(Font.ITALIC, 12f));
        quoteArea.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        quoteArea.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        // Add top padding to separate the text from the monitor frame
        quoteArea.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Assemble the panel
        mainPanel.add(imageFrame, BorderLayout.NORTH);
        mainPanel.add(quoteArea, BorderLayout.CENTER);

        return mainPanel;
    }

    // ==============================================================================================
    // === COINFLIP =================================================================================
    // ==============================================================================================
    private JPanel createCoinflipPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Bet Input
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel betLabel = new JLabel("Bet (" + coinflipCurrency.name() + "):");
        betLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        betLabel.setFont(GameWindow.FALLOUT_FONT);
        controls.add(betLabel, gbc);

        gbc.gridx = 1;
        coinflipBetField = new JTextField("10", 5);
        styleTextField(coinflipBetField);
        controls.add(coinflipBetField, gbc);

        // Choice Selection
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel choiceLabel = new JLabel("Call it:");
        choiceLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        choiceLabel.setFont(GameWindow.FALLOUT_FONT);
        controls.add(choiceLabel, gbc);

        gbc.gridx = 1;
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        headsBtn = new JRadioButton("Heads");
        tailsBtn = new JRadioButton("Tails");
        styleRadioButton(headsBtn);
        styleRadioButton(tailsBtn);
        headsBtn.setSelected(true);
        ButtonGroup group = new ButtonGroup();
        group.add(headsBtn);
        group.add(tailsBtn);
        radioPanel.add(headsBtn);
        radioPanel.add(tailsBtn);
        controls.add(radioPanel, gbc);

        // Action Button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton flipButton = createVegasButton("Flip Coin");
        flipButton.addActionListener(e -> playCoinflip());
        controls.add(flipButton, gbc);

        coinflipOutput = new JTextArea("Welcome to Coinflip.\nCurrency: " + coinflipCurrency.name() + "\nSelect Heads or Tails.");
        styleTextArea(coinflipOutput);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(coinflipOutput), BorderLayout.CENTER);
        return panel;
    }

    private void playCoinflip() {
        int bet = validateBet(coinflipBetField, coinflipCurrency, coinflipOutput);
        if (bet == -1) return;

        boolean chosenHeads = headsBtn.isSelected();
        boolean isHeads = random.nextBoolean();
        result.incrementGamesPlayed();

        String outcomeStr = isHeads ? "HEADS" : "TAILS";
        coinflipOutput.append("\n\nCoin shows: " + outcomeStr);

        if (chosenHeads == isHeads) {
            result.addDelta(coinflipCurrency, bet);
            coinflipOutput.append("\nWINNER! You gained " + bet + " " + coinflipCurrency.name() + ".");
        } else {
            result.addDelta(coinflipCurrency, -bet);
            coinflipOutput.append("\nLOSER. You lost " + bet + " " + coinflipCurrency.name() + ".");
        }
        updateBalanceLabel();
    }

    // ==============================================================================================
    // === BLACKJACK ================================================================================
    // ==============================================================================================
    private JPanel createBlackjackPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);

        JLabel betLabel = new JLabel("Bet (" + blackjackCurrency.name() + "):");
        betLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        betLabel.setFont(GameWindow.FALLOUT_FONT);
        controls.add(betLabel);

        bjBetField = new JTextField("20", 5);
        styleTextField(bjBetField);
        controls.add(bjBetField);

        bjDealBtn = createVegasButton("Deal");
        bjHitBtn = createVegasButton("Hit");
        bjStandBtn = createVegasButton("Stand");
        bjHitBtn.setEnabled(false);
        bjStandBtn.setEnabled(false);

        controls.add(bjDealBtn);
        controls.add(bjHitBtn);
        controls.add(bjStandBtn);

        bjOutput = new JTextArea("Blackjack pays 1:1.\nCurrency: " + blackjackCurrency.name() + "\nDealer stands on 17.");
        styleTextArea(bjOutput);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(bjOutput), BorderLayout.CENTER);

        bjDealBtn.addActionListener(e -> startBlackjackRound());
        bjHitBtn.addActionListener(e -> blackjackHit());
        bjStandBtn.addActionListener(e -> blackjackStand());

        return panel;
    }

    private void startBlackjackRound() {
        if (bjInRound) return;
        bjCurrentBet = validateBet(bjBetField, blackjackCurrency, bjOutput);
        if (bjCurrentBet == -1) return;

        bjPlayerHand = new java.util.ArrayList<>();
        bjDealerHand = new java.util.ArrayList<>();

        bjPlayerHand.add(drawCard());
        bjDealerHand.add(drawCard());
        bjPlayerHand.add(drawCard());
        bjDealerHand.add(drawCard());

        result.incrementGamesPlayed();
        bjInRound = true;
        toggleBlackjackControls(true);

        updateBlackjackOutput(false);

        if (calcHand(bjPlayerHand) == 21) {
            bjOutput.append("\nBLACKJACK! Natural 21!");
            blackjackStand();
        }
    }

    private void blackjackHit() {
        if (!bjInRound) return;
        int card = drawCard();
        bjPlayerHand.add(card);
        bjOutput.append("\nYou drew a " + card);

        if (calcHand(bjPlayerHand) > 21) {
            bjOutput.append("\nBUST! You went over 21.");
            endBlackjackRound(false);
        } else {
            updateBlackjackOutput(false);
        }
    }

    private void blackjackStand() {
        if (!bjInRound) return;

        while (calcHand(bjDealerHand) < 17) {
            bjDealerHand.add(drawCard());
        }

        updateBlackjackOutput(true);

        int pScore = calcHand(bjPlayerHand);
        int dScore = calcHand(bjDealerHand);

        bjOutput.append("\n\nFinal: You (" + pScore + ") vs Dealer (" + dScore + ")");

        if (dScore > 21) {
            bjOutput.append("\nDealer BUSTS! You win!");
            endBlackjackRound(true);
        } else if (pScore > dScore) {
            bjOutput.append("\nYou WIN!");
            endBlackjackRound(true);
        } else if (pScore < dScore) {
            bjOutput.append("\nDealer wins.");
            endBlackjackRound(false);
        } else {
            bjOutput.append("\nPUSH. Bet returned.");
            bjInRound = false;
            toggleBlackjackControls(false);
        }
    }

    private void endBlackjackRound(boolean win) {
        if (win) {
            result.addDelta(blackjackCurrency, bjCurrentBet);
        } else {
            result.addDelta(blackjackCurrency, -bjCurrentBet);
        }
        updateBalanceLabel();
        bjInRound = false;
        toggleBlackjackControls(false);
    }

    private void toggleBlackjackControls(boolean inRound) {
        bjDealBtn.setEnabled(!inRound);
        bjBetField.setEnabled(!inRound);
        bjHitBtn.setEnabled(inRound);
        bjStandBtn.setEnabled(inRound);
    }

    private void updateBlackjackOutput(boolean showDealer) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dealer: ");
        if (showDealer) {
            sb.append(bjDealerHand.toString()).append(" (").append(calcHand(bjDealerHand)).append(")");
        } else {
            sb.append("[").append(bjDealerHand.get(0)).append(", ?]");
        }
        sb.append("\nYou:    ").append(bjPlayerHand.toString()).append(" (").append(calcHand(bjPlayerHand)).append(")");
        bjOutput.setText(sb.toString());
    }

    private int drawCard() {
        int[] possibleCards = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11};
        return possibleCards[random.nextInt(possibleCards.length)];
    }

    private int calcHand(java.util.List<Integer> hand) {
        int total = 0;
        int aces = 0;
        for (int c : hand) {
            total += c;
            if (c == 11) aces++;
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    // ==============================================================================================
    // === BACCARAT =================================================================================
    // ==============================================================================================
    private JPanel createBaccaratPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);

        JLabel betLabel = new JLabel("Bet (" + baccaratCurrency.name() + "):");
        betLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        betLabel.setFont(GameWindow.FALLOUT_FONT);
        controls.add(betLabel);

        bacBetField = new JTextField("20", 5);
        styleTextField(bacBetField);
        controls.add(bacBetField);

        JLabel typeLabel = new JLabel(" on ");
        typeLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        typeLabel.setFont(GameWindow.FALLOUT_FONT);
        controls.add(typeLabel);

        bacTypeBox = new JComboBox<>(new String[]{"Player (1:1)", "Banker (0.95:1)", "Tie (8:1)"});
        bacTypeBox.setFont(GameWindow.FALLOUT_FONT);
        bacTypeBox.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        bacTypeBox.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        controls.add(bacTypeBox);

        JButton playBtn = createVegasButton("Deal");
        controls.add(playBtn);

        bacOutput = new JTextArea("Baccarat.\nCurrency: " + baccaratCurrency.name() + "\nPredict winner: Player or Banker.");
        styleTextArea(bacOutput);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(bacOutput), BorderLayout.CENTER);

        playBtn.addActionListener(e -> playBaccarat());

        return panel;
    }

    private void playBaccarat() {
        int bet = validateBet(bacBetField, baccaratCurrency, bacOutput);
        if (bet == -1) return;

        String choice = (String) bacTypeBox.getSelectedItem();
        result.incrementGamesPlayed();

        java.util.List<Integer> pHand = new java.util.ArrayList<>();
        java.util.List<Integer> bHand = new java.util.ArrayList<>();
        pHand.add(drawBaccaratCard()); bHand.add(drawBaccaratCard());
        pHand.add(drawBaccaratCard()); bHand.add(drawBaccaratCard());

        StringBuilder log = new StringBuilder();
        log.append("Initial Deal:\n");
        log.append(" Player: ").append(formatBaccaratHand(pHand)).append("\n");
        log.append(" Banker: ").append(formatBaccaratHand(bHand)).append("\n");

        int pVal = calcBaccarat(pHand);
        int bVal = calcBaccarat(bHand);

        if (pVal >= 8 || bVal >= 8) {
            log.append("Natural!\n");
        } else {
            int pThird = -1;
            if (pVal <= 5) {
                pThird = drawBaccaratCard();
                pHand.add(pThird);
                log.append(" Player draws: ").append(cardName(pThird)).append("\n");
                pVal = calcBaccarat(pHand);
            }

            boolean bankerDraws = false;
            if (pThird == -1) {
                if (bVal <= 5) bankerDraws = true;
            } else {
                if (bVal <= 2) bankerDraws = true;
                else if (bVal == 3 && pThird != 8) bankerDraws = true;
                else if (bVal == 4 && pThird >= 2 && pThird <= 7) bankerDraws = true;
                else if (bVal == 5 && pThird >= 4 && pThird <= 7) bankerDraws = true;
                else if (bVal == 6 && pThird >= 6 && pThird <= 7) bankerDraws = true;
            }

            if (bankerDraws) {
                int bCard = drawBaccaratCard();
                bHand.add(bCard);
                log.append(" Banker draws: ").append(cardName(bCard)).append("\n");
                bVal = calcBaccarat(bHand);
            }
        }

        log.append("\nFinal: Player ").append(pVal).append(" - Banker ").append(bVal).append("\n");

        String winner;
        if (pVal > bVal) winner = "Player";
        else if (bVal > pVal) winner = "Banker";
        else winner = "Tie";

        double winnings = 0;
        boolean won = false;
        if (choice.startsWith("Player") && winner.equals("Player")) {
            winnings = bet;
            won = true;
        } else if (choice.startsWith("Banker") && winner.equals("Banker")) {
            winnings = bet * 0.95;
            won = true;
        } else if (choice.startsWith("Tie") && winner.equals("Tie")) {
            winnings = bet * 8;
            won = true;
        }

        if (won) {
            log.append("WIN! +").append((int)winnings).append(" ").append(baccaratCurrency.name());
            result.addDelta(baccaratCurrency, (int)winnings);
        } else {
            log.append("LOSS. -").append(bet).append(" ").append(baccaratCurrency.name());
            result.addDelta(baccaratCurrency, -bet);
        }

        bacOutput.setText(log.toString());
        updateBalanceLabel();
    }

    private int drawBaccaratCard() {
        return random.nextInt(13) + 1;
    }

    private int calcBaccarat(java.util.List<Integer> hand) {
        int sum = 0;
        for (int c : hand) {
            if (c >= 10) sum += 0;
            else sum += c;
        }
        return sum % 10;
    }

    private String formatBaccaratHand(java.util.List<Integer> hand) {
        StringBuilder sb = new StringBuilder("[");
        for (int c : hand) sb.append(cardName(c)).append(" ");
        sb.append("] (").append(calcBaccarat(hand)).append(")");
        return sb.toString();
    }

    private String cardName(int c) {
        if (c == 1) return "A";
        if (c == 11) return "J";
        if (c == 12) return "Q";
        if (c == 13) return "K";
        return String.valueOf(c);
    }

    // ==============================================================================================
    // === DICE =====================================================================================
    // ==============================================================================================
    private JPanel createDicePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputs.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);

        JLabel betLabel = new JLabel("Bet (" + diceCurrency.name() + "):");
        betLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        betLabel.setFont(GameWindow.FALLOUT_FONT);
        inputs.add(betLabel);

        diceBetField = new JTextField("1", 5);
        styleTextField(diceBetField);
        inputs.add(diceBetField);

        JLabel guessLabel = new JLabel("Guess:");
        guessLabel.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        guessLabel.setFont(GameWindow.FALLOUT_FONT);
        inputs.add(guessLabel);

        JComboBox<Integer> guessBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        guessBox.setFont(GameWindow.FALLOUT_FONT);
        guessBox.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        guessBox.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        inputs.add(guessBox);

        JButton rollButton = createVegasButton("Roll Die");
        inputs.add(rollButton);

        diceOutput = new JTextArea("Dice Game.\nCurrency: " + diceCurrency.name() + "\nGuess the number (1-6). Pays 4x.");
        styleTextArea(diceOutput);

        panel.add(inputs, BorderLayout.NORTH);
        panel.add(new JScrollPane(diceOutput), BorderLayout.CENTER);

        rollButton.addActionListener(e -> playDice(guessBox));
        return panel;
    }

    private void playDice(JComboBox<Integer> guessBox) {
        int bet = validateBet(diceBetField, diceCurrency, diceOutput);
        if (bet == -1) return;

        int guess = (Integer) guessBox.getSelectedItem();
        int roll = random.nextInt(6) + 1;
        result.incrementGamesPlayed();

        if (guess == roll) {
            int profit = bet * 4;
            result.addDelta(diceCurrency, profit);
            diceOutput.setText("Roll: " + roll + "\nWINNER! + " + profit + " " + diceCurrency.name());
        } else {
            result.addDelta(diceCurrency, -bet);
            diceOutput.setText("Roll: " + roll + "\nLoser. - " + bet + " " + diceCurrency.name());
        }
        updateBalanceLabel();
    }

    // ==============================================================================================
    // === HELPERS ==================================================================================
    // ==============================================================================================
    private int validateBet(JTextField field, ResourceType type, JTextArea output) {
        int bet;
        try {
            bet = Integer.parseInt(field.getText());
        } catch (NumberFormatException e) {
            output.setText("Invalid bet amount."); return -1;
        }

        if (bet <= 0) {
            output.setText("Bet must be positive."); return -1;
        }

        int currentRes = player.getResources().getOrDefault(type, 0)
                + result.getDeltas().getOrDefault(type, 0);

        if (type == ResourceType.BOTTLECAPS) {
            currentRes = player.getBottleCaps() + result.getDeltas().getOrDefault(ResourceType.BOTTLECAPS, 0);
        }

        if (bet > currentRes) {
            output.setText("Insufficient funds. You have " + currentRes + " " + type.name()); return -1;
        }
        return bet;
    }

    private void updateBalanceLabel() {
        StringBuilder sb = new StringBuilder("Balance: ");

        int caps = player.getBottleCaps() + result.getDeltas().getOrDefault(ResourceType.BOTTLECAPS, 0);
        sb.append(caps).append(" Caps | ");

        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.BOTTLECAPS) continue;
            int amount = player.getResources().getOrDefault(type, 0) + result.getDeltas().getOrDefault(type, 0);
            if (amount > 0) {
                sb.append(amount).append(" ").append(type.name()).append(" | ");
            }
        }

        String txt = sb.toString();
        if (txt.endsWith(" | ")) {
            txt = txt.substring(0, txt.length() - 3);
        }
        balanceLabel.setText(txt);
    }

    public CasinoResult getResult() {
        return this.result;
    }

    // Custom Orange Button for Vegas Theme
    private JButton createVegasButton(String text) {
        JButton button = new JButton(text);
        button.setFont(GameWindow.FALLOUT_FONT);
        button.setBackground(new Color(80, 40, 10)); // Dark Orange/Brown BG
        button.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(GameWindow.NEW_VEGAS_ORANGE, 1));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled())
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/hover.wav").toString());
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    // Play cash sound for "Cash Out" or "Deal"?
                    // Let's stick to standard click for interaction, results play cash.
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/click.wav").toString());
                }
            }
        });
        return button;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(GameWindow.FALLOUT_FONT);
        tf.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        tf.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        tf.setCaretColor(GameWindow.NEW_VEGAS_ORANGE);
        tf.setBorder(BorderFactory.createLineBorder(GameWindow.NEW_VEGAS_ORANGE));
    }

    private void styleTextArea(JTextArea ta) {
        ta.setFont(GameWindow.FALLOUT_FONT);
        ta.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        ta.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        ta.setEditable(false);
    }

    private void styleRadioButton(JRadioButton rb) {
        rb.setFont(GameWindow.FALLOUT_FONT);
        rb.setBackground(GameWindow.NEW_VEGAS_BACKGROUND);
        rb.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        rb.setFocusPainted(false);
    }
}