package org.example.viewer;

import org.example.sender.RemoteDesktopInterface;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.imageio.ImageIO;
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

    private void sendMouseEventWithScaling(MouseEvent e, boolean isPressed) throws RemoteException {
        // Get the screen dimensions of the client
        Dimension clientScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int clientScreenWidth = clientScreenSize.width;
        int clientScreenHeight = clientScreenSize.height;

        // Get the screen dimensions of the server
        int serverScreenWidth = remoteDesktop.getScreenWidth();
        int serverScreenHeight = remoteDesktop.getScreenHeight();

        // Calculate the scaling factors
        double xScaleFactor = (double) serverScreenWidth / clientScreenWidth;
        double yScaleFactor = (double) serverScreenHeight / clientScreenHeight;

        // Adjust the point based on insets and component sizes
        Point dragPoint = e.getPoint();
        Insets insets = screenPanel.getInsets();
        int topBarHeight = getRootPane().getHeight() - screenPanel.getHeight();
        dragPoint.translate(-insets.left, -insets.top - topBarHeight );

        // Scale the coordinates
        int scaledX = (int) (dragPoint.x * xScaleFactor);
        int scaledY = (int) (dragPoint.y * yScaleFactor);

        // Send the scaled coordinates and button state to the remote desktop
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
}
