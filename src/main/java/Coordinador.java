import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class Coordinador {
    static Config cfg;
    static Lamport reloj;
    static Bitacora log;
    static boolean inseguro;

    static volatile int ultimo = 0;
    static volatile int llamado = 0;
    static final Object cerrojo = new Object();
    static final Map<Integer, Long> ultimoLatido = new ConcurrentHashMap<>();
    static final Map<Integer, String> estadoVentanilla = new ConcurrentSkipListMap<>();

    public static void main(String[] args) throws Exception {
        inseguro = Arrays.asList(args).contains("--inseguro");
        cfg = new Config("config.properties");
        log = new Bitacora("coordinador", "0");
        reloj = new Lamport(log.ultimoLamport());
        log.registrar(reloj.tick(), "ARRANQUE", "cupo=" + cfg.cupo + " modo=" + (inseguro ? "INSEGURO" : "NORMAL"));
        System.out.println("Coordinador en puerto " + cfg.puerto + " cupo=" + cfg.cupo
                + (inseguro ? "  [MODO INSEGURO]" : ""));

        Thread monitor = new Thread(Coordinador::vigilarLatidos, "monitor");
        monitor.setDaemon(true);
        monitor.start();

        try (ServerSocket servidor = new ServerSocket(cfg.puerto)) {
            while (true) {
                Socket s = servidor.accept();
                new Thread(() -> atender(s)).start();
            }
        }
    }

    static void atender(Socket s) {
        try (s;
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {
            while (true) {
                String mensaje = Trama.leer(in);
                Trama.escribir(out, procesar(mensaje));
            }
        } catch (EOFException e) {
        } catch (IOException e) {
            log.registrar(reloj.tick(), "ERROR_CONEXION", e.getMessage());
        }
    }

    static String procesar(String mensaje) {
        String tipo;
        String token;
        int ventanilla;
        long recibido;
        try {
            String[] p = mensaje.split("\\|", -1);
            tipo = p[0];
            token = p[1];
            ventanilla = Integer.parseInt(p[2]);
            recibido = Long.parseLong(p[3]);
        } catch (RuntimeException e) {
            return responder("?", 0, "RECHAZADA", "mensaje mal formado", 0, true);
        }
        long t = reloj.recibir(recibido);

        if (!tokenValido(token)) {
            log.registrar(t, "TOKEN_INVALIDO", "tipo=" + tipo + " ventanilla=" + ventanilla);
            return responder(tipo, ventanilla, "RECHAZADA", "token invalido", 0, true);
        }
        if (tipo.equals("LATIDO")) {
            registrarLatido(ventanilla, t);
            return responder(tipo, ventanilla, "ESTADO", "latido", 0, false);
        }
        log.registrar(t, "RECIBE_" + tipo, "ventanilla=" + ventanilla + " lamport_msg=" + recibido);

        String resultado = "ESTADO";
        String motivo = "";
        int numero = 0;
        switch (tipo) {
            case "TURNO" -> {
                resultado = "ASIGNADO";
                if (inseguro) {
                    int leido = ultimo;
                    if (leido < cfg.cupo) {
                        pausa(50);
                        ultimo = leido + 1;
                        numero = leido + 1;
                    } else {
                        resultado = "RECHAZADA";
                        motivo = "cupo agotado";
                    }
                } else {
                    synchronized (cerrojo) {
                        if (ultimo < cfg.cupo) {
                            ultimo++;
                            numero = ultimo;
                        } else {
                            resultado = "RECHAZADA";
                            motivo = "cupo agotado";
                        }
                    }
                }
            }
            case "LLAMAR" -> {
                synchronized (cerrojo) {
                    if (llamado < ultimo) {
                        llamado++;
                        numero = llamado;
                        resultado = "LLAMADO";
                    } else {
                        resultado = "RECHAZADA";
                        motivo = "no hay turnos en espera";
                    }
                }
            }
            case "ESTADO" -> { /* solo consulta */ }
            default -> {
                resultado = "RECHAZADA";
                motivo = "tipo desconocido";
            }
        }
        return responder(tipo, ventanilla, resultado, motivo, numero, true);
    }

    static String responder(String tipo, int ventanilla, String resultado, String motivo, int numero, boolean anotar) {
        long t = reloj.tick();
        if (anotar) {
            log.registrar(t, "ENVIA_RESPUESTA",
                    tipo + " ventanilla=" + ventanilla + " " + resultado + " numero=" + numero + " " + motivo);
        }
        return "RESPUESTA|" + resultado + "|" + motivo + "|" + numero + "|" + ultimo + "|" + cfg.cupo + "|"
                + llamado + "|" + textoVentanillas() + "|" + t;
    }

    static boolean tokenValido(String recibido) {
        return MessageDigest.isEqual(recibido.getBytes(StandardCharsets.UTF_8),
                cfg.token.getBytes(StandardCharsets.UTF_8));
    }

    static void registrarLatido(int ventanilla, long t) {
        ultimoLatido.put(ventanilla, System.currentTimeMillis());
        String antes = estadoVentanilla.put(ventanilla, "ACTIVA");
        if (!"ACTIVA".equals(antes)) {
            log.registrar(t, "VENTANILLA_ACTIVA", "ventanilla=" + ventanilla);
        }
    }

    static void vigilarLatidos() {
        while (true) {
            pausa(500);
            long ahora = System.currentTimeMillis();
            for (Map.Entry<Integer, Long> e : ultimoLatido.entrySet()) {
                long silencio = ahora - e.getValue();
                if (silencio > cfg.caidaMs && "ACTIVA".equals(estadoVentanilla.get(e.getKey()))) {
                    estadoVentanilla.put(e.getKey(), "CAIDA");
                    log.registrar(reloj.tick(), "VENTANILLA_CAIDA",
                            "ventanilla=" + e.getKey() + " sin latidos " + silencio + " ms");
                }
            }
        }
    }

    static String textoVentanillas() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 3; i++) {
            if (i > 1) sb.append(',');
            sb.append(i).append('=').append(estadoVentanilla.getOrDefault(i, "SIN_LATIDOS"));
        }
        return sb.toString();
    }

    static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
