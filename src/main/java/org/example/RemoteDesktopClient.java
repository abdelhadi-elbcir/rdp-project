package org.example;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class RemoteDesktopClient extends JFrame implements MouseListener, MouseMotionListener {
    private RemoteDesktopInterface remoteDesktop; // Interface pour la communication avec le bureau distant
    private JPanel screenPanel; // Panel pour afficher l'écran distant

    public RemoteDesktopClient() {
        super("Remote Desktop Client"); // Titre de la fenêtre
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Fermeture de l'application à la fermeture de la fenêtre
        setSize(800, 600); // Taille de la fenêtre
        setLocationRelativeTo(null); // Centrer la fenêtre

        // Initialisation du panel pour afficher l'écran
        screenPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Capture de l'écran distant et affichage
                if (remoteDesktop != null) {
                    try {
                        byte[] screenData = remoteDesktop.captureScreen(); // Capture de l'écran distant
                        ByteArrayInputStream bais = new ByteArrayInputStream(screenData);
                        BufferedImage screenImage = ImageIO.read(bais);
                        Image scaledImage = screenImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                        g.drawImage(scaledImage, 0, 0, null); // Affichage de l'image capturée
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(RemoteDesktopClient.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        add(screenPanel, BorderLayout.CENTER); // Ajout du panel à la fenêtre
        screenPanel.addMouseListener(this); // Écouteur de clics de souris
        screenPanel.addMouseMotionListener(this); // Écouteur de mouvements de souris

        boolean connected = false;
        // Boucle pour se connecter au serveur
        while (!connected) {
            String password = JOptionPane.showInputDialog(this, "Enter server password:"); // Demande du mot de passe
            if (password == null) {
                System.out.println("Password is required to connect to the server.");
                System.exit(0);
            } else {
                try {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099); // Récupération du registre RMI
                    remoteDesktop = (RemoteDesktopInterface) registry.lookup("irisi"); // Recherche de l'interface distante
                    connected = remoteDesktop.setPassword(password); // Validation du mot de passe
                    if (!connected) {
                        JOptionPane.showMessageDialog(this, "Invalid password. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (RemoteException | NotBoundException e) {
                    e.printStackTrace();
                }
            }
        }

        // Thread pour mettre à jour l'écran à intervalle régulier
        new Thread(() -> {
            while (true) {
                screenPanel.repaint(); // Redessiner l'écran
                try {
                    Thread.sleep(50); // Attendre avant de redessiner
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Méthodes de l'interface MouseListener

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        try {
            remoteDesktop.sendMouseEvent(e.getX(), e.getY(), e.getButton(), true); // Envoi de l'événement de clic de souris au serveur distant
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        try {
            remoteDesktop.sendMouseEvent(e.getX(), e.getY(), e.getButton(), false); // Envoi de l'événement de relâchement de souris au serveur distant
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // Méthodes de l'interface MouseMotionListener

    @Override
    public void mouseDragged(MouseEvent e) {
        try {
            remoteDesktop.sendMouseEvent(e.getX(), e.getY(), MouseEvent.NOBUTTON, false); // Envoi de l'événement de déplacement de souris au serveur distant
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        try {
            remoteDesktop.sendMouseEvent(e.getX(), e.getY(), MouseEvent.NOBUTTON, false); // Envoi de l'événement de mouvement de souris au serveur distant
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopClient().setVisible(true)); // Création de l'interface utilisateur dans le thread de l'interface Swing
    }
}