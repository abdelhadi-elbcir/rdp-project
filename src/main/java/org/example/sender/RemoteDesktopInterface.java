package org.example.sender;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteDesktopInterface extends Remote {
    byte[] captureScreen() throws RemoteException;
    boolean setPassword(String providedPassword) throws RemoteException;
    void sendMouseEvent(int x, int y, int button, boolean isPressed) throws RemoteException;
    void sendKeyboardEvent(int keyCode, boolean isPressed) throws RemoteException;
    int getScreenWidth() throws RemoteException;
    int getScreenHeight() throws RemoteException;
}