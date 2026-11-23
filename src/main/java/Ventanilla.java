import java.util.Scanner;

public class Ventanilla {
    static Config cfg;
    static int id;
    static Lamport reloj;
    static Bitacora log;
    static volatile boolean coordinadorOk = true;

    public static void main(String[] args) throws Exception {
        id = Integer.parseInt(args[0]);
        cfg = new Config("config.properties");
        log = new Bitacora("V" + id, String.valueOf(id));
        reloj = new Lamport(log.ultimoLamport());
        log.registrar(reloj.tick(), "ARRANQUE", "ventanilla=" + id);

        Thread latidos = new Thread(Ventanilla::enviarLatidos, "latidos");
        latidos.setDaemon(true);
        latidos.start();

        System.out.println("Ventanilla V" + id + " lista. Ordenes: turno | llamar | estado | tokenmalo | fin");
        Scanner consola = new Scanner(System.in);
        while (true) {
            System.out.print("V" + id + "> ");
            if (!consola.hasNextLine()) break;
            String orden = consola.nextLine().trim().toLowerCase();
            switch (orden) {
                case "turno" -> mostrar(Cliente.solicitar(cfg, id, reloj, log, "TURNO", cfg.token));
                case "llamar" -> mostrar(Cliente.solicitar(cfg, id, reloj, log, "LLAMAR", cfg.token));
                case "estado" -> mostrar(Cliente.solicitar(cfg, id, reloj, log, "ESTADO", cfg.token));
                case "tokenmalo" -> mostrar(Cliente.solicitar(cfg, id, reloj, log, "TURNO", "token-incorrecto"));
                case "fin" -> {
                    log.registrar(reloj.tick(), "FIN", "ventanilla=" + id);
                    return;
                }
                case "" -> { }
                default -> System.out.println("Orden desconocida");
            }
        }
    }

    static void mostrar(String[] r) {
        if (r == null) {
            System.out.println("RECHAZADA: coordinador no disponible; no se entregan turnos sin su autorizacion");
            return;
        }
        String num = r[3].equals("0") ? "" : " numero=" + r[3];
        System.out.println(r[1] + (r[2].isEmpty() ? "" : " (" + r[2] + ")") + num
                + "  emitidos=" + r[4] + " cupo=" + r[5] + " llamados=" + r[6] + "  ventanillas[" + r[7] + "]");
    }

    static void enviarLatidos() {
        while (true) {
            String[] r = Cliente.solicitar(cfg, id, reloj, null, "LATIDO", cfg.token);
            if (r == null && coordinadorOk) {
                coordinadorOk = false;
                log.registrar(reloj.tick(), "COORDINADOR_CAIDO", "sin respuesta a latidos");
            } else if (r != null && !coordinadorOk) {
                coordinadorOk = true;
                log.registrar(reloj.tick(), "COORDINADOR_RECUPERADO", "responde de nuevo");
            }
            try {
                Thread.sleep(cfg.latidoMs);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
