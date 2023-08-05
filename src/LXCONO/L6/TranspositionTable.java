package LXCONO.L6;

import java.util.Random;

import connectx.CXBoard;

public class TranspositionTable {
    private static final int DEFAULT_SIZE = 1024 * 1024 * 4; // 4MB
    public static final int FLAG_EXACT = 0;
    public static final int FLAG_LOWER = 1;
    public static final int FLAG_UPPER = 2;
    public static final int ERROR = Integer.MIN_VALUE;

    private final int[][] table;
    final long[][] zobrist;
    private long currentHash;
    private int bestMove;

    public TranspositionTable(CXBoard board) {
        Random rand = new Random(System.currentTimeMillis());
        zobrist = new long[board.N][2];
        for (int j = 0; j < board.N; j++) {
            zobrist[j][0] = rand.nextLong();
            zobrist[j][1] = rand.nextLong();
        }
        table = new int[DEFAULT_SIZE][];
    }

    private int index() {
        return (int) Math.abs(currentHash % DEFAULT_SIZE);
    }

    public void put(int depth, int bestMove, int value, int flag) {
        int[] entry = table[index()];
        if (entry == null || entry[0] != currentHash) {
            entry = new int[6];
            entry[0] = (int) currentHash;
            entry[1] = (int) (currentHash >>> 32);
            table[index()] = entry;
        }
        entry[2] = depth;
        entry[3] = bestMove;
        entry[4] = value;
        entry[5] = flag;
    }

    public int get(int depth, int alpha, int beta) {
        int[] entry = table[index()];
        if (entry != null && entry[0] == currentHash && entry[2] >= depth) {
            if (entry[5] == FLAG_LOWER && entry[4] <= alpha) {
                return alpha;
            } else if (entry[5] == FLAG_UPPER && entry[4] >= beta) {
                return beta;
            } else if (entry[5] == FLAG_EXACT) {
                return entry[4];
            }
        }
        return ERROR;
    }

    public void setCurrentHash(long hash) {
        currentHash ^= hash;
    }

    public void setBestMove(int move) {
        bestMove = move;
    }

    public int getBestMove() {
        return bestMove;
    }
}

/*
 * Idea dietro lo zobrist system:
 * 1 -> generare un random long per ogni possibile cella della griglia
 * 2 -> si inizia con un hash = 0 e poi bisogna fare lo XOR con il valore
 * zobrist precedente per quella cella per ottenere l'indice di quella cella
 * nella
 * transposition table
 * FINE ZOBRIST
 * 
 * Transposition Table:
 * 1-> i valori nella trasposition table vanno cercati medinate l'indice hash
 * generato prima
 * 2-> per ogni entry nella transposition table dobbiamo memorizzare -> score,
 * bestmove, zobristkey, flag (ci dice quanto è buono il valore), profondità.
 * Articolo : https://stackoverflow.com/questions/20009796/transposition-tables
 * 
 * PROBLEMA:
 * Come inserire una chiave hash di tipo long in una transposition table di tipo
 * int?
 * SOLUZIONE: If you need to store a long value in a transposition table without
 * increasing the overall size of the table,
 * you can use a technique called key-value splitting. The basic idea is to
 * split the long key into two smaller int keys and store
 * them separately in the table along with the corresponding value.
 * 
 * 
 */
