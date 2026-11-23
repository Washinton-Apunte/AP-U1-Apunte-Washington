import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Bitacora {
    private final Path archivo;
    private final String id;

    public Bitacora(String proceso, String id) throws IOException {
        this.id = id;
        Files.createDirectories(Path.of("logs"));
        this.archivo = Path.of("logs", proceso + ".log");
    }

    public synchronized void registrar(long lamport, String evento, String detalle) {
        String linea = lamport + ";" + id + ";" + evento + ";" + detalle + System.lineSeparator();
        try {
            Files.writeString(archivo, linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("No pude escribir la bitacora: " + e.getMessage());
        }
    }

    public long ultimoLamport() {
        long mayor = 0;
        try {
            if (!Files.exists(archivo)) return 0;
            for (String linea : Files.readAllLines(archivo)) {
                if (linea.isBlank()) continue;
                mayor = Math.max(mayor, Long.parseLong(linea.split(";", 4)[0]));
            }
        } catch (IOException | NumberFormatException e) {
            // si algo falla, seguimos con lo que se leyo
        }
        return mayor;
    }
}
