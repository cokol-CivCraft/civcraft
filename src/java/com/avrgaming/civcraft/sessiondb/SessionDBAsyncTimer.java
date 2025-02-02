package com.avrgaming.civcraft.sessiondb;

import com.avrgaming.civcraft.database.SQLController;

import java.sql.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class SessionDBAsyncTimer implements Runnable {

    private static final int UPDATE_AMOUNT = 30;
    public static ReentrantLock lock = new ReentrantLock();
    public static Queue<SessionAsyncRequest> requestQueue = new LinkedList<>();


    @Override
    public void run() {

        Connection gameConnection = null;
        Connection globalConnection = null;

        for (int i = 0; i < UPDATE_AMOUNT; i++) {
            lock.lock();
            try {
                SessionAsyncRequest request = requestQueue.poll();
                if (request == null) {
                    return;
                }

                Connection cntx;
                switch (request.database) {
                    case GAME -> {
                        if (gameConnection == null || gameConnection.isClosed()) {
                            gameConnection = SQLController.getGameConnection();
                        }
                        cntx = gameConnection;
                    }
                    case GLOBAL -> {
                        if (globalConnection == null || globalConnection.isClosed()) {
                            globalConnection = SQLController.getGameConnection();
                        }
                        cntx = globalConnection;
                    }
                    default -> {
                        return;
                    }
                }

                switch (request.op) {
                    case ADD -> performAdd(request, cntx);
                    case DELETE -> performDelete(request, cntx);
                    case DELETE_ALL -> performDeleteAll(request, cntx);
                    case UPDATE -> performUpdate(request, cntx);
                    case UPDATE_INSERT -> performUpdateInsert(request, cntx);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

    }

    public void performAdd(SessionAsyncRequest request, Connection cntx) throws Exception {

        String code = "INSERT INTO `" + request.tb_prefix + "SESSIONS` (`request_id`, `key`, `value`, `time`, `civ_id`, `town_id`, `struct_id`) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement s = cntx.prepareStatement(code, Statement.RETURN_GENERATED_KEYS);
        s.setNull(1, Types.INTEGER);
        s.setString(2, request.entry.key);
        s.setString(3, request.entry.value);
        s.setLong(4, request.entry.time);
        s.setString(5, request.entry.civ_uuid.toString());
        s.setString(6, request.entry.town_uuid.toString());
        s.setString(7, request.entry.struct_uuid.toString());


        int rs = s.executeUpdate();
        if (rs == 0) {
            throw new Exception("Could not execute SQLController code:" + code);
        }

        ResultSet res = s.getGeneratedKeys();
        while (res.next()) {
            //Grab the first one...
            request.entry.request_id = res.getInt(1);
        }
        res.close();
        s.close();

    }

    private void performUpdate(SessionAsyncRequest request, Connection cntx) throws Exception {
        String code;
        code = "UPDATE `" + request.tb_prefix + "SESSIONS` SET `value`= ? WHERE `request_id` = ?";
        PreparedStatement s = cntx.prepareStatement(code);
        s.setString(1, request.entry.value);
        s.setInt(2, request.entry.request_id);

        int rs = s.executeUpdate();
        s.close();
        if (rs == 0) {
            throw new Exception("Could not execute SQLController code:" + code + " value=" + request.entry.value + " reqid=" + request.entry.request_id);
        }

    }

    private void performUpdateInsert(SessionAsyncRequest request, Connection cntx) throws Exception {
        String code;
        code = "UPDATE `" + request.tb_prefix + "SESSIONS` SET `value`= ? WHERE `request_id` = ?";
        PreparedStatement s = cntx.prepareStatement(code);
        s.setString(1, request.entry.value);
        s.setInt(2, request.entry.request_id);

        int rs = s.executeUpdate();
        s.close();
        if (rs == 0) {
            throw new Exception("Could not execute SQLController code:" + code);
        }

    }

    private void performDeleteAll(SessionAsyncRequest request, Connection cntx) throws Exception {
        String code = "DELETE FROM `" + request.tb_prefix + "SESSIONS` WHERE `key` = ?";
        PreparedStatement s = cntx.prepareStatement(code);
        s.setString(1, request.entry.key);
        s.executeUpdate();
        s.close();

    }


    private void performDelete(SessionAsyncRequest request, Connection cntx) throws Exception {
        String code;

        code = "DELETE FROM `" + request.tb_prefix + "SESSIONS` WHERE `request_id` = ?";
        PreparedStatement s = cntx.prepareStatement(code);
        s.setInt(1, request.entry.request_id);

        int rs = s.executeUpdate();
        s.close();
        if (rs == 0) {
            throw new Exception("Could not execute SQLController code:" + code + " where entry id:" + request.entry.request_id);
        }

    }

}
