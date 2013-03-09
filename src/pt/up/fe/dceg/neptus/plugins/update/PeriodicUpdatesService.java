/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.plugins.update;

import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

public class PeriodicUpdatesService {

    protected static LinkedBlockingQueue<UpdateRequest> updateRequests = new LinkedBlockingQueue<UpdateRequest>();
    private static boolean updatesEnabled = true;
    private static final int DEFAULT_NUMBER_OF_THREADS = 2;
    private static boolean started = false;
    private static Vector<IPeriodicUpdates> clients = new Vector<IPeriodicUpdates>();
    private static Vector<IPeriodicUpdates> defunctClients = new Vector<IPeriodicUpdates>();
    private static Vector<Thread> updaterThreads = new Vector<Thread>();
    private PeriodicUpdatesService() {}

    protected static LinkedHashMap<Object, Long> updateTimes = new LinkedHashMap<Object, Long>(); 

    /**
     * Verifies that the updates are currently enabled
     * @return <b>true</b> if updates are enabled or <b>false</b> otherwise
     */
    public static boolean isUpdatesEnabled() {
        synchronized (PeriodicUpdatesService.class) {
            return updatesEnabled;
        }
    }

    /**
     * Deactivates updates 
     */
    public static void stopUpdating() {
        synchronized (PeriodicUpdatesService.class) {
            updatesEnabled = false;			
        }
    }

    /**
     * Adds a new client that is to be updated periodically
     * @param client The client that wants to be warned at specific time intervals
     */
    public static void register(IPeriodicUpdates client) {
        if (clients.contains(client)) {
            NeptusLog.pub().info("Code in "+ReflectionUtil.getCallerStamp()+" tried to add an already registered updates client");
            return;
        }
        updateTimes.put(client, 0L);
        clients.add(client);

        updateRequests.add(new UpdateRequest(client));
        if (!started) {
            started = true;
            for (int i = 0; i < DEFAULT_NUMBER_OF_THREADS; i++) {
                Thread t = getUpdaterThread("Periodic Updates - "+(i+1));
                updaterThreads.add(t);
                t.setDaemon(true);
                t.start();
            }
        }
    }

    /**
     * Removes a client that doesn't want to be updated anymore
     * @param client The client that wants to be removed
     */
    public static void unregister(IPeriodicUpdates client) {
        
        updateTimes.remove(client);
        
        if (clients.contains(client)) {
            clients.remove(client);
            if (clients.isEmpty()) {
                System.out.println("Periodic Listener Service with 0 clients - cleaning...");
                for (Thread t : updaterThreads) {
                    t.interrupt();
                }
                started = false;
                defunctClients.clear();
            }
            else
                defunctClients.add(client);				
        }
    }



    /**
     * Calculates the nearest upcoming update, waits the time until the update time and calls the update method
     */
    private static boolean nextUpdate() {
        UpdateRequest ur = null;

        try {
            ur = updateRequests.take();
            
            while (ur == null || ur.getNextUpdateTime() >= System.currentTimeMillis()) {
                if(defunctClients.contains(ur.getSource())) {
                    defunctClients.remove(ur.getSource());
                    ur = null;		
                }

                if (ur != null)
                    updateRequests.put(ur);
                Thread.sleep(10);
                ur = updateRequests.take();					
            }
            long time = System.currentTimeMillis();
            if (!updateTimes.containsKey(ur.getSource()))
                updateTimes.put(ur.getSource(), 0L);
            boolean not_finished = true;
            try {
                not_finished = ur.update();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            catch (Error e) {
                e.printStackTrace();
            }
            time = System.currentTimeMillis()-time;
            Long lastTime = updateTimes.get(ur.getSource());
            if (lastTime != null)
                updateTimes.put(ur.getSource(), lastTime+time);

            if (not_finished)
                updateRequests.add(ur);
            else
                unregister(ur.getSource());				
        }
        catch (InterruptedException e) {
            return false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static Thread getUpdaterThread(String name) {
        Thread t =  new Thread(name) {
            @Override
            public void run() {
                while(isUpdatesEnabled() && nextUpdate())
                    ;					
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    }



    public static void main(String[] args) {

        PeriodicUpdatesService.register(new IPeriodicUpdates() {
            long previousTime = System.currentTimeMillis();

            @Override
            public long millisBetweenUpdates() {
                return 100;
            }

            @Override
            public boolean update() {
                System.out.println("a "+(System.currentTimeMillis()-previousTime));
                previousTime = System.currentTimeMillis();
                return true;
            }
        });

        PeriodicUpdatesService.register(new IPeriodicUpdates() {
            long previousTime = System.currentTimeMillis();
            @Override
            public long millisBetweenUpdates() {
                return 900;
            }

            @Override
            public boolean update() {
                System.out.println("b "+(System.currentTimeMillis()-previousTime));
                previousTime = System.currentTimeMillis();
                return true;
            }
        });
    }

}
