package exec;

import java.util.Vector;

import datastore.Const;
import datastore.SSM;
import parser.HierarchyParser;
import parser.KeywordParser;
import parser.Normalizer;


/////////////////////////////////////////////////////////////////////////////////
// This class (re)populates the project's database
// Note there is a dependency among the sections and it is best to run this
// program in its entirety.
//
/////////////////////////////////////////////////////////////////////////////////
public class InitDB {
   
   public static String getTable(String s) {
      return SSM.database + "." + s;   
   }
   
   
   public static void createDBFiles(String partFile, String rawFile, Vector<String> whiteList, boolean useReplacement) {
      
      try {
         System.out.println("\n\nStarting phase 1");
         HierarchyParser hierarchyParser = new HierarchyParser();
         hierarchyParser.createDBTable(partFile);
         
         System.out.println("\n\nStarting phase 2");
         Normalizer normalizer = new Normalizer();
         normalizer.parse(rawFile, 0, whiteList, useReplacement);   
         
         System.out.println("\n\nStarting phase 3");
         KeywordParser keywordParser = new KeywordParser();
         keywordParser.parseKeyword(-1);
      } catch (Exception e) {
         e.printStackTrace();   
         System.exit(0);
      }
   }
   
   
   public static void main(String args[]) {
      HierarchyParser hierarchyParser = new HierarchyParser();
      Normalizer normalizer = new Normalizer();
      KeywordParser keywordParser = new KeywordParser();
      
      
      // Build a white list here - anything outside of the
      // white list will not get parsed into the system
      // use replacement will replace identifiers
      //boolean useReplacement = true;
      boolean useReplacement = true;
      Vector<String> whiteList = new Vector<String>();
      //whiteList.add("GENERAL MOTORS CORP.");
      //whiteList.add("FORD MOTOR COMPANY");
      //whiteList.add("DAIMLERCHRYSLER CORPORATION");
      whiteList.add("TOYOTA MOTOR CORPORATION");
      
      /*
      whiteList.add("FORD MOTOR COMPANY");                       
      whiteList.add("DAIMLERCHRYSLER CORPORATION");              
      whiteList.add("TOYOTA MOTOR CORPORATION");                 
      whiteList.add("HONDA (AMERICAN HONDA MOTOR CO.)");         
      whiteList.add("NISSAN NORTH AMERICA, INC.");               
      whiteList.add("VOLKSWAGEN OF AMERICA, INC");               
      whiteList.add("CHRYSLER GROUP LLC");                       
      whiteList.add("HYUNDAI MOTOR COMPANY");                    
      whiteList.add("MAZDA NORTH AMERICAN OPERATIONS");          
      whiteList.add("MITSUBISHI MOTORS NORTH AMERICA, INC.");   
      whiteList.add("CHRYSLER LLC"); 
      */
      
      
      
      
      ////////////////////////////////////////////////////////////////////////////////
      // 0) Create table schema ?
      ////////////////////////////////////////////////////////////////////////////////
      
      ////////////////////////////////////////////////////////////////////////////////
      // 1) Parse and load the part hierarchy file
      ////////////////////////////////////////////////////////////////////////////////
      try {
         System.out.println("\n\nStarting phase 1");
         hierarchyParser.createDBTable(Const.PART_FILE);
      } catch (Exception e) {
         e.printStackTrace();   
         System.exit(0);
      }
      
      
      
      ////////////////////////////////////////////////////////////////////////////////
      // 2) Normalize text data records
      ////////////////////////////////////////////////////////////////////////////////
      try {
         System.out.println("\n\nStarting phase 2");
         //normalizer.parse(Const.DATA_FILE, 0, null);   
         normalizer.parse(Const.DATA_FILE, 0, whiteList, useReplacement);   
         
      } catch (Exception e) {
         e.printStackTrace();   
         System.exit(0);
      }
      
      
      ////////////////////////////////////////////////////////////////////////////////
      // 3) Run keywords and component extractions
      ////////////////////////////////////////////////////////////////////////////////
      try {
         System.out.println("\n\nStarting phase 3");
         //keywordParser.parseKeyword(70046); -- lock 
         //keywordParser.parseKeyword(70146); -- steering wheel
         keywordParser.parseKeyword(-1);
         
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(0);
      }
      System.out.println("All Done");
      
      
      ////////////////////////////////////////////////////////////////////////////////
      // 4) Create optimized cache from existing tables
      ////////////////////////////////////////////////////////////////////////////////
      
   }
}
