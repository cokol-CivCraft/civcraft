/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.threading.sync;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class SyncUpdateInventory implements Runnable {

    //	public static final int QUEUE_SIZE = 4096;
    public static final int UPDATE_LIMIT = 200;

    /*
     * Performs the desired action on a provided multi-inventory.
     */
    public static Queue<UpdateInventoryRequest> requestQueue = new LinkedList<>();
    public static ReentrantLock lock;

    public SyncUpdateInventory() {
        lock = new ReentrantLock();
    }

    @Override
    public void run() {

        boolean retBool = false;
        if (lock.tryLock()) {
            try {
                for (int i = 0; i < UPDATE_LIMIT; i++) {
                    UpdateInventoryRequest request = requestQueue.poll();
                    if (request == null) {
                        return;
                    }


                    switch (request.action) {
                        case ADD -> {
                            int leftovers = request.multiInv.addItem(request.stack);
                            retBool = !(leftovers > 0);
                        }
                        case REMOVE -> {
                            try {
                                retBool = request.multiInv.removeItem(request.stack, true);
                            } catch (CivException e) {
                                e.printStackTrace();
                            }
                        }
                        case SET -> {
                            retBool = true;
                            request.inv.setContents(request.cont);
                        }
                    }

                    request.result = retBool;
                    request.finished = true;
                    request.condition.signalAll();
                }
            } finally {
                lock.unlock();
            }
        } else {
            CivLog.warning("Sync update inventory lock is busy, trying again next tick");
        }
    }
}
