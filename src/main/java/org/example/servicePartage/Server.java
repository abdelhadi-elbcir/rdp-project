package org.example.servicePartage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import io.github.cdimascio.dotenv.Dotenv;


/**
 * La classe Controle sert de point d'entrée pour démarrer le serveur RMI de partage d'écran et de contrôle à distance.
 * Elle crée et enregistre l'objet distant dans le registre RMI, rendant les méthodes du serveur accessibles aux clients distants.
 */

public class Server {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                              .directory("./src/main/assets/.env")
                              .load();
        try {
            ServiceInterfaceImpl server = ServiceInterfaceImpl.getInstance();
            // Exportation de l'objet serveur pour qu'il puisse recevoir des appels RMI
            ServiceIntreface stub = (ServiceIntreface) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(dotenv.get("port")));
            // Liaison du stub de l'objet serveur dans le registre
            registry.rebind(dotenv.get("stub"), stub);
            System.out.println("Le serveur de bureau a distance est en cours d'execution...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
