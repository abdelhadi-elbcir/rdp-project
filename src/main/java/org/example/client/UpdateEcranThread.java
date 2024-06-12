package org.example.client;

import javax.swing.*;
import java.awt.*;

/**
 * Cette classe représente un thread en arrière-plan chargé de rafraîchir continuellement le panneau d'écran
 * de l'interface graphique (GUI) du client de bureau distant à un intervalle défini.
 */

public class UpdateEcranThread  extends Thread{
    private JPanel screenPanel;


    public UpdateEcranThread(JPanel screenPanel) {
        this.screenPanel = screenPanel;
    }

    @Override
    public void run() {
        while (true) {
            screenPanel.repaint();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
