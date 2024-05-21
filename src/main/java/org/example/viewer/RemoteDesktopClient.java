package org.example.viewer;

import org.example.sender.RemoteDesktopInterface;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class RemoteDesktopClient extends JFrame implements MouseListener, MouseMotionListener, KeyListener {
    private RemoteDesktopInterface remoteDesktop; // Interface pour la communication avec le bureau distant
    private JPanel screenPanel; // Panel pour afficher l'écran distant
    JMenuBar menuBar;

    public RemoteDesktopClient() {
        super("Remote Desktop Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set the frame to full screen
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            setUndecorated(true); // Remove title bar and borders
            gd.setFullScreenWindow(this); // Set this frame to full screen
        } else {
            System.err.println("Full screen not supported");
            setSize(800, 600);
            setLocationRelativeTo(null);
        }

        screenPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (remoteDesktop != null) {
                    try {
                        byte[] screenData = remoteDesktop.captureScreen();
                        ByteArrayInputStream bais = new ByteArrayInputStream(screenData);
                        BufferedImage screenImage = ImageIO.read(bais);
                        Image scaledImage = screenImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                        g.drawImage(scaledImage, 0, 0, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(RemoteDesktopClient.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };


        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem sendFileItem = new JMenuItem("Send File");
        JMenuItem receiveFileItem = new JMenuItem("Receive File");

        sendFileItem.addActionListener(e -> sendFile());
        receiveFileItem.addActionListener(e -> receiveFile());

        fileMenu.add(sendFileItem);
        fileMenu.add(receiveFileItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);


        add(screenPanel, BorderLayout.CENTER);
        screenPanel.addMouseListener(this);
        screenPanel.addMouseMotionListener(this);
        screenPanel.addKeyListener(this);
        screenPanel.setFocusable(true);

        boolean connected = false;
        while (!connected) {
            String password = JOptionPane.showInputDialog(this, "Enter server password:");
            if (password == null) {
                System.out.println("Password is required to connect to the server.");
                System.exit(0);
            } else {
                try {

                    Registry registry = LocateRegistry.getRegistry("100.70.33.100", 1099);


                    remoteDesktop = (RemoteDesktopInterface) registry.lookup("irisi");
                    connected = remoteDesktop.setPassword(password);
                    if (!connected) {
                        JOptionPane.showMessageDialog(this, "Invalid password. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (RemoteException | NotBoundException e) {
                    e.printStackTrace();
                }
            }
        }

        new Thread(() -> {
            while (true) {
                screenPanel.repaint();
                try {
                    Thread.sleep(50);
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
            sendMouseEventWithScaling(e, true); // Envoi de l'événement de clic de souris avec mise à l'échelle
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        try {
            sendMouseEventWithScaling(e, false); // Envoi de l'événement de relâchement de souris avec mise à l'échelle
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
            sendMouseEventWithScaling(e, false); // Envoi de l'événement de déplacement de souris avec mise à l'échelle
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        try {
            sendMouseEventWithScaling(e, false); // Envoi de l'événement de mouvement de souris avec mise à l'échelle
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    private void sendMouseEventWithScaling(MouseEvent e, boolean isPressed) throws RemoteException {
        Dimension clientScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int clientScreenWidth = clientScreenSize.width;
        int clientScreenHeight = clientScreenSize.height;

        int serverScreenWidth = remoteDesktop.getScreenWidth();
        int serverScreenHeight = remoteDesktop.getScreenHeight();

        double xScaleFactor = (double) serverScreenWidth / clientScreenWidth;
        double yScaleFactor = (double) serverScreenHeight / clientScreenHeight;

        Point dragPoint = e.getPoint();
        Insets insets = screenPanel.getInsets();
        int menuBarHeight = menuBar.getHeight();
        int taskBarHeight = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration()).bottom;

        // Calculate a dynamic adjustment factor for Y
        double yOffsetFactor = 24 + (dragPoint.y / (double) clientScreenHeight) * 25;  // Example scaling factor

        dragPoint.translate(-insets.left, -insets.top - menuBarHeight + (int) yOffsetFactor);

        System.out.println("Original Point: " + dragPoint);
        System.out.println("Insets: " + insets);
        System.out.println("Menu Bar Height: " + menuBarHeight);
        System.out.println("Task Bar Height: " + taskBarHeight);

        int scaledX = (int) (dragPoint.x * xScaleFactor);
        int scaledY = (int) (dragPoint.y * yScaleFactor);
        System.out.println("Scaled X: " + scaledX + ", Scaled Y: " + scaledY);

        remoteDesktop.sendMouseEvent(scaledX, scaledY, e.getButton(), isPressed);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not implemented
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            remoteDesktop.sendKeyboardEvent(e.getKeyCode(), true);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        try {
            remoteDesktop.sendKeyboardEvent(e.getKeyCode(), false);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopClient().setVisible(true));
    }
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("All Files", ".");
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String sourceFilePath = selectedFile.getAbsolutePath();
            String destinationFileName = selectedFile.getName();
            new FileTransferThread(true, sourceFilePath, destinationFileName).start();
        }
    }

    private void receiveFile() {
        String remoteFilePath = JOptionPane.showInputDialog(this, "Enter the remote file path:");
        if (remoteFilePath != null && !remoteFilePath.isEmpty()) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File localFile = fileChooser.getSelectedFile();
                String localFilePath = localFile.getAbsolutePath();
                new FileTransferThread(false, remoteFilePath, localFilePath).start();
            }
        }
    }

    private class FileTransferThread extends Thread {
        private boolean sendMode;
        private String sourceFilePath;
        private String destinationFilePath;

        public FileTransferThread(boolean sendMode, String sourceFilePath, String destinationFilePath) {
            this.sendMode = sendMode;
            this.sourceFilePath = sourceFilePath;
            this.destinationFilePath = destinationFilePath;
        }

        @Override
        public void run() {
            try {
                if (sendMode) {
                    byte[] fileData = Files.readAllBytes(Paths.get(sourceFilePath));
                    String destinationPath = destinationFilePath; // Specify the relative path on the server
                    remoteDesktop.sendFile(destinationPath, fileData);
                } else {
                    byte[] fileData = remoteDesktop.receiveFile(sourceFilePath);
                    Files.write(Paths.get(destinationFilePath), fileData);
                }
                JOptionPane.showMessageDialog(RemoteDesktopClient.this, "File transfer completed successfully.", "File Transfer", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(RemoteDesktopClient.this, "Error during file transfer: " + e.getMessage(), "File Transfer Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }}
}
