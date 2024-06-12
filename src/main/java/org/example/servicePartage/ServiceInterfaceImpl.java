package org.example.servicePartage;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * La classe ServiceInterfaceImpl implémente l'interface ServiceInterface pour fournir les fonctionnalités
 * de partage d'écran et de contrôle à distance. Elle utilise le modèle Singleton pour s'assurer
 * qu'il n'y a qu'une seule instance de cette classe dans l'application.
 * Cette classe utilise la classe java.awt.Robot pour simuler les événements du clavier et de la souris
 * et pour capturer l'écran. Elle gère également la génération et la vérification d'un code de partage unique
 * pour sécuriser les connexions distantes.
 */

public class ServiceInterfaceImpl implements ServiceIntreface {


    private static ServiceInterfaceImpl instance;
    private String codePartage;
    private boolean connecte;
    private Robot robot;

    /**
     * Le constructeur est défini comme étant privé pour éviter toute instanciation externe et directe.
     * Il initialise également l'instance de Robot et génère un code de partage unique.
     *
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    private ServiceInterfaceImpl() throws RemoteException {
        super();
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        genererCodePartage();
        connecte = false;
    }

    /**
    * Cette methode permet de generer un code de partage unique a chaque machine pour que les machine distantes puissent y acceder
     */
    private void genererCodePartage() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        codePartage = Base64.getEncoder().encodeToString(bytes);
        System.out.println(codePartage);
        JOptionPane.showMessageDialog(null, "Votre code est : " + codePartage, "Code de partage", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Cette méthode permet de récupérer l'instance unique du serveur.
     *
     * @return l'instance unique de ServiceInterfaceImpl.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    public static ServiceInterfaceImpl getInstance() throws RemoteException {
        if (instance == null) {
            instance = new ServiceInterfaceImpl();
        }
        return instance;
    }

    @Override
    public byte[] capturerEcran() throws RemoteException {
        if (connecte) {
            try {
                Robot robot = new Robot();
                Rectangle ecran = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage captureEcran = robot.createScreenCapture(ecran);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(captureEcran, "png", baos);
                return baos.toByteArray();
            } catch (IOException | AWTException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            throw new RemoteException("La connexion a echoue");
        }
    }

    @Override
    public boolean verifierCode(String code) throws RemoteException {
        if (code.equals(codePartage)) {
            connecte = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void envoieEventSouris(int x, int y, int button, boolean isPressed) throws RemoteException {
        // Déplace le curseur de la souris aux coordonnées (x, y)
        robot.mouseMove(x, y);
        // Vérifie si un bouton de la souris a été spécifié
        if (button != MouseEvent.NOBUTTON) {
            // Obtient le masque d'événement correspondant au bouton de la souris
            int mask = InputEvent.getMaskForButton(button);
            if (isPressed) {
                robot.mousePress(mask);
            } else {
                robot.mouseRelease(mask);
            }
        }
    }

    @Override
    public int getLargeur() throws RemoteException {
        return Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    @Override
    public int getHauteur() throws RemoteException {
        return Toolkit.getDefaultToolkit().getScreenSize().height;
    }

    @Override
    public void envoieEventClavier(int keyCode, boolean isPressed) throws RemoteException {
        try {
            if (isPressed) {
                robot.keyPress(keyCode);
            } else {
                robot.keyRelease(keyCode);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void envoieFichier(String chemin, byte[] fileData) throws RemoteException {
        try {
            File file = new File(chemin);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fileData);
            fos.close();
        } catch (IOException e) {
            throw new RemoteException("Erreur lors de l'envoie: " + e.getMessage());
        }
    }

    @Override
    public byte[] receiveFile(String filePath) throws RemoteException {
        try {
            File file = new File(filePath);
            return Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            throw new RemoteException("Une erreur s'est produit: " + e.getMessage());
        }
    }

    @Override
    public String ouvrirGestionnaireFichiers() throws RemoteException {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Tous les fichiers", ".");
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }


}