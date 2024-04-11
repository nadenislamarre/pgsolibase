import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
    
public class PGSolibase {

    public static void upgradeDB(Connection db, String schema, String directory) throws SQLException, IOException {
	db.setAutoCommit(false);
	var st = db.createStatement();

	// lock in case an other process wants to upgrade
	st.executeQuery("select pg_advisory_lock(1)");
	int n = getDBVersion(db, schema);
	System.out.println("current db version : "+n);
	File f = dbFile(directory, n+1);
	while(f.exists()) {
	    upgradeStep(db, f, n+1);
	    // commit then relock to continue from the last version (in case a process took the lock
	    System.out.println("commit");
	    db.commit();
	    st.executeQuery("select pg_advisory_lock(1)");
	    n = getDBVersion(db, schema);
	    f = dbFile(directory, n+1);
	}
	st.close();
	db.commit();
    }

    private static File dbFile(String directory, int n) {
	return new File(directory + "/db" + n + ".sql");
    }

    private static void upgradeStep(Connection db, File f, int n) throws SQLException, IOException {
	System.out.println("upgrading schema to version "+n);

	// step from file
	Path filePath = Path.of(f.getAbsolutePath());
	String content = Files.readString(filePath);
	var stf = db.createStatement();
	System.out.println("sql:");
	System.out.print(content);
	System.out.println("-----");
	stf.execute(content);
	stf.close();

	// include the upgrade to the version
	if(n == 1) {
	    var st = db.createStatement();
	    st.execute("insert into dbparameters(key, value) values ('dbversion', 1)");
	    st.close();
	} else {
	    var st = db.prepareStatement("update dbparameters set value=? where key='dbversion'");
	    st.setString(1, Integer.toString(n));
	    st.execute();
	    st.close();
	}
    }

    public static int getDBVersion(Connection db, String schema) throws SQLException {
	int result = 0;
	var st = db.prepareStatement("select tablename from pg_tables where schemaname=? and tablename='dbparameters'");
	st.setString(1, schema);
	var rs = st.executeQuery();
	if(rs.next()) {
	    var stl = db.createStatement();
	    var rsl = stl.executeQuery("select value from " + schema + ".dbparameters where key='dbversion'");
	    if(rsl.next()) {
		result = Integer.parseInt(rsl.getString("value"));
	    }
	    rsl.close();
	}
	st.close();
	return result;
    }

    public static void main(String[] args) {
	if(args.length != 5) {
	    System.out.println("PGSolibase <url> <user> <password> <schema> <directory>");
	    System.exit(1);
	}
	try {
	    Connection db = DriverManager.getConnection(args[0], args[1], args[2]);
	    PGSolibase.upgradeDB(db, args[3], args[4]);
	    db.close();
	    System.exit(0);
        } catch (Exception  e) {
            System.err.println("Error: " + e.getMessage());
	    System.exit(1);
        }
    }
}
