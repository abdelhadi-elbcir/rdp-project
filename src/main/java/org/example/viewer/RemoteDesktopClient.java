package org.example.viewer;

import org.example.sender.RemoteDesktopInterface;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;

public class RemoteDesktopClient extends JFrame implements MouseListener, MouseMotionListener, KeyListener {
        private RemoteDesktopInterface remoteDesktop; // Interface pour la communication avec le bureau distant
    private JPanel screenPanel; // Panel pour afficher l'écran distant

    public RemoteDesktopClient() {
        super("Remote Desktop Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

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
        add(screenPanel, BorderLayout.CENTER);

        // Add buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());

        JButton playAudioButton = new JButton("Play Audio");
        playAudioButton.addActionListener(e -> playAudio());
        buttonsPanel.add(playAudioButton);

        JButton shareFileButton = new JButton("Share File");
        shareFileButton.addActionListener(e -> shareFile());
        buttonsPanel.add(shareFileButton);

        JButton editContentButton = new JButton("Edit Content");
        editContentButton.addActionListener(e -> editContent());
        buttonsPanel.add(editContentButton);

        add(buttonsPanel, BorderLayout.SOUTH); // Add buttons panel to the bottom

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

    // Méthode pour envoyer les événements de souris avec mise à l'échelle
    private void sendMouseEventWithScaling(MouseEvent e, boolean isPressed) throws RemoteException {
        int scaledX = scaleX(e.getX());
        int scaledY = scaleY(e.getY());
        remoteDesktop.sendMouseEvent(scaledX, scaledY, e.getButton(), isPressed);
    }

    // Méthodes de mise à l'échelle
    private int scaleX(int x) throws RemoteException {
        // Calcule la mise à l'échelle en fonction de la résolution de l'écran du serveur
        int serverScreenWidth = remoteDesktop.getScreenWidth();
        int clientScreenWidth = getClientScreenWidth();
        return x * serverScreenWidth / clientScreenWidth;
    }

    private int scaleY(int y) throws RemoteException {
        // Calcule la mise à l'échelle en fonction de la résolution de l'écran du serveur
        int serverScreenHeight = remoteDesktop.getScreenHeight();
        int clientScreenHeight = getClientScreenHeight();
        return y * serverScreenHeight / clientScreenHeight;
    }

    private int getClientScreenWidth() {
        return Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    private int getClientScreenHeight() {
        return Toolkit.getDefaultToolkit().getScreenSize().height;
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

    private void playAudio() {
        try {
            byte[] audioData = remoteDesktop.getAudio();
            if (audioData != null) {
                AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();

                sourceDataLine.write(audioData, 0, audioData.length);

                sourceDataLine.drain();
                sourceDataLine.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void shareFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                byte[] fileContent = remoteDesktop.getFileContent(selectedFile.getAbsolutePath());
                if (fileContent != null) {
                    JFileChooser saveFileChooser = new JFileChooser();
                    result = saveFileChooser.showSaveDialog(this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File saveFile = saveFileChooser.getSelectedFile();
                        FileOutputStream fos = new FileOutputStream(saveFile);
                        fos.write(fileContent);
                        fos.close();
                        JOptionPane.showMessageDialog(this, "File shared successfully!");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to get file content from server.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void editContent() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                byte[] fileContent = remoteDesktop.getFileContent(selectedFile.getAbsolutePath());
                if (fileContent != null) {
                    // Open file content in an editor or perform desired operations
                    String content = new String(fileContent);
                    JTextArea textArea = new JTextArea(content);
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    JOptionPane.showMessageDialog(this, scrollPane, "Edit Content", JOptionPane.PLAIN_MESSAGE);
                    String editedContent = textArea.getText();
                    if (!editedContent.equals(content)) {
                        remoteDesktop.setFileContent(selectedFile.getAbsolutePath(), editedContent.getBytes());
                        JOptionPane.showMessageDialog(this, "File content updated successfully!");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to get file content from server.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }




}