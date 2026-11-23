import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PruebaCarga {
    public static void main(String[] args) throws Exception {
        Config cfg = new Config("config.properties");
        AtomicInteger asignadas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        Set<Integer> distintos = ConcurrentHashMap.newKeySet();
        CountDownLatch salida = new CountDownLatch(1);
        List<Thread> hilos = new ArrayList<>();

        for (int v = 1; v <= 3; v++) {
            Lamport reloj = new Lamport(0);
            for (int k = 0; k < 10; k++) {
                final int ventanilla = v;
                Thread h = new Thread(() -> {
                    try {
                        salida.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    String[] r = Cliente.solicitar(cfg, ventanilla, reloj, null, "TURNO", cfg.token);
                    if (r != null && r[1].equals("ASIGNADO")) {
                        asignadas.incrementAndGet();
                        distintos.add(Integer.parseInt(r[3]));
                    } else {
                        rechazadas.incrementAndGet();
                    }
                });
                hilos.add(h);
                h.start();
            }
        }
        salida.countDown();
        for (Thread h : hilos) h.join();

        String[] e = Cliente.solicitar(cfg, 1, new Lamport(0), null, "ESTADO", cfg.token);
        System.out.println("Solicitudes lanzadas        : 30");
        System.out.println("Asignadas                   : " + asignadas.get());
        System.out.println("Rechazadas                  : " + rechazadas.get());
        System.out.println("Numeros distintos entregados: " + distintos.size() + "  " + new java.util.TreeSet<>(distintos));
        System.out.println("Emitidos segun coordinador  : " + (e == null ? "sin respuesta" : e[4])
                + " (cupo " + cfg.cupo + ")");
    }
}

