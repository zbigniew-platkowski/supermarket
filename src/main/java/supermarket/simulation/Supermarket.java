package supermarket.simulation;

import supermarket.config.KonfiguracjaSymulacji;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Supermarket implements Klient.PowiadomienieSupermarketu {

    private final KonfiguracjaSymulacji cfg;

    private final List<StanowiskoKasowe> stanowiska = new ArrayList<>();
    
    // Lista klientow w sklepie niebedacych w kolejce
    private final List<Klient> wSklepiePozaKolejka = new ArrayList<>();
    private final Semaphore mutexWSklepiePozaKolejka = new Semaphore(1);

    private final ScheduledExecutorService generator = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService pulaKierownika = Executors.newSingleThreadExecutor();
    
    private final AtomicInteger wskaznikKasy = new AtomicInteger(0);
    
    private volatile boolean wejscieZamkniete = false;

    private final ExecutorService pulaStanowisk;
    public Supermarket(KonfiguracjaSymulacji cfg) {
        this.cfg = cfg;

        this.pulaStanowisk = Executors.newFixedThreadPool(cfg.liczbaKas());

        for (int i = 0; i < cfg.liczbaKas(); i++) {
            stanowiska.add(new StanowiskoKasowe(i + 1));
        }
    }

    public List<StanowiskoKasowe> stanowiskaKasowe() { return stanowiska; }
    public int liczbaKlientowNaKase() { return cfg.liczbaKlientowNaKase(); }
    public int liczbaKas() { return cfg.liczbaKas(); }
    public int minLiczbaKasOtwartych() { return cfg.minLiczbaKasOtwartych(); }

    public int liczbaKlientowWSklepie() {
        int robiacyZakupy;
        try {
            mutexWSklepiePozaKolejka.acquire();
            robiacyZakupy = wSklepiePozaKolejka.size();
        } catch (InterruptedException e) {
            robiacyZakupy = 0;
        } finally {
            mutexWSklepiePozaKolejka.release();
        }
        int wKolejce = stanowiska.stream().mapToInt(StanowiskoKasowe::dlugoscKolejki).sum();
        int obslugiwani = (int) stanowiska.stream().filter(StanowiskoKasowe::czyObsluguje).count();
        return robiacyZakupy + wKolejce + obslugiwani;
    }

    public int liczbaKlientowWKolejkach() {
        return stanowiska.stream().mapToInt(StanowiskoKasowe::dlugoscKolejki).sum();
    }

    public void start() {
        System.out.println("Sklep zostaje otwarty");

        for (int i = 0; i < cfg.minLiczbaKasOtwartych(); i++) {
            StanowiskoKasowe sk = stanowiska.get(i);
            sk.otworz();
            pulaStanowisk.submit(sk);
        }

        pulaKierownika.submit(new Kierownik(this));

        // Generator klientów
        double lambda   = cfg.lambdaPoissonNaSekunde();
        long okresMs = 100;
        generator.scheduleAtFixedRate(() -> {
            int toGen = poisson(lambda * okresMs / 1000.0);
            for (int i = 0; i < toGen; i++) utworzKlienta();
        }, 0, okresMs, TimeUnit.MILLISECONDS);

        generator.schedule(this::zamknijWejscieDoSklepu, cfg.czasTrwaniaSymulacji(), TimeUnit.SECONDS);
    }

    public void koniecSymulacji() {

        stanowiska.forEach(sk -> {
            if(sk.pobierzStanKasy()) {sk.oznaczDoZamkniecia();
                System.out.println("Kasa " + sk.id() + " zostaje zamknięta");
            }
        });

        pulaKierownika.shutdownNow();
        pulaStanowisk.shutdownNow();

        System.out.println("Sklep zostaje zamkniety – wszystkie kasy zamknięte");
    }

    private void zamknijWejscieDoSklepu() {
        wejscieZamkniete = true;
        generator.shutdownNow();
        System.out.println("Wejście do sklepu zostaje zamknięte");
    }

    public void czyMoznaZamknacSklep() {
        if (wejscieZamkniete && liczbaKlientowWSklepie() == 0)
        {
            koniecSymulacji();
        }
    }
    @Override
    // Koniec zakupow - idz do kasy
    public void koniecZakupow(Klient klient) {
        try {
            mutexWSklepiePozaKolejka.acquire();
            wSklepiePozaKolejka.remove(klient);
        } catch (InterruptedException ignored) {
        } finally {
            mutexWSklepiePozaKolejka.release();
        }
        
        // Wybór najkrótszej kolejki
        StanowiskoKasowe najkrotszaKolejka = stanowiska.stream()
                .filter(StanowiskoKasowe::pobierzStanKasy)
                .min(Comparator.comparingInt(StanowiskoKasowe::dlugoscKolejki))
                .orElseThrow();
        
        System.out.println("Klient-" + klient.id() + " podchodzi do kasy " + najkrotszaKolejka.id());
        
        najkrotszaKolejka.dodajKlienta(klient);
    }

    public void otworzKolejnaKase() {
        for (int i = 0; i < stanowiska.size(); i++) {
            int idKolejnejKasy = wskaznikKasy.getAndIncrement() % stanowiska.size();
            StanowiskoKasowe sk = stanowiska.get(idKolejnejKasy);
            if (!sk.pobierzStanKasy()) {
                sk.otworz();
                pulaStanowisk.submit(sk);
                System.out.println("Kasa " + sk.id() + " zostaje otwarta");
                break;
            }
        }
    }

    public void zamknijKase() {
        stanowiska.stream()
                .filter(StanowiskoKasowe::pobierzStanKasy)
                .findFirst()
                .ifPresent(d -> {
                    d.oznaczDoZamkniecia();
                    System.out.println("Kasa " + d.id() + " do zamknięcia");
                });
    }

    private void utworzKlienta() {
        Klient k = Klient.random(
                cfg.minCzasZakupow(), cfg.maxCzasZakupow(),
                cfg.minCzasObslugi(), cfg.maxCzasObslugi(),
                this
        );
        try {
            mutexWSklepiePozaKolejka.acquire();
            wSklepiePozaKolejka.add(k);
        } catch (InterruptedException ignored) {
        } finally {
            mutexWSklepiePozaKolejka.release();
        }
        System.out.println("Klient-" + k.id() + " wchodzi do sklepu");
        new Thread(k).start();
    }

    private static int poisson(double lambda) {
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do { k++; p *= ThreadLocalRandom.current().nextDouble(); } while (p > l);
        return k - 1;
    }
}