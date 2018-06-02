package org.kissweb.rest;

import org.kissweb.database.Connection;
import org.kissweb.database.Record;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyVetoException;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Author: Blake McBride
 * Date: 5/4/18
 */
public class ServiceBase extends HttpServlet {

    private static Connection.ConnectionType connectionType;
    private static String host;
    private static String database;
    private static String user;
    private static String password;
    private static String applicationPath;
    private static boolean underIDE = false;
    private static final String IDEPath = "Kiss.war";
    private static ComboPooledDataSource cpds;
    protected Connection DB;

    public static String getApplicationPath() {
        return applicationPath;
    }

    private static File search(File f) {
        File[] list = f.listFiles();
        if (list != null)
            for (File f2 : list)
                if (f2.isDirectory()) {
                    if (underIDE && f2.getName().equals(IDEPath))
                        continue;
                    File f3 = search(f2);
                    if (f3 != null)
                        return f3;
                } else if (f2.getName().equals("KissInit.groovy")) {
                    return f2;
                }
        return null;
    }

    static void setApplicationPath(HttpServletRequest request) {
        String path = request.getServletContext().getRealPath("/");
        path = path.replaceAll("\\\\", "/");  // for Windows
        System.out.println("* * * Context path = " + path);
        if (path.endsWith("/" + IDEPath + "/")) {
            underIDE = true;
            System.out.println("* * * Is running under IDE");
        } else
            System.out.println("* * * Is not running under IDE");
        File f;
        do {
            path = path.substring(0, path.lastIndexOf('/'));
            if (underIDE)
                path = path.substring(0, path.lastIndexOf('/'));
            f = search(new File(path));
        } while (f == null  &&  path.lastIndexOf('/') > 0);

        if (f == null)
            return;

        path = f.getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('/'));

        ServiceBase.applicationPath = path + "/";
        System.out.println("* * * Application path set to " + path);
    }

    public static boolean isUnderIDE() {
        return underIDE;
    }

    protected String login(String user, String password) throws Exception {
        Record rec = DB.fetchOne("select * from users where user_name = ? and user_password = ? and user_active = 'Y'", user, password);
        if (rec == null)
            throw new Exception("Invalid login.");
        UserCache.UserData ud = UserCache.newUser(user, password);
        ud.user_id = rec.getInt("user_id");
        return ud.uuid;
    }

    protected int checkLogin(String uuid) throws Exception {
        UserCache.UserData ud = UserCache.findUser(uuid);
        if (ud == null)
            throw new Exception("Login timed out; please re-login.");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeout = ud.lastAccessDate.plusSeconds(120);  // cache user data for 120 seconds
        if (now.isAfter(timeout)) {
            Record rec = DB.fetchOne("select * from users where user_name = ? and user_password = ? and user_active = 'Y'", ud.username, ud.password);
            if (rec == null) {
                UserCache.removeUser(uuid);
                throw new Exception("Invalid login.");
            }
        }
        ud.lastAccessDate = LocalDateTime.now();
        return ud.user_id;
    }

    protected void closeSession() {
        try {
            if (DB != null)
                DB.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DB = null;
        }
    }

    protected static String getDynamicClassPath() {
        return "";
    }

    protected void makeDatabaseConnection () throws PropertyVetoException, SQLException, ClassNotFoundException {
        if (cpds == null) {
            System.out.println("* * * Attempting to connect to database " + host + ":" + database + ":" + user);
            String cstr = Connection.makeConnectionString(connectionType, host, database, user, password);
            Connection con;
            try {
                con = new Connection(connectionType, cstr);
            } catch (Exception e) {
                System.out.println("* * * Database connection failed (see application/KissInit.groovy)");
                System.out.println("* * * " + e.getMessage());
                throw e;
            }
            con.close();
            System.out.println("* * * Database connection succeeded");

            cpds = new ComboPooledDataSource();

            cpds.setJdbcUrl(cstr);

            cpds.setDriverClass( Connection.getDriverName(connectionType) );

            cpds.setMaxStatements( 180 );
        }
    }

    protected void newDatabaseConnection () throws SQLException {
        DB = new Connection(cpds.getConnection());
    }

    public static Connection.ConnectionType getConnectionType() {
        return connectionType;
    }

    public static void setConnectionType(Connection.ConnectionType connectionType) {
        ServiceBase.connectionType = connectionType;
    }

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        ServiceBase.host = host;
    }

    public static String getDatabase() {
        return database;
    }

    public static void setDatabase(String database) {
        ServiceBase.database = database;
    }

    public static String getUser() {
        return user;
    }

    public static void setUser(String user) {
        ServiceBase.user = user;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        ServiceBase.password = password;
    }
}
