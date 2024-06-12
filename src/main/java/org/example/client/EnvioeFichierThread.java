package org.example.client;

import org.example.servicePartage.ServiceIntreface;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Classe représentant un thread utilisé pour envoyer ou recevoir un fichier via le service partagé.
 * Ce thread est utilisé pour effectuer les opérations de transfert de fichier de manière asynchrone,
 * afin de ne pas bloquer l'interface utilisateur pendant le transfert.
 */
public class EnvioeFichierThread extends Thread {
    private boolean envoie;
    private String source;
    private String destination;
    private ServiceIntreface servicePartage;
    private Client remoteDesktopClient;

    /**
     * Constructeur de la classe EnvioeFichierThread.
     *
     * @param envoie                   Le mode de transfert : true pour l'envoi, false pour la réception.
     * @param source                 Le chemin du fichier source.
     * @param destination            Le chemin de destination du fichier.
     * @param servicePartage         Le service partagé utilisé pour le transfert.
     * @param remoteDesktopClient    Référence vers l'instance de RemoteDesktopClient pour afficher les messages.
     */
    public EnvioeFichierThread(boolean envoie, String source, String destination, ServiceIntreface servicePartage, Client remoteDesktopClient) {
        this.envoie = envoie;
        this.source = source;
        this.destination = destination;
        this.servicePartage = servicePartage;
        this.remoteDesktopClient = remoteDesktopClient;
    }

    /**
     * Méthode exécutée par le thread pour effectuer le transfert de fichier.
     */
    @Override
    public void run() {
        try {
            if (envoie) {
                byte[] fileData = Files.readAllBytes(Paths.get(source));
                String chemin = destination;
                servicePartage.envoieFichier(chemin, fileData);
                JOptionPane.showMessageDialog(remoteDesktopClient, "Le fichier a été envoyé.", "Transfert fichier", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String cheminDistant = servicePartage.ouvrirGestionnaireFichiers();
                if (cheminDistant != null && !cheminDistant.isEmpty()) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int result = fileChooser.showSaveDialog(remoteDesktopClient);

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File fichierLocal = fileChooser.getSelectedFile();
                        String cheminLocal = fichierLocal.getAbsolutePath();
                        byte[] fileData = servicePartage.receiveFile(cheminDistant);
                        Files.write(Paths.get(cheminLocal), fileData);
                        JOptionPane.showMessageDialog(remoteDesktopClient, "Le fichier a été reçu.", "Transfert fichier", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(remoteDesktopClient, "Une erreur s'est produite : " + e.getMessage(), "Transfert fichier", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
