package ai.modular.gp.score;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public final class ScoreSerial {
    private ScoreSerial() {}

    public static void save(ScoreNode n, String path) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(path))) {
            write(n, w);
        }
    }

    private static void write(ScoreNode n, PrintWriter w) {
        if (n.type == ScoreConfig.FEATURE) {
            w.println("F " + n.feature);
        } else if (n.type == ScoreConfig.CONST) {
            w.println("C " + n.value);
        } else {
            w.println("O " + n.type);
            for (ScoreNode k : n.kids) write(k, w);
        }
    }

    public static ScoreNode load(String path) throws IOException {
        try (BufferedReader b = new BufferedReader(new FileReader(path))) {
            return read(b);
        }
    }

    private static ScoreNode read(BufferedReader b) throws IOException {
        String line = b.readLine();
        if (line == null) throw new IOException("unexpected end of scoring-tree file");
        String[] tok = line.trim().split("\\s+");
        ScoreNode n = new ScoreNode();
        if (tok[0].equals("F")) {
            n.type = ScoreConfig.FEATURE;
            n.feature = Integer.parseInt(tok[1]);
        } else if (tok[0].equals("C")) {
            n.type = ScoreConfig.CONST;
            n.value = Double.parseDouble(tok[1]);
        } else {
            n.type = Integer.parseInt(tok[1]);
            int a = ScoreNode.arity(n.type);
            n.kids = new ScoreNode[a];
            for (int i = 0; i < a; i++) n.kids[i] = read(b);
        }
        return n;
    }
}
