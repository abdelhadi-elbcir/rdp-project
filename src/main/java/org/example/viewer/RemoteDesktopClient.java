package org.example.viewer;

import org.example.sender.RemoteDesktopInterface;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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

public class RemoteDesktopClient extends JFrame implements MouseListener, MouseMotionListener, KeyListener {
    private RemoteDesktopInterface remoteDesktop;
    private JPanel screenPanel;
    private JMenuBar menuBar;
    private TargetDataLine microphone;
    private boolean isStreamingAudio = false;

    public RemoteDesktopClient() {
        super("Remote Desktop Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        //if (gd.isFullScreenSupported()) {
        //    setUndecorated(true);
        //    gd.setFullScreenWindow(this);
        //} else {
        System.err.println("Full screen not supported");
        setSize(800, 600);
        setLocationRelativeTo(null);
        //}

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

        JMenu audioMenu = new JMenu("Audio");
        JMenuItem startAudioStreamItem = new JMenuItem("Start Audio Stream");
        JMenuItem stopAudioStreamItem = new JMenuItem("Stop Audio Stream");

        startAudioStreamItem.addActionListener(e -> startAudioStream());
        stopAudioStreamItem.addActionListener(e -> stopAudioStream());

        audioMenu.add(startAudioStreamItem);
        audioMenu.add(stopAudioStreamItem);
        menuBar.add(audioMenu);

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
                    //Registry registry = LocateRegistry.getRegistry("100.70.37.230", 1099);
                    Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
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

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        try {
            sendMouseEventWithScaling(e, true);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        try {
            sendMouseEventWithScaling(e, false);
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
            sendMouseEventWithScaling(e, false);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        try {
            sendMouseEventWithScaling(e, false);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    private void sendMouseEventWithScaling(MouseEvent e, boolean isPressed) throws RemoteException {
        boolean isFullScreen = (getExtendedState() == JFrame.MAXIMIZED_BOTH);

        int panelWidth = screenPanel.getWidth();
        int panelHeight = screenPanel.getHeight();

        int serverScreenWidth = remoteDesktop.getScreenWidth();
        int serverScreenHeight = remoteDesktop.getScreenHeight();

        double xScaleFactor = (double) serverScreenWidth / panelWidth;
        double yScaleFactor = (double) serverScreenHeight / panelHeight;

        Point panelPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), screenPanel);

        if (isFullScreen) {
            Insets insets = screenPanel.getInsets();
            int menuBarHeight = menuBar.getHeight();
            double yOffsetFactor = 24 + (panelPoint.y / (double) panelHeight) * 25;

            panelPoint.translate(-insets.left, -insets.top - menuBarHeight + (int) yOffsetFactor);
        }

        int scaledX = (int) (panelPoint.x * xScaleFactor);
        int scaledY = (int) (panelPoint.y * yScaleFactor);

        System.out.println("Panel Point: " + panelPoint);
        System.out.println("Scaled X: " + scaledX + ", Scaled Y: " + scaledY);
        System.out.println("Full Screen: " + isFullScreen);

        remoteDesktop.sendMouseEvent(scaledX, scaledY, e.getButton(), isPressed);
    }

    @Override
    public void keyTyped(KeyEvent e) {
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

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("All Files", "*.*");
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

    private void startAudioStream() {
        if (isStreamingAudio) {
            JOptionPane.showMessageDialog(this, "Audio stream is already running.", "Audio Stream", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            remoteDesktop.startAudioStream();
            isStreamingAudio = true;
            JOptionPane.showMessageDialog(this, "Audio streaming started.", "Audio Stream", JOptionPane.INFORMATION_MESSAGE);
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error starting audio stream: " + e.getMessage(), "Audio Stream Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopAudioStream() {
        if (!isStreamingAudio) {
            JOptionPane.showMessageDialog(this, "Audio stream is not running.", "Audio Stream", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            remoteDesktop.stopAudioStream();
            isStreamingAudio = false;
            JOptionPane.showMessageDialog(this, "Audio streaming stopped.", "Audio Stream", JOptionPane.INFORMATION_MESSAGE);
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error stopping audio stream: " + e.getMessage(), "Audio Stream Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class FileTransferThread extends Thread {
        private boolean sending;
        private String sourcePath;
        private String destinationPath;

        public FileTransferThread(boolean sending, String sourcePath, String destinationPath) {
            this.sending = sending;
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
        }

        @Override
        public void run() {
            try {
                if (sending) {
                    byte[] fileData = Files.readAllBytes(Paths.get(sourcePath));
                    remoteDesktop.sendFile(destinationPath, fileData);
                } else {
                    byte[] fileData = remoteDesktop.receiveFile(sourcePath);
                    Files.write(Paths.get(destinationPath), fileData);
                }
                JOptionPane.showMessageDialog(RemoteDesktopClient.this, "File transfer complete.", "File Transfer", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(RemoteDesktopClient.this, "File transfer failed: " + e.getMessage(), "File Transfer Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RemoteDesktopClient client = new RemoteDesktopClient();
            client.setVisible(true);
        });
    }
}
