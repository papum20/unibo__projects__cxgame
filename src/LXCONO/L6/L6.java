package LXCONO.L6;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.TreeSet;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import connectx.CXCell;

public class L6 implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private CXBoard B;
    private int TIMEOUT;
    private long START;
    private TranspositionTable transpositionTable;
    private int best_move;

    public L6() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        B = new CXBoard(M, N, K);
        transpositionTable = new TranspositionTable(B);
        best_move = 1;
    }

    private void markColumnHash(int col) {
        transpositionTable.setCurrentHash(transpositionTable.zobrist[col][B.currentPlayer()]);
    }

    private void unmarkColumnHash() {
        CXCell c = B.getMarkedCells()[B.getMarkedCells().length - 1];
        transpositionTable.setCurrentHash(transpositionTable.zobrist[c.j][B.currentPlayer() == 1 ? 0 : 1]);
        B.unmarkColumn();
    }
    /*
     * Funzione per prendere l'ultima x in cui si è giocata la mossa in una data
     * colonna.
     */

    public static int getX(CXBoard B, int y) {
        int count = 0;
        for (int i = 0; i < B.M; i++) {
            if (B.cellState(i, y) != CXCellState.FREE) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
    // usare un array per salvare l'altezza raggiunta in ogni colonna

    public int eval(CXBoard board, int col) {

        int best = 0;
        int secondbest = 0;
        int last_row = getX(board, col);
        CXCellState s = board.cellState(last_row, col);
        int n;
        /*
         * if (s == CXCellState.FREE) {
         * return 1;
         * }
         */

        n = 1;
        for (int k = 1; col - k >= 0 && (board.cellState(last_row, col - k) == s
                || board.cellState(last_row, col - k) == CXCellState.FREE); k++) {
            if (board.cellState(last_row, col - k) == s)
                n++;
        } // backward check
        for (int k = 1; col + k < board.N && (board.cellState(last_row, col + k) == s
                || board.cellState(last_row, col + k) == CXCellState.FREE); k++) {
            if (board.cellState(last_row, col + k) == s)
                n++;
        } // forward check
        best = n;

        // Vertical check
        n = 1;
        for (int k = 1; last_row + k < board.M
                && (board.cellState(last_row + k, col) == s
                        || board.cellState(last_row + k, col) == CXCellState.FREE); k++) {
            if (board.cellState(last_row + k, col) == s)
                n++;
        }
        best = Math.max(best, n);
        secondbest = Math.min(best, n);

        // Diagonal check
        n = 1;
        for (int k = 1; last_row - k >= 0 && col - k >= 0
                && (board.cellState(last_row - k, col - k) == s || board.cellState(last_row - k, col - k) == s); k++) {
            if (board.cellState(last_row - k, col - k) == s)
                n++;
        } // backward check
        for (int k = 1; last_row + k < board.M && col + k < board.N
                && (board.cellState(last_row + k, col + k) == s || board.cellState(last_row + k, col + k) == s); k++) {
            if (board.cellState(last_row + k, col + k) == s)
                n++;
        } // forward check
        if (n > secondbest) {
            best = Math.max(best, n);
            secondbest = Math.min(best, n);
        }

        // Anti-diagonal check
        n = 1;
        for (int k = 1; last_row - k >= 0 && col + k < board.N
                && (board.cellState(last_row - k, col + k) == s || board.cellState(last_row - k, col
                        + k) == s); k++) {
            if (board.cellState(last_row - k, col + k) == s)
                n++;
        } // backward check
        for (int k = 1; last_row + k < board.M && col - k >= 0
                && (board.cellState(last_row + k, col - k) == s || board.cellState(last_row + k, col
                        - k) == s); k++) {
            if (board.cellState(last_row + k, col - k) == s)
                n++;
        } // forward check
        if (n > secondbest) {
            best = Math.max(best, n);
            secondbest = Math.min(best, n);
        }
        if (best >= B.X)
            return 9999; // in alternativa, Integer.MAX_VALUE
        else {
            int sol = (int) (best + secondbest * 0.5);
            return sol;
        }
    }

    public int alphabeta(int depth, int alpha, int beta, boolean maximizingPlayer) {
        int score = 1;
        // int score = transpositionTable.get(depth, alpha, beta);
        int localBestmove = 1; // inizializza localBestmove a -1
        int best_val = maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE; // inizializza best_val in base al tipo
                                                                                 // di giocatore
        int flag_type = transpositionTable.FLAG_EXACT;

        /*
         * if (score != transpositionTable.ERROR) {
         * System.out.println("Cache hit");
         * return score; // cache hit
         * }
         */ // problema: perchè mi trova sempre uno score = -1?

        if (depth == 0 || B.gameState() != CXGameState.OPEN) {
            if (B.gameState() == myWin) {
                return Integer.MAX_VALUE;
            } else if (B.gameState() == yourWin) { // aggiungi parentesi mancante
                return Integer.MIN_VALUE;
            } else {
                return eval(B, localBestmove);
            }
        }

        if (maximizingPlayer) {
            for (int i : B.getAvailableColumns()) {
                markColumnHash(i);
                B.markColumn(i);
                score = alphabeta(depth - 1, alpha, beta, false);
                // System.out.println("Il valore calcolato per la colonna" + i + "è: " + score);
                unmarkColumnHash();
                if (score > best_val) { // aggiorna il valore di best_val e localBestmove
                    best_val = score;
                    // System.out.println("Ha aggiornato il valore con " + best_val);
                    localBestmove = i;
                }
                if (best_val >= beta) {
                    flag_type = transpositionTable.FLAG_UPPER;
                    break;
                }
                alpha = Math.max(alpha, best_val);
            }
        } else {
            for (int i : B.getAvailableColumns()) {
                markColumnHash(i);
                B.markColumn(i);
                score = alphabeta(depth - 1, alpha, beta, true);
                unmarkColumnHash();
                if (score < best_val) { // aggiorna il valore di best_val e localBestmove
                    // System.out.println("Ha aggiornato il valore2");
                    best_val = score;
                    localBestmove = i;
                }
                if (best_val <= alpha) {
                    flag_type = transpositionTable.FLAG_LOWER;
                    break;
                }
                beta = Math.min(beta, best_val);
            }
        }
        System.out.println("The best move calcolated is" + localBestmove);
        transpositionTable.put(depth, localBestmove, best_val, flag_type); // aggiorna la tabella di
                                                                           // trasposizione
        return best_val;
    }

    // ASPIRATION SEARCH: Usata nell'iterative deeping per ridurre la finestra su
    // cui alphabeta va ad effettuare la ricerca
    // discutere scelta del valore treshold
    /*
     * public int IDAS(int max_depth, int treshold) {
     * int best = alphabeta(3, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
     * System.out.println("All inizio il valore di best è:" + best);
     * int current_depth_limit = 4;
     * START = System.currentTimeMillis();
     * while (current_depth_limit <= max_depth) {
     * int new_value = alphabeta(current_depth_limit, best - treshold, best +
     * treshold, false);
     * if (checktime()) {
     * break;
     * }
     * if (new_value <= best - treshold) { // failed_low
     * new_value = alphabeta(current_depth_limit, Integer.MIN_VALUE, new_value,
     * false);
     * } else if (new_value >= best + treshold) {
     * new_value = alphabeta(current_depth_limit, new_value, Integer.MAX_VALUE,
     * false);
     * }
     * best = new_value;
     * current_depth_limit += 2;
     * }
     * System.out.println(best);
     * return best;
     * }
     */

    private boolean checktime() {
        return (System.currentTimeMillis() - START) / 1000.0 > TIMEOUT * (99.0 / 100.0);
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)]; // Save a random column
        int bestValue = -1;
        int max_depth = B.getAvailableColumns().length;
        int bestMove = 1;
        System.out.println("Trying");
        for (int i : L) {
            if (checktime()) {
                break;
            }
            B.markColumn(i);
            if (B.gameState() == myWin) {
                return i;
            }
            // int value = IDAS(max_depth, 2);
            int value = alphabeta(7, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            B.unmarkColumn();
            if (value > bestValue) {
                System.out.println("E' entrato");
                bestValue = value;
                bestMove = i;
            }
            System.out.println("got here2");
        }
        // Select a random column if no valid moves are available
        if (bestMove == 1) {
            System.err.println("No valid moves available! Random column selected.");
            return save;
        }
        System.out.println("At least one valid move calculated");
        B.markColumn(bestMove);
        return bestMove;
    }

    public String playerName() {
        return "Alphabeta2";
    }
}

// aggiornare i valori nelle tabelle hash