package LXCONO.L3;

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

public class L3 implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;

    public L3() {

    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
    }

    public static int getX(CXBoard B, int y) {
        int count = 0;
        for (int i = 1; i < B.M; i++) {
            if (B.cellState(i, y) != CXCellState.FREE) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public int eval(CXBoard board, int col) {

        int best = 0;
        int secondbest = 0;
        int last_row = getX(board, col);
        CXCellState s = board.cellState(last_row, col);
        int n = 1;

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
        for (int k = 1; last_row + k < board.M && (board.cellState(last_row + k, col) == s
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
        if (best >= board.X)
            return best + secondbest; // in alternativa, Integer.MAX_VALUE
        else {
            int sol = (int) (best + secondbest * 0.5);
            return sol;
        }
    }

    public int alphabeta(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            if (board.gameState() != CXGameState.OPEN && board.gameState() == myWin) {
                return Integer.MAX_VALUE; // win
            } else if (board.gameState() != CXGameState.OPEN && board.gameState() == yourWin) {
                return Integer.MIN_VALUE; // loss
            } else {
                int evaluate = eval(board, board.getLastMove().j);
                // System.out.println(evaluate);
                return evaluate;
            }
        }
        // Massimizzare la valutazione per il giocatore corrente
        if (maximizingPlayer) {
            int bestValue = Integer.MIN_VALUE;
            for (int column : board.getAvailableColumns()) {
                // Effettuare la mossa sulla colonna selezionata
                board.markColumn(column);

                // Calcolare il valore della mossa
                int value = alphabeta(board, depth - 1, alpha, beta, false);
                bestValue = Math.max(bestValue, value);

                // Aggiornare il valore di alpha
                alpha = Math.max(alpha, bestValue);

                // Annullare la mossa effettuata sulla colonna selezionata
                board.unmarkColumn();

                // Verificare se si può tagliare il ramo
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        }

        // Minimizzare la valutazione per l'avversario
        else {
            int bestValue = Integer.MAX_VALUE;
            for (int column : board.getAvailableColumns()) {
                // Effettuare la mossa sulla colonna selezionata dall'avversario
                board.markColumn(column);

                // Calcolare il valore della mossa
                int value = alphabeta(board, depth - 1, alpha, beta, true);
                bestValue = Math.min(bestValue, value);

                // Aggiornare il valore di beta
                beta = Math.min(beta, bestValue);

                // Annullare la mossa effettuata sulla colonna selezionata
                board.unmarkColumn();

                // Verificare se si può tagliare il ramo
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        }
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)]; // Save a random column
        int bestValue = -1;

        try {
            int bestMove = -1;
            System.out.println("Trying");
            for (int i : L) {
                checktime();
                B.markColumn(i);
                if (B.gameState() == myWin) {
                    return i;
                } else if (B.gameState() == yourWin) {
                    System.out.println("Avoided");
                    return i;
                }
                int value = alphabeta(B, 6, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                B.unmarkColumn();
                if (value > bestValue) {
                    bestValue = value;
                    bestMove = i;
                }
                System.out.println("got here2");
            }
            // Select a random column if no valid moves are available
            if (bestMove == -1) {
                System.err.println("No valid moves available! Random column selected.");
                return save;
            }
            System.out.println("At least one valid move calculated");
            return bestMove;
        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Random column selected");
            return save;
        }
    }

    /**
     * Check if we can block adversary's victory
     *
     * Returns a blocking column if there is one, otherwise a random one
     */

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    public String playerName() {
        return "L3E";
    }

}
