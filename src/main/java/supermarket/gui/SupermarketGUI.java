package supermarket.gui;

import supermarket.config.KonfiguracjaSymulacji;
import supermarket.simulation.StanowiskoKasowe;
import supermarket.simulation.Supermarket;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SupermarketGUI extends JFrame {

    private static final int DLUGOSC_KAFLA = 384;
    private static final int DLUGOSC_IKONY = 288;
    private static final int H_ODSTEP = 16, V_ODSTEP = 16; // Odstępy między kaflami
    private static final Dimension WIELKOSC_KAFLA = new Dimension(DLUGOSC_KAFLA, DLUGOSC_KAFLA);

    private final Supermarket supermarket;

    // Etykiety znajdujące się nagłówku
    private final JLabel calkowitaLiczbaKlientowEtykieta = new JLabel("W sklepie: 0", SwingConstants.CENTER);
    private final JLabel kolejkaLiczbaKlientowEtykieta = new JLabel("Przy kasach: 0", SwingConstants.CENTER);
    private final JLabel pozostalyCzas = new JLabel("Pozostały czas: 0 s", SwingConstants.CENTER);
    
    // Przypisanie klasom etykiet
    private final Map<StanowiskoKasowe, JLabel> etykietyKas = new HashMap<>();
    private final Map<StanowiskoKasowe, JLabel> etykietyStatusu = new HashMap<>();

    // Przypisanie nazw do odpowiednich ikon
    private final Map<String, ImageIcon> ikony = new HashMap<>();

    private final Timer odswiezTimer;
    private final long koniecSymulacji;

    public SupermarketGUI(KonfiguracjaSymulacji cfg) {
        super("Supermarket"); // Tytuł okna

        this.supermarket = new Supermarket(cfg);
        this.koniecSymulacji = System.currentTimeMillis() + cfg.czasTrwaniaSymulacji() * 1000L;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // Zamykanie okna
        setLayout(new BorderLayout(8, 8)); // 8px odstępu

        utworzNaglowek();
        utworzKafleKas();

        // Ustawienie wielkości okna tak, żeby wszystkie 4 kafle kas w wierszu się zmieściły
        setSize(4 * (DLUGOSC_KAFLA + H_ODSTEP) + 4 * H_ODSTEP, 1000);

        // Ustawienie okna na środku monitora
        setLocationRelativeTo(null);

        // Odświeżanie co 200 milisekund
        odswiezTimer = new Timer(200, e -> odswiezGUI());
        odswiezTimer.start();

        supermarket.start();
    }

    // Nagłówek informujący o liczbie klientów w sklepie, przy kasach oraz czasie do zamknięcia sklepu
    private void utworzNaglowek() {
        JPanel naglowek = new JPanel(new GridLayout(3, 1));
        Font f = calkowitaLiczbaKlientowEtykieta.getFont().deriveFont(Font.BOLD, 24f);
        calkowitaLiczbaKlientowEtykieta.setFont(f);
        kolejkaLiczbaKlientowEtykieta.setFont(f);
        pozostalyCzas.setFont(f);
        naglowek.add(calkowitaLiczbaKlientowEtykieta);
        naglowek.add(kolejkaLiczbaKlientowEtykieta);
        naglowek.add(pozostalyCzas);
        add(naglowek, BorderLayout.NORTH);
    }

    // Tworzenie kafli każdej kasy
    private void utworzKafleKas() {
        ikony.put("otwarta_obslugiwany_0_kolejka_0", loadIcon("otwarta_obslugiwany_0_kolejka_0.png"));
        ikony.put("otwarta_obslugiwany_1_kolejka_0", loadIcon("otwarta_obslugiwany_1_kolejka_0.png"));
        ikony.put("otwarta_obslugiwany_1_kolejka_1", loadIcon("otwarta_obslugiwany_1_kolejka_1.png"));
        ikony.put("otwarta_obslugiwany_1_kolejka_2", loadIcon("otwarta_obslugiwany_1_kolejka_2.png"));
        ikony.put("otwarta_obslugiwany_1_kolejka_3", loadIcon("otwarta_obslugiwany_1_kolejka_3.png"));
        ikony.put("otwarta_obslugiwany_1_kolejka_4", loadIcon("otwarta_obslugiwany_1_kolejka_4.png"));
        ikony.put("otwarta_obslugiwany_1_kolejka_5_i_wiecej", loadIcon("otwarta_obslugiwany_1_kolejka_5_i_wiecej.png"));
        ikony.put("zamknieta", loadIcon("zamknieta.png"));

        JPanel grid = new JPanel(new GridLayout(0, 4, H_ODSTEP, V_ODSTEP));
        grid.setBorder(BorderFactory.createEmptyBorder(V_ODSTEP, H_ODSTEP, V_ODSTEP, H_ODSTEP));

        for (StanowiskoKasowe k : supermarket.stanowiskaKasowe()) {

            // Na początku kasy zamknięte - wyswietlenie odpowiedniego obrazka i liczba klientów 0
            // Etykieta zawierająca ikonę i tekst kasy
            JLabel etykietaKasy = new JLabel("Kasa " + k.id() + ": 0",
                    ikony.get("zamknieta"),
                    SwingConstants.CENTER);

            etykietaKasy.setHorizontalTextPosition(SwingConstants.CENTER);
            etykietaKasy.setVerticalTextPosition(SwingConstants.TOP);
            etykietaKasy.setIconTextGap(-48); // Nałożenie tekstu na wiszącą część obrazka u góry

            JLabel status = new JLabel("KASA ZAMKNIĘTA", SwingConstants.CENTER);
            status.setFont(status.getFont().deriveFont(Font.BOLD, 18f));

            // Tworzenie panelu wewnętrznego, żeby ułożyć etykietyKasy i status
            JPanel inner = new JPanel();
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            inner.setOpaque(false);

            etykietaKasy.setAlignmentX(Component.CENTER_ALIGNMENT);
            status.setAlignmentX(Component.CENTER_ALIGNMENT);

            inner.add(etykietaKasy);
            inner.add(Box.createVerticalStrut(-60)); // Odległość między obrazem a napisem
            inner.add(status);

            // Dodanie panelu zawierającego kafle kasy
            JPanel kafelKasy = new JPanel(new BorderLayout());
            kafelKasy.setPreferredSize(WIELKOSC_KAFLA);
            kafelKasy.add(inner, BorderLayout.CENTER);
            kafelKasy.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

            etykietyKas.put(k, etykietaKasy);
            etykietyStatusu.put(k, status);
            grid.add(kafelKasy);
        }

        // Przewijanie
        JScrollPane scroll = new JScrollPane(grid,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
    }
    
    private void odswiezGUI() {
        calkowitaLiczbaKlientowEtykieta.setText("W sklepie: " + supermarket.liczbaKlientowWSklepie());
        kolejkaLiczbaKlientowEtykieta.setText("W kolejce do kasy: " + supermarket.liczbaKlientowWKolejkach());
        long pozostalo = Math.max(0, (koniecSymulacji - System.currentTimeMillis()) / 1000);
        pozostalyCzas.setText("Pozostały czas: " + pozostalo + " s");

        etykietyKas.forEach((kasa, etykietaKasy) -> {
            int wKolejce = kasa.dlugoscKolejki();
            boolean obslugiwany = kasa.czyObsluguje(); // Sprawdzenie, czy ktos w ogole jest obslugiwany przy kasie
            boolean calkowiteZamkniecie = !kasa.pobierzStanKasy() && wKolejce == 0 && obslugiwany == false;
            // Jeżeli kasa nie jest otwarta i ma 0 klientów, to znaczy, że musi być całkowicie zamknięta,
            // żeby odróżnić od przypadku, gdy kasa jest otwarta i ma 0 klientów, bo co najmniej dwie muszą być zawsze otwarte

            String stan;
            if (calkowiteZamkniecie) {
                stan = "zamknieta";
            } else if(obslugiwany) {
                if(wKolejce >= 5) {
                    stan = "otwarta_obslugiwany_1_kolejka_5_i_wiecej";
                } else {
                    stan = "otwarta_obslugiwany_1_kolejka_" + wKolejce;
                }
            } else {
                stan = "otwarta_obslugiwany_0_kolejka_0";
            }
            
            etykietaKasy.setIcon(ikony.get(stan));
            etykietaKasy.setText("Kolejka kasy " + kasa.id() + ": " + wKolejce);

            JLabel status = etykietyStatusu.get(kasa);
            if (kasa.pobierzStanKasy()) {
                status.setText("KASA OTWARTA");
                status.setForeground(new Color(0x009900));
            } else if (calkowiteZamkniecie) {
                status.setText("KASA ZAMKNIĘTA");
                status.setForeground(new Color(0xcc0000));
            } else {
                status.setText("DO ZAMKNIĘCIA");
                status.setForeground(new Color(0xff8800));
            }
        });
    }

    // Ładowanie obrazów kas
    private ImageIcon loadIcon(String nazwaPliku) {
        java.net.URL url = getClass().getResource("/img/" + nazwaPliku);
        if (url == null) throw new IllegalStateException("Brak pliku ikony: " + nazwaPliku);

        // Skalowanie obrazu
        Image img = new ImageIcon(url).getImage().getScaledInstance(DLUGOSC_IKONY, DLUGOSC_IKONY, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
}