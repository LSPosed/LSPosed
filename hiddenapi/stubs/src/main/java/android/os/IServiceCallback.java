package android.os;
public interface IServiceCallback extends IInterface
{
    public static abstract class Stub extends android.os.Binder implements android.os.IServiceCallback {
    }
    /**
     * Called when a service is registered.
     *
     * @param name the service name that has been registered with
     * @param binder the binder that is registered
     */
    public void onRegistration(java.lang.String name, android.os.IBinder binder) throws android.os.RemoteException;
}
