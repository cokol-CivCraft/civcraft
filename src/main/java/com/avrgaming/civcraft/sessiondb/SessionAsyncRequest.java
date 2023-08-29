package com.avrgaming.civcraft.sessiondb;

import com.avrgaming.civcraft.threading.TaskMaster;

public class SessionAsyncRequest {
    String tb_prefix;
    public SessionEntry entry;
    public Operation op;
    public Database database;

    public enum Operation {
        ADD,
        DELETE,
        DELETE_ALL,
        UPDATE,
        UPDATE_INSERT
    }

    public enum Database {
        GAME,
        GLOBAL
    }

    public SessionAsyncRequest(Operation op, Database data, String prefix, SessionEntry entry) {
        this.op = op;
        this.database = data;
        this.tb_prefix = prefix;
        this.entry = entry;
    }

    public void queue() {
        if (!SessionDBAsyncTimer.lock.tryLock()) {
            /* Couldn't get lock wait using async task. */
            TaskMaster.asyncTask(() -> {
                SessionDBAsyncTimer.lock.lock();
                try {
                    SessionDBAsyncTimer.requestQueue.add(SessionAsyncRequest.this);
                } finally {
                    SessionDBAsyncTimer.lock.unlock();
                }
            }, 0);
        }
        try {
            SessionDBAsyncTimer.requestQueue.add(this);
        } finally {
            SessionDBAsyncTimer.lock.unlock();
        }

    }

}
