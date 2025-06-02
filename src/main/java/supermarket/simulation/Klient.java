package supermarket.simulation;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

// Wątek klienta - porusza się po sklepie, a potem idzie do kasy
public class Klient implements Runnable {

    private static final AtomicInteger KOLEJNE_ID = new AtomicInteger(1);

    private final int id = KOLEJNE_ID.getAndIncrement(); // ID klienta

    private final PowiadomienieSupermarketu powiadomienie;
    private final int czasZakupow;
    private final int czasObslugi;

    public interface PowiadomienieSupermarketu {
        void koniecZakupow(Klient klient);
    }

    public Klient(int czasZakupow, int czasObslugi, PowiadomienieSupermarketu powiadomienie) {
        this.czasZakupow = czasZakupow;
        this.czasObslugi = czasObslugi;
        this.powiadomienie = powiadomienie;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(czasZakupow); // Chodzenie klienta po sklepie
            powiadomienie.koniecZakupow(this); // Przejscie do kasy
        } catch (InterruptedException ignored) { }
    }

    public int id() { return id; }
    public int czasObslugi() { return czasObslugi; }

    // Tworzenie losowych czasow klientow
    public static Klient random(int minCzasZakupow, int maxCzasZakupow, int minCzasObslugi, int maxCzasObslugi, PowiadomienieSupermarketu p) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int sklep = rnd.nextInt(minCzasZakupow, maxCzasZakupow + 1);
        int obsluga = rnd.nextInt(minCzasObslugi, maxCzasObslugi + 1);
        return new Klient(sklep, obsluga, p);
    }
}
