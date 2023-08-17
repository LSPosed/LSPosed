package org.lsposed.manager.services;

public class ServiceManager {
    private static LSPManagerDispatchService clientService = null;

    public  static  void start(){
        clientService = new LSPManagerDispatchService();
    }
}
