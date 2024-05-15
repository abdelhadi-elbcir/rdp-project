package org.example;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.SecureRandom;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopServer implements RemoteDesktopInterface {
    private static RemoteDesktopServer instance; // Instance unique du serveur
    private String password; // Mot de passe du serveur
    private boolean isConnected; // Indique si le client est connecté
    private Robot robot;

    // Constructeur privé pour empêcher l'instanciation directe
    private RemoteDesktopServer() throws RemoteException {
        super(); // Appel au constructeur de la classe mère
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        generatePassword(); // Génération du mot de passe au démarrage du serveur
        isConnected = false; // Initialisation de la connexion à false
    }

    // Méthode privée pour générer un mot de passe aléatoire
    private void generatePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        password = Base64.getEncoder().encodeToString(bytes);
        System.out.println("Server password: " + password);
        JOptionPane.showMessageDialog(null, "Server password: " + password, "Password Generated", JOptionPane.INFORMATION_MESSAGE);
    }

    // Méthode statique pour récupérer l'instance unique du serveur
    public static RemoteDesktopServer getInstance() throws RemoteException {
        if (instance == null) {
            instance = new RemoteDesktopServer();
        }
        return instance;
    }

    @Override
    public byte[] captureScreen() throws RemoteException {
        if (isConnected) { // Vérifie si le client est connecté
            try {
                Robot robot = new Robot();
                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage screenCapture = robot.createScreenCapture(screenRect);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(screenCapture, "png", baos);
                return baos.toByteArray(); // Retourne l'image capturée sous forme de tableau d'octets
            } catch (IOException | AWTException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            throw new RemoteException("Connection not established. Please provide the correct password.");
        }
    }

    @Override
    public boolean setPassword(String providedPassword) throws RemoteException {
        if (providedPassword.equals(password)) { // Vérifie si le mot de passe fourni est correct
            isConnected = true; // Établit la connexion
            return true;
        } else {
            return false; // Mot de passe incorrect
        }
    }


    @Override
    public void sendMouseEvent(int x, int y, int button, boolean isPressed) throws RemoteException {
        robot.mouseMove(x, y);
        if (button != MouseEvent.NOBUTTON) {
            int mask = InputEvent.getMaskForButton(button);
            if (isPressed) {
                robot.mousePress(mask);
            } else {
                robot.mouseRelease(mask);
            }
        }
    }

    @Override
    public void sendKeyboardEvent(int keyCode, boolean isPressed) throws RemoteException {
    }

    @Override
    public void sendAudioData(byte[] audioData) throws RemoteException {
    }

    // Méthode principale pour démarrer le serveur
    public static void main(String[] args) {
        try {
            RemoteDesktopServer server = RemoteDesktopServer.getInstance(); // Récupère l'instance du serveur
            RemoteDesktopInterface stub = (RemoteDesktopInterface) UnicastRemoteObject.exportObject(server, 0); // Exporte l'objet distant

            Registry registry = LocateRegistry.createRegistry(1099); // Crée le registre RMI
            registry.rebind("irisi", stub); // Lie l'interface distante au registre

            System.out.println("Remote Desktop Server is running..."); // Message de confirmation du démarrage
        } catch (Exception e) {
            e.printStackTrace(); // Affiche les erreurs en cas d'échec du démarrage du serveur
        }
    }
}