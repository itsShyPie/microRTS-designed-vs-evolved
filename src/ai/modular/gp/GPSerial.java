package ai.modular.gp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public final class GPSerial {
    private GPSerial() {}

    public static void save(GPNode n, String path) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(path))) {
            write(n, w);
        }
    }

    private static void write(GPNode n, PrintWriter w) {
        if (n.leaf) {
            w.println("L " + n.action);
        } else {
            w.println("I " + n.featureLeft + " " + (n.useConst ? 1 : 0) + " "
                    + n.constant + " " + n.featureRight + " " + (n.greater ? 1 : 0));
            write(n.t, w);
            write(n.f, w);
        }
    }

    public static GPNode load(String path) throws IOException {
        try (BufferedReader b = new BufferedReader(new FileReader(path))) {
            return read(b);
        }
    }

    private static GPNode read(BufferedReader b) throws IOException {
        String line = b.readLine();
        if (line == null) throw new IOException("unexpected end of GP tree file");
        String[] tok = line.trim().split("\\s+");
        if (tok[0].equals("L")) {
            return GPNode.leaf(Integer.parseInt(tok[1]));
        }
        GPNode n = new GPNode();
        n.leaf = false;
        n.featureLeft = Integer.parseInt(tok[1]);
        n.useConst = tok[2].equals("1");
        n.constant = Double.parseDouble(tok[3]);
        n.featureRight = Integer.parseInt(tok[4]);
        n.greater = tok[5].equals("1");
        n.t = read(b);
        n.f = read(b);
        return n;
    }
}
