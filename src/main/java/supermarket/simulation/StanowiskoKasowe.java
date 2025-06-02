package supermarket.simulation;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

// Wątek kasjera z użyciem semaforów
public class StanowiskoKasowe implements Runnable {

    private final int id; // Numer kasy

    private final List<Klient> kolejka = new LinkedList<>(); // Lista klientów czekających na obsługę

    private final Semaphore semKolejka = new Semaphore(0); // Semafor zliczający klientów w kolejce kasy
    private final Semaphore mutexKolejki = new Semaphore(1); // Binarny semafor, który zapewnia wyłączny dostęp do sekcji krytycznej

    private final AtomicBoolean czyOtwarta = new AtomicBoolean(false); // Flaga okreslająca, czy kasa przyjmuje nowych klientów

    // Konstruktor ustawiający numer kasy
    public StanowiskoKasowe(int id) {
        this.id = id;
    }

    public int id() { return id; }

    public int dlugoscKolejki() { return semKolejka.availablePermits(); }
    public boolean pobierzStanKasy() { return czyOtwarta.get(); }

    public void otworz() { czyOtwarta.set(true); }
    public void oznaczDoZamkniecia() { czyOtwarta.set(false); }

    // Dodanie klienta do listy
    public void dodajKlienta(Klient k) {
        try {
            mutexKolejki.acquire(); // Wejście do sekcji krytycznej, opuszczenie semafora
            kolejka.add(k); // Dodanie klienta do listy
        } catch (InterruptedException ignored) {
        } finally {
            mutexKolejki.release(); // Wyjście z sekcji krytycznej, podniesienie semafora
        }
        semKolejka.release(); // Dodanie klienta do kolejki
    }

    private final AtomicBoolean obsluga = new AtomicBoolean(false);
    public boolean czyObsluguje() { return obsluga.get(); }

    @Override
    public void run() {
        try {
            while (true) {
                semKolejka.acquire(); // Oczekiwanie na klienta

                Klient k;
                mutexKolejki.acquire(); // Wejście do sekcji krytycznej, tylko wątek kasy może manipulowac kolejką
                try {
                    k = kolejka.remove(0); // Pobranie pierwszego klienta z listy
                } finally {
                    mutexKolejki.release(); // Wyjście z sekcji krytycznej
                }

                obsluga.set(true); // Klient jest obsługiwany
                Thread.sleep(k.czasObslugi()); // Czas obsługi klienta
                obsluga.set(false); // Zakończenie obsługi klienta

                // Sprawdzenie, czy kasa może zostać zamknięta
                if (!czyOtwarta.get() && semKolejka.availablePermits() == 0 && czyObsluguje() == false) {
                    System.out.println("Kasa " + id + " zostaje zamknięta");
                    return;
                }

            }
        } catch (InterruptedException ignored) { }
    }
}
