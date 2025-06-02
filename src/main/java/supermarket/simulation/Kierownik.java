package supermarket.simulation;

import java.util.List;

// Wątek kierownika
public class Kierownik implements Runnable {

    private final Supermarket supermarket;

    public Kierownik(Supermarket supermarket) {
        this.supermarket = supermarket;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(500);  // Decyzja co pól sekundy
            } catch (InterruptedException e) {
                return;
            }
            
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
        }
    }
}
