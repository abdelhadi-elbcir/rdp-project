package org.example;

import java.awt.*;
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
    private static RemoteDesktopServer instance;
    private String password;
    private boolean isConnected;

    private RemoteDesktopServer() throws RemoteException {
        super();
        generatePassword();
        isConnected = false;
    }

    private void generatePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        password = Base64.getEncoder().encodeToString(bytes);
        System.out.println("Server password: " + password);
        JOptionPane.showMessageDialog(null, "Server password: " + password, "Password Generated", JOptionPane.INFORMATION_MESSAGE);
    }

    public static RemoteDesktopServer getInstance() throws RemoteException {
        if (instance == null) {
            instance = new RemoteDesktopServer();
        }
        return instance;
    }

    @Override
    public byte[] captureScreen() throws RemoteException {
        if (isConnected) {
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
        } else {
            throw new RemoteException("Connection not established. Please provide the correct password.");
        }
    }

    @Override
    public boolean setPassword(String providedPassword) throws RemoteException {
        if (providedPassword.equals(password)) {
            isConnected = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void sendMouseEvent(int x, int y, int button, boolean isPressed) throws RemoteException {

    }

    @Override
    public void sendKeyboardEvent(int keyCode, boolean isPressed) throws RemoteException {

    }

    @Override
    public void sendAudioData(byte[] audioData) throws RemoteException {

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