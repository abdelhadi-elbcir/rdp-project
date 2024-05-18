package org.example.sender;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Sender {
    public static void main(String[] args) {
        try {
            RemoteDesktopInterfaceImpl server = RemoteDesktopInterfaceImpl.getInstance(); // Récupère l'instance du serveur
            RemoteDesktopInterface stub = (RemoteDesktopInterface) UnicastRemoteObject.exportObject(server, 0); // Exporte l'objet distant

            Registry registry = LocateRegistry.createRegistry(1099); // Crée le registre RMI
            registry.rebind("irisi", stub); // Lie l'interface distante au registre

            System.out.println("Remote Desktop Server is running..."); // Message de confirmation du démarrage
        } catch (Exception e) {
            e.printStackTrace(); // Affiche les erreurs en cas d'échec du démarrage du serveur
        }
    }

}
