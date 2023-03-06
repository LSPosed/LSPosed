package android.os;

public interface IPowerManager extends IInterface {
    void reboot(boolean confirm, String reason, boolean wait) throws RemoteException;

    abstract class Stub extends Binder implements IPowerManager {

        public static IPowerManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
