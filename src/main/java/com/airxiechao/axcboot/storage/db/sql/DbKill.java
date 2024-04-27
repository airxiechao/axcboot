package com.airxiechao.axcboot.storage.db.sql;

import com.airxiechao.axcboot.storage.db.sql.model.DbProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class DbKill {

    private static final Logger logger = LoggerFactory.getLogger(DbKill.class);

    /**
     * show processlist
     * @param full
     * @param user
     * @param db
     * @param noSleep
     * @return
     */
    public static List<DbProcess> showProcessList(
            DbManager dbManager,
            boolean full,
            String user,
            String db,
            boolean noSleep
    ){
        return showProcessList(dbManager, full, user, db, null, noSleep);
    }

    public static List<DbProcess> showProcessList(
            DbManager dbManager,
            boolean full,
            String user,
            String db,
            String datasource,
            boolean noSleep
    ){
        StringBuilder sb = new StringBuilder();
        sb.append("show ");
        if(full){
            sb.append("full ");
        }
        sb.append("processlist ");

        String sql = sb.toString();

        List<DbProcess> processList = dbManager.selectBySql(sql, DbProcess.class, datasource);
        processList = processList.stream().filter(dbProcess -> {
            if(null != user && !user.equals(dbProcess.getUser())){
                return false;
            }

            if(null != db && !db.equals(dbProcess.getDb())){
                return false;
            }

            if(noSleep && "Sleep".equals(dbProcess.getCommand())){
                return false;
            }

            return true;
        }).collect(Collectors.toList());

        return processList;
    }

    /**
     * kill
     * @param pid
     * @return
     */
    public static boolean kill(DbManager dbManager, long pid){
        return kill(dbManager, pid, null);
    }

    public static boolean kill(DbManager dbManager, long pid, String datasource){
        try{
            dbManager.executeBySql("kill " + pid, datasource);
            logger.info("db killed " + pid);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * kill stalled process
     * @param full
     * @param user
     * @param db
     * @param noSleep
     * @param maxSecs
     */
    public static void killStalledProcess(
            DbManager dbManager,
            boolean full,
            String user,
            String db,
            boolean noSleep,
            int maxSecs
    ){
        killStalledProcess(dbManager, full, user, db, null, noSleep, maxSecs);
    }

    public static void killStalledProcess(
            DbManager dbManager,
            boolean full,
            String user,
            String db,
            String datasource,
            boolean noSleep,
            int maxSecs
    ){
        List<DbProcess> processes = showProcessList(dbManager, full, user, db, noSleep);
        for(DbProcess process : processes){
            if(process.getTime() > maxSecs){
                kill(dbManager, process.getId(), datasource);
            }
        }
    }
}
