package agolf.game;

import agolf.GameApplet;
import agolf.GameContainer;
import agolf.SynchronizedInteger;
import com.aapeli.colorgui.Choicer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

class PlayerInfoPanel extends Panel implements ItemListener, MouseListener {

    private static final Font fontDialog12 = new Font("Dialog", Font.PLAIN, 12);
    private static final Font fontDialog12b = new Font("Dialog", Font.BOLD, 12);
    private static final Font fontDialog10 = new Font("Dialog", Font.PLAIN, 10);
    private static final Color currentTrackScoreHighlightColor = new Color(224, 255, 224);
    private static final Color aColor372 = new Color(128, 208, 128);
    private static final Color losingComparisonColor = new Color(224, 0, 0);
    private static final Color leadingComparisonColor = new Color(0, 128, 0);
    private static final Color[][] playerColors =
            new Color[][] { // first color == player is in game, second == player has left the game
                {new Color(0, 0, 255), new Color(0, 0, 0)},
                {new Color(255, 0, 0), new Color(0, 0, 0)},
                {new Color(128, 128, 0), new Color(0, 0, 0)},
                {new Color(0, 160, 0), new Color(0, 0, 0)},
                {new Color(102, 0, 204), new Color(0, 0, 0)},
                {new Color(0, 153, 153), new Color(0, 0, 0)},
                {new Color(255, 153, 255), new Color(0, 0, 0)},
                {new Color(255, 153, 51), new Color(0, 0, 0)}
            };

    private static int scoreComparisonMode;
    private GameContainer gameContainer;
    private final int width;
    private final int height;
    private boolean initialized;
    private int playerCount;
    private int trackCount;
    private int maxStrokes;
    private int strokeTimeout;
    private int trackScoring; // either stroke scoring (0) or track scoring (1)
    private int currentTrackIndex;
    private int activePlayerId; // id of whoever's turn it is
    private int[] gameOutcome;
    protected int playerId; // player id of this client
    protected String[] playerNames;
    protected String[] playerClans;
    private SynchronizedInteger[][] trackStrokes;
    private SynchronizedInteger[] totalStrokes;
    private int[] playerLeaveReasons; // see PART_ enums in Lobby class
    private boolean[] playerVotedToSkip;
    private boolean[] playerReadyForNewGame;
    private int[] trackScoresMultipliers;
    private int[][] resultsToCompareScoreAgainst;
    private Choicer compareResultsChoicer;
    private Image image;
    private Graphics graphics;
    private TimerThread timerThread;
    private int timeRemainingForShot; // seconds

    protected PlayerInfoPanel(GameContainer gameContainer, int width, int height) {
        this.gameContainer = gameContainer;
        this.width = width;
        this.height = height;
        this.setSize(width, height);
        this.setLayout(null);
        this.timeRemainingForShot = -1;
        this.initialized = false;
        this.trackScoresMultipliers = null;
        this.resultsToCompareScoreAgainst = null;
    }

    public void addNotify() {
        super.addNotify();
        this.repaint();
    }

    @Override
    public void paint(Graphics g) {
        this.update(g);
    }

    @Override
    public void update(Graphics g) {
        if (this.image == null) {
            this.image = this.createImage(this.width, this.height);
            this.graphics = this.image.getGraphics();
        }

        this.graphics.setColor(GameApplet.colourGameBackground);
        this.graphics.fillRect(0, 0, this.width, this.height);
        if (this.initialized) {
            // draw score comparison row
            int[] comparedResults = null;
            if (this.resultsToCompareScoreAgainst != null && scoreComparisonMode > 0) {
                comparedResults = this.resultsToCompareScoreAgainst[scoreComparisonMode - 1];
                this.graphics.setFont(fontDialog12);
                this.graphics.setColor(aColor372);
                this.graphics.drawString(
                        this.gameContainer.textManager.getGame("GamePlayerInfo_CompareResultNick"), 20, 20);
                int totalComparisonStrokes = 0;

                for (int comparedTrackIndex = 0;
                        comparedTrackIndex <= this.currentTrackIndex && comparedTrackIndex < this.trackCount;
                        ++comparedTrackIndex) {
                    this.graphics.drawString(
                            comparedResults[comparedTrackIndex] > 0
                                    ? String.valueOf(comparedResults[comparedTrackIndex])
                                    : "?",
                            130 + comparedTrackIndex * 20,
                            20);
                    totalComparisonStrokes += comparedResults[comparedTrackIndex];
                }

                if (totalComparisonStrokes > 0) {
                    this.graphics.drawString("= " + totalComparisonStrokes, 130 + this.trackCount * 20 + 15, 20);
                }
            }

            int yBaseline = (9 - this.playerCount) * 13;
            // draw highlight around scores for current track
            if (this.currentTrackIndex >= 0 && this.currentTrackIndex < this.trackCount) {
                this.graphics.setColor(currentTrackScoreHighlightColor);
                this.graphics.fillRect(
                        130 + this.currentTrackIndex * 20 - 5 + 1, yBaseline - 13, 19, this.playerCount * 15 + 2);
                this.graphics.fillRect(
                        130 + this.currentTrackIndex * 20 - 5, yBaseline - 13 + 1, 21, this.playerCount * 15 + 2 - 2);
            }

            // draw player status text
            for (int player = 0; player < this.playerCount; ++player) {
                Font font = this.playerId == player ? fontDialog12b : fontDialog12;
                int playerLeft = this.playerLeaveReasons[player] == 0 ? 0 : 1;
                Color color = playerColors[player][playerLeft];
                this.graphics.setFont(font);
                this.graphics.setColor(color);

                int yOffset = yBaseline + 15 * player;
                // draw 1., 2., etc in front of player name
                if (this.playerCount > 1) {
                    this.graphics.drawString(player + 1 + ".", 2, yOffset);
                }

                // draw player name
                if (this.playerNames[player] != null) {
                    this.graphics.drawString(this.playerNames[player], 20, yOffset);
                }

                // draw track scores
                for (int track = 0; track < this.trackCount; ++track) {
                    if (track <= this.currentTrackIndex) {
                        int strokes = this.trackStrokes[player][track].get();
                        if (comparedResults != null) {
                            if (track < this.currentTrackIndex && strokes < comparedResults[track]) {
                                this.graphics.setColor(leadingComparisonColor);
                            }

                            if (strokes > comparedResults[track] && comparedResults[track] > 0) {
                                this.graphics.setColor(losingComparisonColor);
                            }
                        }

                        this.graphics.drawString(
                                strokes >= 0
                                        ? String.valueOf(strokes)
                                        : this.gameContainer.textManager.getGame("GamePlayerInfo_Skipped"),
                                130 + track * 20,
                                yOffset);
                        this.graphics.setColor(color);
                    } else if (this.trackScoresMultipliers[track] == 1) {
                        this.graphics.drawString("-", 130 + track * 20 + 5, yOffset);
                    } else {
                        this.graphics.setFont(fontDialog10);
                        this.graphics.setColor(playerColors[player][1]);
                        this.graphics.drawString(
                                "(*" + this.trackScoresMultipliers[track] + ")", 130 + track * 20, yOffset - 1);
                        this.graphics.setFont(font);
                        this.graphics.setColor(color);
                    }
                }

                // draw sum of player strokes
                this.graphics.drawString(
                        "= " + this.totalStrokes[player].get(), 130 + this.trackCount * 20 + 15, yOffset);

                String scoreDifference = null;
                int[] scoreDifferences = this.getScoreDifferences();
                // draw difference with leader's score
                if (scoreDifferences != null && this.playerLeaveReasons[player] == 0) {
                    if (scoreDifferences[player] == 0) {
                        if (this.gameOutcome == null) {
                            scoreDifference = this.gameContainer.textManager.getGame("GamePlayerInfo_Leader");
                        }
                    } else {
                        scoreDifference = this.gameContainer.textManager.getGame(
                                "GamePlayerInfo_AfterLeader",
                                (scoreDifferences[player] > 0 ? "+" : "") + scoreDifferences[player]);
                    }

                    if (scoreDifference != null) {
                        this.graphics.drawString(scoreDifference, 130 + this.trackCount * 20 + 15 + 40, yOffset);
                    }
                }

                String statusMessage = null;
                // draw "waiting for player" text if lobby has empty slots
                if (this.playerNames[player] == null) {
                    statusMessage = "GamePlayerInfo_WaitingPlayer";
                }

                // draw "Your turn" or "Currently playing" next to player whose turn it is
                String timeRemaining = "";
                if (this.playerCount > 1 && this.activePlayerId == player) {
                    if (this.activePlayerId == this.playerId) {
                        statusMessage = "GamePlayerInfo_OwnTurn";
                    } else {
                        statusMessage = "GamePlayerInfo_PlayerTurn";
                    }
                    if (this.timerThread != null && this.timeRemainingForShot > 0 && this.strokeTimeout > 0) {
                        timeRemaining = " (" + this.gameContainer.textManager.getTime(this.timeRemainingForShot) + ")";
                    }
                }

                if (this.gameOutcome != null) {
                    if (this.gameOutcome[player] == 1) {
                        statusMessage = "GamePlayerInfo_Winner";
                    } else if (this.gameOutcome[player] == 0) {
                        statusMessage = "GamePlayerInfo_Draw";
                    }
                }

                if (statusMessage != null) {
                    this.graphics.drawString(
                            this.gameContainer.textManager.getGame(statusMessage) + timeRemaining,
                            130 + this.trackCount * 20 + 15 + 40 + 40,
                            yOffset);
                }

                String extraMessage = null;
                if (this.playerVotedToSkip[player]) {
                    extraMessage = "GamePlayerInfo_VoteSkipTrack";
                }

                if (this.playerReadyForNewGame[player]) {
                    extraMessage = "GamePlayerInfo_ReadyForNewGame";
                }

                if (this.playerLeaveReasons[player] == 5) {
                    extraMessage = "GamePlayerInfo_Quit_ConnectionProblem";
                }

                if (this.playerLeaveReasons[player] == 4) {
                    extraMessage = "GamePlayerInfo_Quit_Part";
                }

                if (extraMessage != null) {
                    this.graphics.drawString(
                            this.gameContainer.textManager.getGame(extraMessage),
                            130 + this.trackCount * 20 + 15 + 40 + 40 + 100,
                            yOffset);
                }
            }
        }

        g.drawImage(this.image, 0, 0, this);
    }

    public void itemStateChanged(ItemEvent var1) {
        scoreComparisonMode = this.compareResultsChoicer.getSelectedIndex();
        this.repaint();
    }

    public void mouseEntered(MouseEvent var1) {}

    public void mouseExited(MouseEvent var1) {}

    public void mousePressed(MouseEvent var1) {
        if (var1.getClickCount() == 2) {
            int var2 = var1.getY();
            int var3 = (5 - this.playerCount) * 13;

            for (int var4 = 0; var4 < this.playerCount; ++var4) {
                if (this.playerNames[var4] != null && var2 >= var3 - 12 && var2 < var3 + 3) {
                    this.gameContainer.gameApplet.showPlayerCard(this.playerNames[var4]);
                }

                var3 += 15;
            }
        }
    }

    public void mouseReleased(MouseEvent var1) {}

    public void mouseClicked(MouseEvent var1) {}

    protected void init(int playerCount, int trackCount, int maxStrokes, int strokeTimeout, int trackScoring) {
        this.playerCount = playerCount;
        this.trackCount = trackCount;
        this.maxStrokes = maxStrokes;
        this.strokeTimeout = strokeTimeout;
        this.trackScoring = trackScoring;
        this.playerNames = new String[playerCount];
        this.playerClans = new String[playerCount];
        this.trackStrokes = new SynchronizedInteger[playerCount][trackCount];
        this.totalStrokes = new SynchronizedInteger[playerCount];

        for (int player = 0; player < playerCount; ++player) {
            for (int track = 0; track < trackCount; ++track) {
                this.trackStrokes[player][track] = new SynchronizedInteger();
            }

            this.totalStrokes[player] = new SynchronizedInteger();
        }

        this.playerLeaveReasons = new int[playerCount];
        this.playerVotedToSkip = new boolean[playerCount];
        this.playerReadyForNewGame = new boolean[playerCount];

        for (int player = 0; player < playerCount; ++player) {
            this.playerNames[player] = null;
            this.playerClans[player] = null;
            this.playerLeaveReasons[player] = 0;
        }

        this.trackScoresMultipliers = new int[trackCount];

        for (int track = 0; track < trackCount; ++track) {
            this.trackScoresMultipliers[track] = 1;
        }

        this.playerId = -1;
        this.reset();
        this.initialized = true;
        this.repaint();
    }

    protected void setTrackScoresMultipliers(int[] trackScoresMultipliers) {
        this.trackScoresMultipliers = trackScoresMultipliers;
        this.repaint();
    }

    protected void addPlayer(int playerID, String name, String clan, boolean isLocalPlayer) {
        this.playerNames[playerID] = name;
        this.playerClans[playerID] = clan;
        if (isLocalPlayer) {
            this.playerId = playerID;
        }

        this.repaint();
        this.removeMouseListener(this);
        this.addMouseListener(this);
    }

    protected boolean setPlayerPartStatus(int playerId, int status) {
        if (status == 6) {
            this.playerNames[playerId] = null;
        } else {
            this.playerLeaveReasons[playerId] = status;
            if (this.gameContainer.gamePanel.state == 2) {
                this.gameContainer.gamePanel.state = 3;
                this.repaint();
                return true;
            }
        }

        this.repaint();
        return false;
    }

    protected void reset() {
        for (int player = 0; player < this.playerCount; ++player) {
            for (int track = 0; track < this.trackCount; ++track) {
                this.trackStrokes[player][track].set(0);
            }

            this.totalStrokes[player].set(0);
            this.playerVotedToSkip[player] = this.playerReadyForNewGame[player] = false;
        }

        this.currentTrackIndex = -1;
        this.activePlayerId = -1;
        this.gameOutcome = null;
        this.repaint();
    }

    protected int startNextTrack() {
        for (int player = 0; player < this.playerCount; ++player) {
            this.playerVotedToSkip[player] = false;
        }

        ++this.currentTrackIndex;
        this.repaint();
        return this.trackScoresMultipliers[this.currentTrackIndex];
    }

    protected boolean isOverStrokeLimit(int player) {
        if (this.maxStrokes == 0) {
            return false;
        } else {
            int strokes = this.trackStrokes[player][this.currentTrackIndex].get();
            if (this.trackScoring == 0) {
                strokes /= this.trackScoresMultipliers[this.currentTrackIndex];
            }

            return strokes >= this.maxStrokes;
        }
    }

    protected boolean startTurn(int playerId) {
        this.activePlayerId = playerId;

        if (this.playerCount > 1) {
            this.stopTimer();
            this.timeRemainingForShot = this.strokeTimeout > 0 ? this.strokeTimeout : 180;
            this.timerThread = new TimerThread(this);
        }

        this.repaint();
        return playerId == this.playerId;
    }

    protected void strokeStartedOrEnded(int playerId, boolean isStrokeEnd) {
        if (this.trackScoring == 0) {
            int strokeCount = !isStrokeEnd ? this.trackScoresMultipliers[this.currentTrackIndex] : 1;
            this.trackStrokes[playerId][this.currentTrackIndex].add(strokeCount);
            this.totalStrokes[playerId].add(strokeCount);
        } else {
            this.trackStrokes[playerId][this.currentTrackIndex].add(1);
        }

        this.repaint();
    }

    protected void setScores(int trackId, int[] scores) {
        for (int player = 0; player < this.playerCount; ++player) {
            this.trackStrokes[player][trackId].set(scores[player]);
            this.totalStrokes[player].set(0);

            for (int track = 0; track <= trackId; ++track) {
                int strokes = this.trackStrokes[player][track].get();
                if (strokes >= 0) {
                    this.totalStrokes[player].add(strokes);
                }
            }
        }

        this.repaint();
    }

    protected void setGameOutcome(int[] outcome) {
        this.gameOutcome = outcome;
        this.currentTrackIndex = this.trackCount;
        if (outcome != null) {
            if (outcome[this.playerId] == 1) {
                this.gameContainer.soundManager.playGameWinner();
            } else if (outcome[this.playerId] == 0) {
                this.gameContainer.soundManager.playGameDraw();
            } else {
                this.gameContainer.soundManager.playGameLoser();
            }
        }
        this.activePlayerId = -1;
        this.repaint();
    }

    protected void voteSkip() {
        this.voteSkip(this.playerId);
    }

    protected void voteSkip(int playerId) {
        this.playerVotedToSkip[playerId] = true;
        this.repaint();
    }

    protected void voteSkipReset() {
        for (int var1 = 0; var1 < this.playerCount; ++var1) {
            this.playerVotedToSkip[var1] = false;
        }

        this.repaint();
    }

    protected void readyForNewGameLocal() {
        this.readyForNewGame(this.playerId);
    }

    protected void readyForNewGame(int player) {
        this.playerReadyForNewGame[player] = true;
        this.repaint();
    }

    protected void setState(int state) {
        if (state == 2) {
            for (int player = 0; player < this.playerCount && state == 2; ++player) {
                if (this.playerLeaveReasons[player] != 0) {
                    state = 3;
                    this.gameContainer.gamePanel.state = 3;
                }
            }
        }
    }

    protected void method372() {
        if (this.trackScoring == 0) {
            this.totalStrokes[this.activePlayerId].add(
                    -this.trackStrokes[this.activePlayerId][this.currentTrackIndex].get());
        }

        this.trackStrokes[this.activePlayerId][this.currentTrackIndex].set(-1);
        this.repaint();
    }

    protected String[] getPlayerName(int playerId) {
        return new String[] {this.playerNames[playerId], this.playerClans[playerId]};
    }

    protected String[] getPlayerNames() {
        return this.playerNames;
    }

    protected void initResultsComparison(int[][] results) {
        this.resultsToCompareScoreAgainst = results;
        this.compareResultsChoicer = new Choicer();
        this.compareResultsChoicer.addItem(this.gameContainer.textManager.getGame("GamePlayerInfo_CompareResultNone"));
        this.compareResultsChoicer.addItem(
                this.gameContainer.textManager.getGame("GamePlayerInfo_CompareResultOwn", this.playerNames[0]));
        this.compareResultsChoicer.addItem(
                this.gameContainer.textManager.getGame("GamePlayerInfo_CompareResultTop100Average"));
        this.compareResultsChoicer.addItem(
                this.gameContainer.textManager.getGame("GamePlayerInfo_CompareResultBestOfDay"));
        this.compareResultsChoicer.addItem(
                this.gameContainer.textManager.getGame("GamePlayerInfo_CompareResultBestOfAllTime"));
        this.compareResultsChoicer.addItem(this.gameContainer.textManager.getGame("GamePlayerInfo_CompareResultIdeal"));
        this.compareResultsChoicer.select(scoreComparisonMode);
        this.compareResultsChoicer.setBounds(555, 5, 170, 20);
        this.compareResultsChoicer.addItemListener(this);
        this.setVisible(false);
        this.add(this.compareResultsChoicer);
        this.setVisible(true);
        this.repaint();
    }

    protected boolean shouldSkipTrack() {
        for (int player = 0; player < this.playerCount; ++player) {
            if (this.playerLeaveReasons[player] == 0 && !this.playerVotedToSkip[player]) {
                return false;
            }
        }

        return true;
    }

    protected int method377() {
        return this.playerCount > 1 ? -1 : this.trackStrokes[0][this.currentTrackIndex].get();
    }

    protected void stopTimer() {
        if (this.timerThread != null) {
            this.timerThread.stopRunning();
            this.timerThread = null;
        }
    }

    private int[] getScoreDifferences() {
        int bestScore = this.trackScoring == 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;

        int score;
        for (int i = 0; i < this.playerCount; ++i) {
            if (this.playerLeaveReasons[i] == 0) {
                score = this.totalStrokes[i].get();
                if (this.trackScoring == 0 && score < bestScore) {
                    bestScore = score;
                }

                if (this.trackScoring == 1 && score > bestScore) {
                    bestScore = score;
                }
            }
        }

        int[] scoreDifferences = new int[this.playerCount];
        int winnerCount = 0;

        for (int i = 0; i < this.playerCount; ++i) {
            score = this.totalStrokes[i].get();
            if (score == bestScore) {
                scoreDifferences[i] = 0;
                ++winnerCount;
            } else {
                scoreDifferences[i] = score - bestScore;
            }
        }

        if (winnerCount == this.playerCount) {
            return null;
        } else {
            return scoreDifferences;
        }
    }

    protected boolean run() {
        --this.timeRemainingForShot;
        this.repaint();
        if (this.timeRemainingForShot <= 0) {
            if (this.activePlayerId == this.playerId) {
                this.gameContainer.gamePanel.tryStroke(true);
            }
            this.stopTimer();
            return false;
        } else {
            return true;
        }
    }
}
