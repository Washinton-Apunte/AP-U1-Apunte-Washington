import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    public final int cupo;
    public final String token;
    public final String host;
    public final int puerto;
    public final int latidoMs;
    public final int caidaMs;
    public final int timeoutMs;

    public Config(String ruta) throws IOException {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(ruta)) {
            p.load(in);
        }
        cupo     = Integer.parseInt(p.getProperty("cupo", "10").trim());
        token     = p.getProperty("token", "").trim();
        host      = p.getProperty("host", "localhost").trim();
        puerto    = Integer.parseInt(p.getProperty("puerto.coordinador", "5000").trim());
        latidoMs  = Integer.parseInt(p.getProperty("latido.ms", "2000").trim());
        caidaMs   = Integer.parseInt(p.getProperty("caida.ms", "6000").trim());
        timeoutMs = Integer.parseInt(p.getProperty("timeout.ms", "3000").trim());
    }
}
