package touch;

import TUIO.TuioCursor;

////////////////////////////////////////////////////////////////////////////////
// Place holder for location aware 
////////////////////////////////////////////////////////////////////////////////
public class WCursor {
   public WCursor(int e) {
      element = e;
      cursor = null;
      x = y = 0;
   }
   public WCursor(int e, TuioCursor c) {
      element = e;
      cursor = c;
      x = c.getX();
      y = c.getY();
      timestamp = c.getTuioTime().getTotalMilliseconds();
   }
   
   public long holdTime = 0;
   public int tap = 0;
   
   public long timestamp;      // timestamp
   public float x, y;          // Normalized positions 
   public int element;         // The element associated with the touch point
   public TuioCursor cursor; 
   
}