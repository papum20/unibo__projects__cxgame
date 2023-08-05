
package LXCONO;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.TreeSet;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import connectx.CXCell;

public class L2mo implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;
    private Map<String, Integer> transpositionTable;
    private int[][] killerMoves = new int[10000][2];

    /* Default empty constructor */
    public L2mo() {

        transpositionTable = new HashMap<String, Integer>();
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        transpositionTable.clear();
    }

    public int evaluation(CXBoard B, int x, int y, CXCellState player) {
        CXCellState oppositePlayer; // The player that is not the current player
        if (player == CXCellState.P1) {
            oppositePlayer = CXCellState.P2;
        } else {
            oppositePlayer = CXCellState.P1;
        }
        int height = B.M;
        int width = B.N;
        int numtoConnect = B.X;
        int score = 0;
        int numToPrevent = numtoConnect - 1;
        int oppScore = 0; // Score for the opponent
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (B.cellState(i, j) == oppositePlayer) {
                    int oppConsecutive = getConsecutiveMarks(B, i, j, numToPrevent);
                    oppScore += (oppConsecutive > 0) ? (1 << oppConsecutive) : 0;
                }
            }
        }
        score += oppScore;

        int mobility = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (B.cellState(i, j) == CXCellState.FREE) {
                    int count = 1;
                    while (j + count < width && B.cellState(i, j + count) == CXCellState.FREE) {
                        count++;
                    }
                    mobility += count;
                }
            }
        }
        score += mobility;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int distanceFromCenter = Math.abs(j - width / 2);
                if (B.cellState(i, j) == player) {
                    score += getConsecutiveMarks(B, i, j, numtoConnect) * 10
                            + (height - i) * (101 - distanceFromCenter);
                } else {
                    score -= getConsecutiveMarks(B, i, j, numtoConnect) * 10
                            + (height - i) * (101 - distanceFromCenter);
                }
            }
        }
        return score;
    }

    public static int getConsecutiveMarks(CXBoard B, int x, int y, int numToConnect) {
        CXCellState player = B.cellState(x, y);
        int max = 0;

        int count = 0;
        // controllo orizzontale
        for (int i = Math.max(0, y - numToConnect + 1); i <= Math.min(B.M - 1, y + numToConnect - 1); i++) {
            if (B.cellState(x, i) == player) {
                count++;
                if (count >= max) {
                    max = count;
                }
                if (count >= numToConnect) {
                    return count;
                }
            } else {
                count = 0;
            }
        }

        count = 0;

        // Controllo verticale
        for (int i = Math.max(0, x - numToConnect + 1); i <= Math.min(B.N - 1, x + numToConnect - 1); i++) {
            if (B.cellState(i, y) == player) {
                count++;
                if (count >= max) {
                    max = count;
                }
                if (count >= numToConnect) {
                    return count;
                }
            } else {
                count = 0;
            }
        }

        count = 0;

        // Controllo diagonale verso destra
        for (int i = -numToConnect + 1; i <= numToConnect - 1; i++) {
            int r = x + i;
            int c = y + i;
            if (r >= 0 && r < B.N && c >= 0 && c < B.M) {
                if (B.cellState(r, c) == player) {
                    count++;
                    if (count >= max) {
                        max = count;
                    }
                    if (count >= numToConnect) {
                        return count;
                    }
                } else {
                    count = 0;
                }
            }
        }

        count = 0;

        // Controllo diagonale verso sinistra
        for (int i = -numToConnect + 1; i <= numToConnect - 1; i++) {
            int r = x + i;
            int c = y - i;
            if (r >= 0 && r < B.N && c >= 0 && c < B.M) {
                if (B.cellState(r, c) == player) {
                    count++;
                    if (count >= max) {
                        max = count;
                    }
                    if (count >= numToConnect) {
                        return count;
                    }
                } else {
                    count = 0;
                }
            }
        }
        return max;
    }

    public List<Integer> getOrderedColumns(CXBoard board) {
        List<Integer> orderedColumns = new ArrayList<>(); // creiamo una lista ordinata di colonne
        int[][] killerMoves = new int[100000][2]; // ci salviamo la colonna in cui è stata effettuata la killer move e
                                                  // la profondità
        for (int i = 0; i < 2 && i < killerMoves.length; i++) {
            killerMoves[i][0] = -1;
            killerMoves[i][1] = -1; // initialize killer moves
        }

        // check if there are any killer moves in transposition table
        String boardKey = board.toString(); // get board key
        if (transpositionTable.containsKey(boardKey)) { // check if board key is in transposition table
            Integer storedKillerMove = transpositionTable.get(boardKey); // get killer move from transposition table
            if (storedKillerMove != null) {
                // add stored killer move to beginning of ordered columns
                if (Arrays.stream(board.getAvailableColumns()).anyMatch(i -> i == storedKillerMove)) { // check if
                                                                                                       // killer move is
                    // valid
                    orderedColumns.remove(Integer.valueOf(storedKillerMove)); // remove killer move from ordered columns
                    orderedColumns.add(0, storedKillerMove); // add killer move to beginning of ordered columns
                }
            }
        }
        // add remaining columns to ordered list
        for (int move : board.getAvailableColumns()) { // add remaining columns to ordered list
            if (!orderedColumns.contains(move)) { // check if column is already in ordered list
                orderedColumns.add(move); // add column to ordered list
            }
        }
        // add killer moves to beginning of ordered list
        for (int[] killerMove : killerMoves) {
            int move = killerMove[0]; // prende la colonna in cui è stata effettuata la killermove
            if (move != -1 && Arrays.stream(board.getAvailableColumns()).anyMatch(i -> i == move)
                    && !orderedColumns.contains(move)) { // check if killer move is valid
                orderedColumns.add(0, move); // add killer move to beginning of ordered columns
            }
        }

        return orderedColumns; // return ordered columns
    }

    public int alphabeta(CXBoard B, int depth, boolean maximizingPlayer, int alpha, int beta) throws TimeoutException {
        String boardKey = B.toString(); // generate a key for the board state
        if (transpositionTable.containsKey(boardKey)) {
            // return stored result if available
            return transpositionTable.get(boardKey);
        }
        if (depth == 0 || B.gameState() != CXGameState.OPEN) {
            if (B.gameState() != CXGameState.OPEN && B.gameState() == myWin) {
                return Integer.MAX_VALUE; // win
            } else if (B.gameState() != CXGameState.OPEN && B.gameState() == yourWin) {
                return Integer.MIN_VALUE; // loss
            } else {
                int result = evaluation(B, B.M, B.N, CXCellState.P1);
                transpositionTable.put(boardKey, result); // store result in table
                return result;
            }
        } else if (maximizingPlayer) {
            int bestValue = Integer.MIN_VALUE;
            int killerMove = -1;
            for (int i : getOrderedColumns(B)) {
                checktime();
                B.markColumn(i);
                int value;
                value = alphabeta(B, depth - 1, false, alpha, beta);
                B.unmarkColumn();
                if (value > bestValue) {
                    bestValue = value;
                    killerMove = i;
                    alpha = Math.max(alpha, value);
                }
                if (beta <= alpha) {
                    break;
                }
            }
            if (killerMove != -1) {
                // Store the killer move
                killerMoves[depth][1] = killerMoves[depth][0];
                killerMoves[depth][0] = killerMove;
            }
            transpositionTable.put(boardKey, bestValue);
            return bestValue;
        } else {
            int bestValue = Integer.MAX_VALUE;
            int killerMove = -1;
            for (int i : getOrderedColumns(B)) {
                checktime();
                B.markColumn(i);
                int value;
                value = alphabeta(B, depth - 1, true, alpha, beta);
                B.unmarkColumn();
                if (value < bestValue) {
                    bestValue = value;
                    killerMove = i;
                    beta = Math.min(beta, value);
                }
                if (beta <= alpha) {
                    break;
                }
            }
            if (killerMove != -1) {
                // Store the killer move
                killerMoves[depth][1] = killerMoves[depth][0];
                killerMoves[depth][0] = killerMove;
            }
            transpositionTable.put(boardKey, bestValue);
            return bestValue;
        }
    }

    public int iterativeDeepening(CXBoard B) throws TimeoutException {
        Integer[] L = B.getAvailableColumns();
        int MAX_DEPTH = B.numOfFreeCells();
        int bestMove = L[rand.nextInt(L.length)];
        for (int i = 0; i < MAX_DEPTH; i++) {
            bestMove = alphabeta(B, i, true, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return bestMove;
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();
        Integer[] L = B.getAvailableColumns();
        try {
            int win = singleMoveWin(B, L);
            if (win != -1)
                return win;
            int block = singleMoveBlock(B, L);
            if (block != -1)
                return block;
            int bestValue = Integer.MIN_VALUE;
            int bestMove = -1;
            for (int i : L) {
                checktime();
                B.markColumn(i);
                int value = iterativeDeepening(B);
                B.unmarkColumn();
                if (value > bestValue) {
                    bestValue = value;
                    bestMove = i;
                }
            }
            return bestMove;
        } catch (TimeoutException e) {
            // Timeout: return a random column
            return L[rand.nextInt(L.length)];
        }
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    /**
     * Check if we can win in a single move
     *
     * Returns the winning column if there is one, otherwise -1
     */
    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            if (state == myWin)
                return i; // Winning column found: return immediately
            B.unmarkColumn();
        }
        return -1;
    }

    /**
     * Check if we can block adversary's victory
     *
     * Returns a blocking column if there is one, otherwise a random one
     */
    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
        TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

        for (int i : L) {
            checktime();
            T.add(i); // We consider column i as a possible move
            B.markColumn(i);

            int j;
            boolean stop;

            for (j = 0, stop = false; j < L.length && !stop; j++) {
                // try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} //
                // Uncomment to test timeout
                checktime();
                if (!B.fullColumn(L[j])) {
                    CXGameState state = B.markColumn(L[j]);
                    if (state == yourWin) {
                        T.remove(i); // We ignore the i-th column as a possible move
                        stop = true; // We don't need to check more
                    }
                    B.unmarkColumn(); //
                }
            }
            B.unmarkColumn();
        }

        if (T.size() > 0) {
            Integer[] X = T.toArray(new Integer[T.size()]);
            return X[rand.nextInt(X.length)];
        } else {
            return L[rand.nextInt(L.length)];
        }
    }

    public String playerName() {
        return "Alphabeta2";
    }
}