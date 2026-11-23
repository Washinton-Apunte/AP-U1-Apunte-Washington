import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrdenTotal {
    public static void main(String[] args) throws IOException {
        List<String[]> eventos = new ArrayList<>();
        for (String nombre : new String[]{"coordinador", "V1", "V2", "V3"}) {
            Path f = Path.of("logs", nombre + ".log");
            if (!Files.exists(f)) continue;
            for (String linea : Files.readAllLines(f)) {
                if (linea.isBlank()) continue;
                eventos.add(linea.split(";", 4));
            }
        }
        eventos.sort(Comparator
                .<String[]>comparingLong(e -> Long.parseLong(e[0]))
                .thenComparingInt(e -> Integer.parseInt(e[1])));
        List<String> salida = new ArrayList<>();
        for (String[] e : eventos) salida.add(String.join(";", e));
        Files.write(Path.of("logs", "orden_total.log"), salida);
        System.out.println(eventos.size() + " eventos escritos en logs/orden_total.log");
    }
}

