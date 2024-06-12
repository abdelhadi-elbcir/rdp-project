package org.example.client;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.servicePartage.ServiceIntreface;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Cette classe représente le client du bureau distant. Il fournit une interface graphique permettant à l'utilisateur
 * de contrôler et de visualiser l'écran distant, ainsi que d'envoyer et de recevoir des fichiers.
 */

public class Client extends JFrame implements MouseListener, MouseMotionListener, KeyListener {
    private ServiceIntreface servicePartage;
    private JPanel ecran;
    private JMenuBar menu;
    Dotenv dotenv = Dotenv.configure()
                          .directory("./src/main/assets/.env")
                          .load();

    public Client() {
        super("Bureau client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Configuration de l'affichage en plein écran si possible
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            setUndecorated(true);
            gd.setFullScreenWindow(this);
        } else {
            System.err.println("le mode plien ecran n'est pas supporte");
            setSize(800, 600);
            setLocationRelativeTo(null);
        }

        // Initialisation de la fenêtre de l'écran et du menu
        initUI();

        // Connexion au service distant et initialisation du mise a jour  de l'écran
        connectToServer();
        new UpdateEcranThread(ecran).start();

    }
    /**
     * Initialise l'interface utilisateur de la fenêtre principale, y compris l'écran et le menu.
     */
    private void initUI() {
        // Configuration de l'écran
        ecran = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (servicePartage != null) {
                    try {
                        byte[] screenData = servicePartage.capturerEcran();
                        ByteArrayInputStream bais = new ByteArrayInputStream(screenData);
                        BufferedImage screenImage = ImageIO.read(bais);
                        Image scaledImage = screenImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                        g.drawImage(scaledImage, 0, 0, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(Client.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };



        // Configuration du menu
        menu = new JMenuBar();
        JMenu menuFichier = new JMenu("Menu");
        JMenuItem envoie = new JMenuItem("Envoie");
        JMenuItem reception = new JMenuItem("Reception");
        JMenuItem fermer = new JMenuItem("Fermer");

        envoie.addActionListener(e -> sendFile());
        reception.addActionListener(e -> {
            try {
                receiveFile();
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        });
        fermer.addActionListener(e->{
            dispose();
        });

        menuFichier.add(envoie);
        menuFichier.add(reception);
        menuFichier.add(fermer);
        menu.add(menuFichier);
        setJMenuBar(menu);

        // Ajout de l'écran à la fenêtre et configuration des écouteurs d'événements
        add(ecran, BorderLayout.CENTER);
        ecran.addMouseListener(this);
        ecran.addMouseMotionListener(this);
        ecran.addKeyListener(this);
        ecran.setFocusable(true);
    }

    /**
     * Établit une connexion avec le serveur distant en utilisant RMI.
     */
    private void connectToServer() {
        boolean connected = false;
        while (!connected) {
            String password = JOptionPane.showInputDialog(this, "Saisissez le code du bureau distant:");
            if (password == null) {
                System.out.println("Le code du bureau distant est obligatoire.");
                System.exit(0);
            } else {
                try {
                    Registry registry = LocateRegistry.getRegistry("192.168.1.117",1099);
                    servicePartage = (ServiceIntreface) registry.lookup("irisi");
                    connected = servicePartage.verifierCode(password);
                    if (!connected) {
                        JOptionPane.showMessageDialog(this, "Le code est invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (RemoteException | NotBoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     Les méthodes suivantes gèrent les événements de la souris sur l'interface utilisateur et envoient les informations correspondantes à un service
     distant pour les répliquer
    */
     @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        try {
            miseEchelle(e, true);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        try {
            miseEchelle(e, false);
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

    @Override
    public void mouseDragged(MouseEvent e) {
        try {
            miseEchelle(e, false);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        try {
            miseEchelle(e, false);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }


    /*
     Ces méthodes gèrent les événements du clavier sur l'interface utilisateur et envoient les informations correspondantes à un service distant pour les répliquer
     */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            servicePartage.envoieEventClavier(e.getKeyCode(), true);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        try {
            servicePartage.envoieEventClavier(e.getKeyCode(), false);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Envoie un événement de souris au service distant en mettant à l'échelle les coordonnées pour correspondre à la résolution de l'écran du serveur.
     * Cette méthode ajuste les coordonnées en fonction des différences de résolution entre l'écran du contrôleur et celui du serveur distant,
     * ainsi que des bordures de l'écran et de la hauteur du menu, puis envoie l'événement de souris avec les coordonnées mises à l'échelle.
     *
     * @param e         L'événement de la souris à traiter.
     * @param isPressed Indique si le bouton de la souris est pressé ou relâché.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    private void miseEchelle(MouseEvent e, boolean isPressed) throws RemoteException {
        // Obtient la taille de l'écran du contrôleur (client)
        Dimension tailleEcranControleur = Toolkit.getDefaultToolkit().getScreenSize();
        int ecranControleurLargeur = tailleEcranControleur.width;
        int ecranControleurHauteur = tailleEcranControleur.height;

        // Obtient la largeur et la hauteur de l'écran du controle (serveur distant)
        int ecranControleLargeur = servicePartage.getLargeur();
        int ecranControleHauteur = servicePartage.getHauteur();

        // Calcule les facteurs d'échelle pour les coordonnées X et Y
        double xScaleFactor = (double) ecranControleLargeur / ecranControleurLargeur;
        double yScaleFactor = (double) ecranControleHauteur / ecranControleurHauteur;

        // Obtient le point de déplacement de la souris
        Point dragPoint = e.getPoint();
        // Obtient les bordures de l'écran
        Insets insets = ecran.getInsets();
        int menuHeight = menu.getHeight();

        // Calcule le facteur de décalage Y pour ajuster la hauteur de l'écran et du menu
        double yOffsetFactor = 24 + (dragPoint.y / (double) ecranControleurHauteur) * 25;

        // Ajuste les coordonnées du point de déplacement en tenant compte des bordures et du menu
        dragPoint.translate(-insets.left, -insets.top - menuHeight + (int) yOffsetFactor);

        // Calcule les coordonnées X et Y mises à l'échelle
        int scaledX = (int) (dragPoint.x * xScaleFactor);
        int scaledY = (int) (dragPoint.y * yScaleFactor);

        servicePartage.envoieEventSouris(scaledX, scaledY, e.getButton(), isPressed);
    }

    /**
     * Ouvre une boîte de dialogue de sélection de fichier, permettant à l'utilisateur de choisir un fichier à envoyer.
     * Une fois le fichier sélectionné, un nouveau thread de transfert de fichier est démarré pour envoyer le fichier au destinataire.
     */
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Tous les fichiers", ".");
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String source = selectedFile.getAbsolutePath();
            String destination = selectedFile.getName();
            new EnvioeFichierThread(true, source, destination,servicePartage,this).start();
        }
    }

    private void receiveFile() throws RemoteException {
        new EnvioeFichierThread(false, null, null, servicePartage, this).start();

    }

}
