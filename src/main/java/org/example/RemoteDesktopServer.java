package org.example;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.imageio.ImageIO;

public class RemoteDesktopServer implements RemoteDesktopInterface {
    private static RemoteDesktopServer instance;

    private RemoteDesktopServer() throws RemoteException {
        super();
    }

    public static RemoteDesktopServer getInstance() throws RemoteException {
        if (instance == null) {
            instance = new RemoteDesktopServer();
        }
        return instance;
    }

    @Override
    public byte[] captureScreen() throws RemoteException {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenCapture, "png", baos);
            return baos.toByteArray();
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            RemoteDesktopServer server = RemoteDesktopServer.getInstance();
            RemoteDesktopInterface stub = (RemoteDesktopInterface) UnicastRemoteObject.exportObject(server, 0);

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("irisi", stub);

            System.out.println("Remote Desktop Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}