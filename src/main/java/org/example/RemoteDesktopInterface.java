package org.example;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteDesktopInterface extends Remote {
    byte[] captureScreen() throws RemoteException;
}