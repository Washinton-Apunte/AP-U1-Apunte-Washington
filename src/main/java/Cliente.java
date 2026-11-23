import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;


public class Cliente {

    static String[] solicitar(Config cfg, int ventanilla, Lamport reloj, Bitacora log, String tipo, String token) {
        long t = reloj.tick();
        if (log != null) log.registrar(t, "ENVIA_" + tipo, "ventanilla=" + ventanilla);
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(cfg.host, cfg.puerto), cfg.timeoutMs);
            s.setSoTimeout(cfg.timeoutMs);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            Trama.escribir(out, tipo + "|" + token + "|" + ventanilla + "|" + t);
            String[] r = Trama.leer(in).split("\\|", -1);
            long t2 = reloj.recibir(Long.parseLong(r[8]));
            if (log != null) log.registrar(t2, "RECIBE_RESPUESTA",
                    tipo + " resultado=" + r[1] + " numero=" + r[3] + " " + r[2]);
            return r;
        } catch (IOException | RuntimeException e) {
            if (log != null) {
                log.registrar(reloj.tick(), "COORDINADOR_NO_RESPONDE",
                        tipo + " " + e.getClass().getSimpleName());
            }
            return null;
        }
    }
}
