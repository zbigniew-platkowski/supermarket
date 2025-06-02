package supermarket;

import supermarket.config.KonfiguracjaSymulacji;
import supermarket.gui.SupermarketGUI;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KonfiguracjaSymulacji cfg = KonfiguracjaSymulacji.load("/config.yaml");
            new SupermarketGUI(cfg).setVisible(true);
        });
    }
}
