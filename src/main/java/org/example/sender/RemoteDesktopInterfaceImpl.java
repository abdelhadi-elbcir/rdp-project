package org.example.sender;

import javax.sound.sampled.*;
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

public class RemoteDesktopInterfaceImpl implements RemoteDesktopInterface {
    private static RemoteDesktopInterfaceImpl instance;
    private String password;
    private boolean isConnected;
    private Robot robot;
    private TargetDataLine microphone;
    private ByteArrayOutputStream audioStream;

    private RemoteDesktopInterfaceImpl() throws RemoteException {
        super();
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        generatePassword();
        isConnected = false;
    }

    private void generatePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);

        //password = Base64.getEncoder().encodeToString(bytes);
        password = "1";

        System.out.println("Server password: " + password);
        JOptionPane.showMessageDialog(null, "Server password: " + password, "Password Generated", JOptionPane.INFORMATION_MESSAGE);
    }

    public static RemoteDesktopInterfaceImpl getInstance() throws RemoteException {
        if (instance == null) {
            instance = new RemoteDesktopInterfaceImpl();
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
    public int getScreenWidth() throws RemoteException {
        return Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    @Override
    public int getScreenHeight() throws RemoteException {
        return Toolkit.getDefaultToolkit().getScreenSize().height;
    }

    @Override
    public void sendKeyboardEvent(int keyCode, boolean isPressed) throws RemoteException {
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
    public void sendFile(String filePath, byte[] fileData) throws RemoteException {
        try {
            File file = new File(filePath);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fileData);
            fos.close();
            System.out.println("File received: " + filePath);
        } catch (IOException e) {
            throw new RemoteException("Error sending file: " + e.getMessage());
        }
    }

    @Override
    public byte[] receiveFile(String filePath) throws RemoteException {
        try {
            File file = new File(filePath);
            return Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            throw new RemoteException("Error receiving file: " + e.getMessage());
        }
    }

    @Override
    public byte[] startAudioStream() throws RemoteException {
        if (isConnected) {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    throw new LineUnavailableException("Line not supported");
                }
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                audioStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = microphone.read(buffer, 0, buffer.length)) != -1) {
                    audioStream.write(buffer, 0, bytesRead);
                }

                return audioStream.toByteArray();
            } catch (LineUnavailableException e) {
                throw new RemoteException("Error starting audio stream: " + e.getMessage());
            }
        } else {
            throw new RemoteException("Connection not established. Please provide the correct password.");
        }
    }

    @Override
    public void stopAudioStream() throws RemoteException {
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        if (audioStream != null) {
            try {
                audioStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
