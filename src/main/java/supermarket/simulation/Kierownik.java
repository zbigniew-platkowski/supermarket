package supermarket.simulation;

import java.util.List;
import java.util.concurrent.Semaphore;

// Wątek kierownika
public class Kierownik implements Runnable {

    private final Supermarket supermarket;

    public Kierownik(Supermarket supermarket) {
        this.supermarket = supermarket;
    }

    @Override
    public void run() {
        Semaphore wake = supermarket.stateSem();
        while (!Thread.currentThread().isInterrupted()) {
            
            // Liczba klientow w sklepie
            int liczbaKlientow = supermarket.liczbaKlientowWSklepie();
            List<StanowiskoKasowe> kasy = supermarket.stanowiskaKasowe();
            long otwarteKasy = kasy.stream().filter(StanowiskoKasowe::pobierzStanKasy).count();

            int potrzebneKasyOtwarte = Math.min(
                    supermarket.liczbaKas(),
                    Math.max(
                            supermarket.minLiczbaKasOtwartych(),
                            (int) Math.ceil((double) liczbaKlientow / supermarket.liczbaKlientowNaKase())
                    )
            );

            if (potrzebneKasyOtwarte > otwarteKasy) {
                supermarket.otworzKolejnaKase();
            } else if (potrzebneKasyOtwarte < otwarteKasy && liczbaKlientow < supermarket.liczbaKlientowNaKase() * (otwarteKasy - 1)) {
                supermarket.zamknijKase();
            }

            supermarket.czyMoznaZamknacSklep();

            try {
                wake.acquire();
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }
}
