import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class Trama {
    static final int MAX = 64 * 1024;

    static void escribir(DataOutputStream out, String mensaje) throws IOException {
        byte[] cuerpo = mensaje.getBytes(StandardCharsets.UTF_8);
        out.writeInt(cuerpo.length);
        out.write(cuerpo);
        out.flush();
    }

    static String leer(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > MAX) {
            throw new IOException("trama invalida, longitud=" + n);
        }
        byte[] cuerpo = new byte[n];
        in.readFully(cuerpo);
        return new String(cuerpo, StandardCharsets.UTF_8);
    }
}
