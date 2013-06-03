package db;

import java.sql.*;
import java.util.HashMap;
import java.util.Vector;

import datastore.SSM;


/* Wrapper for handling data base connection, assumes a single database */
public class DBWrapper {
   
   public static Connection conn = null;
   public static Statement stmt = null;
   
   public static void main(String args[]) {
   try {
      DBWrapper dbh = new DBWrapper();   
      ResultSet rs = dbh.execute("Select * from grp");
      while(rs.next()) {
         System.out.println(rs.getInt(1));   
      }
      DBWrapper dbh2 = new DBWrapper();   
      ResultSet rs2 = dbh.execute("Select * from grp");
      while(rs2.next()) {
         System.out.println(rs2.getInt(1));   
      }
   } catch (Exception e) {e.printStackTrace();}
      
      
   }
   
   
   // Default constructor
   public DBWrapper() {
   	try {
	   	getConnection();
   	} catch (Exception e) {
   		e.printStackTrace();
   		System.exit(-1);
   	}
   	
   }
   
   public ResultSet execute(String sql) throws Exception {
      return execute(sql, false);   
   }
   
   
   public ResultSet execute(String sql, boolean useStream) throws Exception {
      System.err.print("Executing : " + sql);
      long startTime = System.currentTimeMillis();
      ResultSet rs = null;
      
      stmt = conn.createStatement();
      stmt.setQueryTimeout(60);
      
      rs = stmt.executeQuery(sql);
      
      long endTime   = System.currentTimeMillis();
      System.err.println("...done (" + (endTime-startTime)+ ")");
      
      return rs;
   }
   
   
   // Retrieves a connection object
   public Connection getConnection(String url, String username, String password) throws Exception {
      //Class.forName("com.mysql.jdbc.Driver").newInstance();
      Class.forName("org.sqlite.JDBC");
      //conn = DriverManager.getConnection("jdbc:sqlite:test.db");
      
      try {
         conn = DriverManager.getConnection("jdbc:sqlite:" + SSM.dbLocation);
      } catch (Exception e) {
         // Just pass it up
         e.printStackTrace();
         throw e;
      }
      //conn.setTransactionIsolation(1);
      return conn;
   }
   
   public Connection getConnection() throws Exception {
      return getConnection("", "", "");
      //return getConnection("jdbc:mysql://localhost/projectv3", "root", "root"); 
      //return getConnection("jdbc:mysql://localhost/"+SSM.database, "root", "root"); 
   }
   
   
   // Clean up resources
   public void cleanup() {
      try {
         if (stmt != null) {
           stmt.close();
           stmt = null;
         }
         if (conn != null) {
            conn.close();
            conn = null;
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(0);
      }
   }
}
