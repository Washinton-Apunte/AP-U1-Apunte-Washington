import java.util.concurrent.atomic.AtomicLong;


public class Lamport {
    private final AtomicLong t;

    public Lamport(long inicial) {
        t = new AtomicLong(inicial);
    }


    public long tick() {
        return t.incrementAndGet();
    }

    public long recibir(long recibido) {
        return t.updateAndGet(local -> Math.max(local, recibido) + 1);
    }

    public long valor() {
        return t.get();
    }
}
