package parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;

import util.DCUtil;
import util.DWin;

import datastore.Const;
import datastore.SSM;
import db.DBWrapper;

public class KeywordParser {
   
   public static void main(String args[]) {
      KeywordParser sh = new KeywordParser();
      //sh.normalize("The quick brown fox, jumps over the lazy blue cow");    
      
      
      //String s = sh.normalize("RADIATOR LEAKED, EVERYTHING LOCKED UP, WHEN DRIVING IT WAS HARD TO  TURN THE STEERING WHEEL.  TT");
      /*
      String s = sh.normalize("TRAVELED ON  INTERSTATE WHEN THE WHEEL AND DRUM CAME OFF THE AXEL.  WHILE CONSUMER WAS DRIVING AT APPROXIMATELY 65MPH, THE DRIVER'S SIDE AIR BAG  DID NOT DEPLOY WHEN VEHICLE HIT A HILL IN MIDDLE OF REVENE.  VEHICLE FLEW INTO MID-AIR,  LANDING ON PASSENGE...");
      
      System.out.println(s);
      Vector<TagInfo> tagInfo = sh.tag2(0, s);
      for (int i=0; i < tagInfo.size(); i++) {
         tagInfo.elementAt(i).print();   
      }
      
      System.exit(-1);
      */
      
      //String s = "Windshield wipers are dangerous because of the furry cats on the elephant, says the gas gauge";
      //String s = "Bad gas gauge ia";
      //String s = "The driver's side automatic lap and shoulder belt latch will not release from the locked position, causing the seat belt to be inoperative. Please explain further details. *ak.";
      //String s = "When making a right hand turn, the steering wheel locked up, causing an accident. *ak.";

//      System.out.println("Orig: " + s);
//      s = sh.normalize(s);
//      System.out.println("Mutated: " + s);
      
      /*
      Vector<TagInfo> tagInfo = sh.tag2(0, normalize(s));
      for (int i=0; i < tagInfo.size(); i++) {
         tagInfo.elementAt(i).print();   
      }
      */
      
      
      System.exit(0);
      
      try {
         sh.parseKeyword(-1);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(0);
      }
   }
   
   
   public KeywordParser() {
      // Domain specific key words
      abbrv.put("abs", "abs");   
      abbrv.put("mph", "mph");
      abbrv.put("ice", "ice");
      
      pStat = new ParseStat();
      
      try {
        BufferedReader reader = DCUtil.openReader(Const.DB_PART_FILE);
        
        String line = "";
        while ( (line = reader.readLine()) != null) {
           int groupId = Integer.parseInt(line.split("\t")[0]);
           String word = line.split("\t")[1];
           	
           word = word.trim();
           word = word.replaceAll("\\n", "");
           word = word.replaceAll("-", " ");
           
           // Remove the under scores so we can normalize it properly
           word = word.replaceAll("_", " ");
           String stem = normalize(word);
           
           System.out.println( groupId  + ": [" + word + "]\t" + stem);
           stemList.add( new T(word, stem, groupId) );
        }
      } catch (Exception e) { 
         e.printStackTrace();
         System.exit(0);
      }
      
      // dirty sort
      T temp[] = new T[ stemList.size() ];
      for (int i=0; i < stemList.size(); i++) temp[i] = stemList.elementAt(i);
      
      for (int i=0; i < temp.length; i++) {
         for (int j=0; j < temp.length; j++) {
            if (temp[i].word.length() >= temp[j].word.length()) {
               T t2 = temp[i];
               temp[i] = temp[j];
               temp[j] = t2;
            }
         }
      }
      stemList.clear();
      stemList.addAll( Arrays.asList(temp));
      
      for (int i=0; i < stemList.size(); i++) {
         System.out.println(stemList.elementAt(i).word);
      }
      
   }
   
   
   public void parseKeyword(int id) throws Exception {
      //DBWrapper dbh = new DBWrapper();   
      //String sql = "select cmplid, cdescr from " + SSM.database + ".cmp_clean ";
      //if (id != -1 ) sql += " where cmplid = " + id;
      
      //ResultSet rs = dbh.execute( sql ); 
      
      BufferedWriter writer  = DCUtil.openWriter(Const.DB_RELATION);
      BufferedWriter writer2 = DCUtil.openWriter(Const.DB_RELATION_OPT);
      
      BufferedReader reader = DCUtil.openReader(Const.DB_DATA);
      
      String line = "";
      
      //while (rs.next()) {
      while ( (line=reader.readLine()) != null ) {
         //int cmplid = rs.getInt(1);   
         //String txt = rs.getString(2);
         int cmplid = Integer.parseInt(line.split("\t")[0]);
         String txt = line.split("\t")[6];
         
         String txt_n = normalize(txt);
         Vector<TagInfo> t = tag2(cmplid, txt_n);
         Hashtable<Integer, Integer> tmp = new Hashtable<Integer, Integer>();
         for (int i=0; i < t.size(); i++) {
            t.elementAt(i).print();                   
            writer.write(t.elementAt(i).toString());
            writer.newLine();
            tmp.put(t.elementAt(i).groupId, t.elementAt(i).groupId);
         }
         Enumeration<Integer> en = tmp.keys();
         while (en.hasMoreElements()) {
            writer2.write(cmplid + "\t" + en.nextElement());   
            writer2.newLine();
         }
         tmp = null;
         
         pStat.totalDocument ++;
         if (t.size() > 0) {
            pStat.documentHit ++;
            pStat.totalHit += t.size();
         }
         
         if (pStat.totalDocument % 5000 == 0) {
            DWin.instance().debug("Processed : " + pStat.totalDocument);   
            writer.flush();
            writer2.flush();
         }
         
         if (pStat.totalDocument % 50000 == 0) {
            System.out.println("Cleaning and reclaiming resources...");
            System.gc();
         }
      }
      writer.flush();
      writer2.flush();
      writer.close();
      writer2.close();
      pStat.print();      
   }
   
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Version 2 - User pattern matching instead of using indexof from String class
   ////////////////////////////////////////////////////////////////////////////////
   public Vector<TagInfo> tag2(int id, String s) {
      Vector<TagInfo> tags = new Vector<TagInfo>();   
      for (int i=0; i < stemList.size(); i++) {
         String wordStr = stemList.elementAt(i).word;
         String stemStr = stemList.elementAt(i).stem;
         int groupId = stemList.elementAt(i).groupId;
         
         // Make sure we only match boundary words
         String patternStr = "\\b" + stemStr + "($|\\b)"; 
         Pattern p = Pattern.compile(patternStr);
         Matcher matcher = p.matcher(s);
         
         while (matcher.find()) {
            int tokenIdx;
            int startIdx = matcher.start();
            int size = stemStr.split("\\s").length - 1; 
            
            if (startIdx == 0) { 
               tokenIdx = 0;
            } else {
               tokenIdx = s.substring(0, startIdx).split(" ").length;
            }
            int tagSize = tags.size();
            if (tagSize > 0) {
               boolean spaceAvailable = true;
               for (int j=0; j < tagSize; j++) {
                  // Check interleave / overlapsj
                  if (  (tokenIdx >= tags.elementAt(j).start && tokenIdx <= tags.elementAt(j).end) ||
                        ( (tokenIdx+size) >= tags.elementAt(j).start && (tokenIdx+size) <= tags.elementAt(j).end) ){
                     spaceAvailable = false;
                  } 
               }
               if (spaceAvailable == true) {
                  tags.add(new TagInfo(id, groupId, tokenIdx, tokenIdx+size));  
               } 
            } else {
               tags.add(new TagInfo(id, groupId, tokenIdx, tokenIdx+size));  
            }
         } // end while
         
         
      } // end for i
      
      return tags;
   }
   
   
   
   // Takes a normalized string and tags the components
   public Vector<TagInfo> tag(String s) {
      Vector<TagInfo> tags = new Vector<TagInfo>();
      for (int i=0; i < stemList.size(); i++) {
         int fromIdx = 0;   
         int index =  0;
         String stemStr = stemList.elementAt(i).stem;
         String wordStr = stemList.elementAt(i).word;
         int groupId = stemList.elementAt(i).groupId;
        
        
         while ( (index = s.indexOf(stemStr, fromIdx)) >= 0) {
            int tokenIdx;
            int size = stemStr.split("\\s").length - 1; 
            
            String first = s.substring(0, index);
            if (first == "") {
               tokenIdx = 0;   
            } else {
               tokenIdx = first.split(" ").length;
            }
            
            int tagSize = tags.size();
            if (tagSize > 0) {
               boolean spaceAvailable = true;
               for (int j=0; j < tagSize; j++) {
                  // Check interleave / overlapsj
                  if (  (tokenIdx >= tags.elementAt(j).start && tokenIdx <= tags.elementAt(j).end) ||
                        ( (tokenIdx+size) >= tags.elementAt(j).start && (tokenIdx+size) <= tags.elementAt(j).end) ){
                     spaceAvailable = false;
                  } 
               }
               if (spaceAvailable == true) {
                  tags.add(new TagInfo(0, groupId, tokenIdx, tokenIdx+size));  
                  //System.out.println("R(" + wordStr + ") Index starts at : " + fromIdx + " position at "  + tokenIdx + " " + (tokenIdx + size));    
               } else {
                  //System.out.println("N(" + wordStr + ") Index starts at : " + fromIdx + " position at "  + tokenIdx + " " + (tokenIdx + size));    
               }
            } else {
               //System.out.println("I(" + wordStr + ") Index starts at : " + fromIdx + " position at "  + tokenIdx + " " + (tokenIdx + size));    
               tags.add(new TagInfo(0, groupId, tokenIdx, tokenIdx+size));  
            }
            
            fromIdx = index + stemStr.length();
//            System.out.println("From Index : " + fromIdx);
            
         }
      }
      return tags;
   }
   
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Inner statisticclasses
   ////////////////////////////////////////////////////////////////////////////////
   class ParseStat{
      public ParseStat() {
         totalDocument = 0;   
         documentHit = 0;   
         totalHit = 0;
      }
      public void print() {
         System.out.println("--------------------------------------------------------------------------------");
         System.out.println("Total Document Processed : " + totalDocument);   
         System.out.println("Total Document Hits : " + documentHit);
         System.out.println("Total Hits : " + totalHit);
         System.out.println("--------------------------------------------------------------------------------");
      }
      int totalDocument;
      int documentHit;
      int totalHit;
   }
   class T {
      public T(String w, String s, int g) { 
         word = w;
         stem = s;
         groupId = g;
      }
      String word = "";    
      String stem = "";
      int groupId;
   }
   
   class TagInfo {
      public TagInfo(int i, int g, int s, int e)  {
         id = i;
         groupId = g;
         start = s;
         end = e;
      }
      
      public String toString() {
         return id + "\t" + groupId + "\t" + start + "\t" + end;   
      }
      
      public void print() { System.out.println(id + "\t" + groupId + "\t" + start + "\t" + end); }
      
      int id;
      int groupId;
      int start;
      int end;
   }
   Vector<T> stemList = new Vector<T>();
   
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // String like "braking system", "brake system", "brake systems" 
   // all have the same semantics. In order to detect them
   // we can try to normalize our sentences. This is not efficient...
   // do this one time only 
   ////////////////////////////////////////////////////////////////////////////////
   public static String normalize(String s) {
//      System.out.println(">> " + s);
      String result = "";
      SnowballStemmer snowball = (SnowballStemmer)(new porterStemmer());   
      String lowerStr = s.toLowerCase();
      lowerStr = lowerStr.replaceAll("\\s\\s+", " ");
      
      String token[] = lowerStr.split("\\s");
//      String[] token = lowerStr.split("[\\p{Punct}|\\s]");
//      String[] token = lowerStr.split("\\s");
//      String[] token = lowerStr.split("\\b");
      
      for (int i=0; i < token.length; i++) {
         String r = "";
         token[i] = token[i].replaceAll("\\p{Punct}", "");
//         System.out.println(token[i]);
         // If this is an abbreviation, skip the stemming
         if ( abbrv.get( token[i] ) == null) {
            snowball.setCurrent( token[i] );
            snowball.stem();
            r = snowball.getCurrent();
         } else {
            r = token[i]; 
         }
         result += r; 
         result += " ";
      }
      
      //System.out.println("result : " + result);
      return result;
   }
   
   
//   public static Hashtable<String, String> stem2Word = new Hashtable<String, String>();
//   public static Hashtable<String, String> word2Stem = new Hashtable<String, String>();
   public static Hashtable<String, String> abbrv = new Hashtable<String, String>();
   public static ParseStat pStat;
   
   
}
