package org.example.viewer;

import javax.swing.*;

public class Viewer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopClient().setVisible(true)); // Création de l'interface utilisateur dans le thread de l'interface Swing
    }

}
