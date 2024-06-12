package org.example.servicePartage;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * ServiceInterface définit les méthodes distantes qui peuvent être invoquées par un client
 * dans une application de partage d'écran et de contrôle à distance utilisant Java RMI.
 * Cette interface étend l'interface Remote, indiquant que ses méthodes
 * peuvent être appelées depuis une machine virtuelle non locale.
 */

public interface ServiceIntreface extends Remote {

    /**
     * Capture l'écran actuel et renvoie l'image sous forme de tableau d'octets.
     *
     * @return un tableau d'octets représentant l'image de l'écran capturé.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    byte[] capturerEcran() throws RemoteException;
    /**
     * Vérifie le code fourni pour établir la connexion.
     *
     * @param code :  le code fourni pour la vérification.
     * @return true si le code est correct, false sinon.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    boolean verifierCode(String code) throws RemoteException;
    /**
     * Envoie d'un événement de souris au système distant.
     *
     * @param x la coordonnée x de l'événement de souris.
     * @param y la coordonnée y de l'événement de souris.
     * @param button le bouton de la souris impliqué dans l'événement.
     * @param isPressed true si le bouton de la souris est pressé, false si relâché.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    void envoieEventSouris(int x, int y, int button, boolean isPressed) throws RemoteException;
    /**
     * Envoie un événement de clavier au système distant.
     *
     * @param keyCode le code de la touche impliquée dans l'événement.
     * @param isPressed true si la touche est pressée, false si relâchée.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    void envoieEventClavier(int keyCode, boolean isPressed) throws RemoteException;
    /**
     * Récupère la largeur de l'écran de la machine distante.
     *
     * @return la largeur de l'écran.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    int getLargeur() throws RemoteException;
    /**
     * Récupère la hauteur de l'écran de la machine distante .
     *
     * @return la hauteur de l'écran.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    int getHauteur() throws RemoteException;
    /**
     * Envoie un fichier au système distant.
     *
     * @param chemin le chemin du fichier à envoyer.
     * @param fileData les données du fichier sous forme de tableau d'octets.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    void envoieFichier(String chemin, byte[] fileData) throws RemoteException;
    /**
     * Reçoit un fichier depuis le système distant.
     *
     * @param filePath le chemin du fichier à recevoir.
     * @return les données du fichier sous forme de tableau d'octets.
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    byte[] receiveFile(String filePath) throws RemoteException;
    /**
     * Ouvre un gestionnaire de fichiers distant pour sélectionner un chemin de fichier.
     *
     * @return le chemin du fichier sélectionné .
     * @throws RemoteException si une erreur de communication distante se produit.
     */
    String ouvrirGestionnaireFichiers() throws RemoteException;


}