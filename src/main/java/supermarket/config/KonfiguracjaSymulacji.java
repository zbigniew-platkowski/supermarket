package supermarket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;

public record KonfiguracjaSymulacji(
        int liczbaKas,
        int minLiczbaKasOtwartych,
        int liczbaKlientowNaKase,
        double lambdaPoissonNaSekunde,
        int minCzasZakupow,
        int maxCzasZakupow,
        int minCzasObslugi,
        int maxCzasObslugi,
        int czasTrwaniaSymulacji
) {

    // Ładowanie pliku konfiguracyjnego YAML
    public static KonfiguracjaSymulacji load(String sciezka) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream in = KonfiguracjaSymulacji.class.getResourceAsStream(sciezka)) {
            if (in == null) {
                throw new IllegalArgumentException("Nie znaleziono pliku YAML: " + sciezka);
            }
            return mapper.readValue(in, KonfiguracjaSymulacji.class);
        } catch (IOException e) {
            throw new RuntimeException("Błąd wczytywania konfiguracji", e);
        }
    }
}
