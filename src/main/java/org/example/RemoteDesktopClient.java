package org.example;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class RemoteDesktopClient extends JFrame {
    private RemoteDesktopInterface remoteDesktop;
    private JPanel screenPanel;

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
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        add(screenPanel, BorderLayout.CENTER);

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            remoteDesktop = (RemoteDesktopInterface) registry.lookup("irisi");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopClient().setVisible(true));
    }
}
