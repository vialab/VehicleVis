package gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Vector;

import javax.media.opengl.GL2;

import model.DCColour;

import org.jdesktop.animation.timing.Animator;

import util.DCUtil;
import util.GraphicUtil;
import util.TextureFont;

import com.jogamp.opengl.util.awt.TextureRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;

import datastore.Const;
import datastore.SSM;
import datastore.SchemeManager;




/////////////////////////////////////////////////////////////////////////////////
// Combo Box implementation
// Basically this is a selectable scroll panel, with an additional pane that 
// triggers the scroll pane and show the selected item.
// 
// Note we assume that the scroll list item is a unique list
// Note this is hardcoded to roll up
/////////////////////////////////////////////////////////////////////////////////
public class DCScrollPane {
   
   public float anchorX = 150.0f;
   public float anchorY = 150.0f;   
   
   public float width = 230.0f;
   //public float height = 200.0f;
   public float height = 0.0f;
   public float yoffset = height;
   
   
   public static float buttonHeight = 10.0f;
   public static float spacing = 22.0f;
   public static short UP   =   1;
   public static short DOWN =   2;
   
   public int maxValue = 0;
   
   
   public float texPanelWidth = 1.0f;
   public float texPanelHeight = 8000.0f;
   
   public boolean visible = true;
   
   //public boolean masterVisible = false; // master level visibility control
   public boolean masterVisible = true; // master level visibility control
   
   public TextureRenderer texture; 
   public Graphics2D g2d;
   
   public static Font font = DCUtil.loadFont(Const.FONT_PATH+"din1451m.ttf", Font.PLAIN, 13f);
   public static Font fontBold = DCUtil.loadFont(Const.FONT_PATH+"din1451m.ttf", Font.BOLD, 13f);
   
   public TextureFont tf = new TextureFont();   
   
   public short direction;
   
   public float depth = 0.0f;
   
   public FontMetrics fm;   
   public Vector<GTag> tagList = new Vector<GTag>();
   
   // The currently selected item in the taglist
   public int current = 0;
   public String currentStr = ""; 
   
   public String label = "";
   
   
   public String uLabel = ""; // Just a way of uniquely identifying the widget by string name, used for logging
   
   
   public boolean dirty = false;
   
   public Animator animator;
   
   public void setHeight(float h) { height = h; }
   public float getHeight() { return height; }
   
   public DCScrollPane(String s) {
      this();
      label = s;
      direction = UP;
   }
   public DCScrollPane() {
      texture = new TextureRenderer((int)texPanelWidth, (int)texPanelHeight, true, true);
      g2d = texture.createGraphics();
      g2d.setFont(font);      
      
      // Get a metric
      fm = g2d.getFontMetrics();
      direction = UP;
   }
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Calculate the height of the panel, estimate the height space for each
   // text item, assuming that text item will only span a single line
   ////////////////////////////////////////////////////////////////////////////////
   public void calculate() {
      tagList.clear();
      // TODO: Calculate real height and with real data
      for (int i=0; i < 10; i++) {
         tagList.add( new GTag(10, (i+1)*spacing, 0+i*spacing, "Test " + i, "Test " + i, -1));    
      }
   }
   
   /*
   public void calculate(String fromDate, String toDate, Integer fromMonth, Integer toMonth, Integer group) {
      int startIdx = CacheManager.instance().getDateKey(fromDate);
      int endIdx   = CacheManager.instance().getDateKey(toDate);
      
      CacheManager.instance().queryTable;
      
      System.out.println("Scroll get period is from " + startIdx + " to " + endIdx );
   }
   */
   
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Render the text content with graphics2D into a opengl compatible texture
   ////////////////////////////////////////////////////////////////////////////////
   public void renderToTexture(Color c) {
      
      
      // Recreate the textureRenderer to make sure we have the write context  
      texture = null;
      texture = new TextureRenderer((int)texPanelWidth, (int)texPanelHeight, true, true);
      g2d = texture.createGraphics();
      
      // Setup anti-aliasing fonts
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
      //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER));
      
      // Render the tags
      for (int i=0; i < tagList.size(); i++) {
         GTag t = tagList.elementAt(i);
         
         /*
         if (i % 2 == 0) 
            g2d.setColor(DCColour.fromInt(200, 200, 200, 255).awtRGBA());
         else 
            g2d.setColor(DCColour.fromInt(230, 230, 230, 255).awtRGBA());
         */
         g2d.setColor(DCColour.fromInt(240, 240, 240, 255).awtRGBA());
         g2d.fillRect((int)0, (int)t.yPrime, (int)width, (int)spacing);
         
         
         
         if (currentStr.equals(t.val))
            g2d.setFont(fontBold);
         else 
            g2d.setFont(font);
         
         if (i > 0) {
            if (currentStr.equals(t.val)) 
               g2d.setColor( SchemeManager.selected.awtRGBA());
            else
               g2d.setColor( DCColour.fromInt(210, 210, 210, 255).awtRGBA());
            g2d.fillRect((int)1, (int)t.y-12, (int)(0.95*this.width*((float)t.num/(float)this.maxValue)), 8);
         }
         
         // Draw da text
         g2d.setColor(Color.BLACK);
         g2d.drawString(t.txt, t.x, t.y-3); //-3 is just a padding to make it like nicer (more or less centered)
         
      }
      
      tf.anchorX = this.anchorX+10;
      tf.anchorY = this.anchorY-18;
      tf.width = this.width;
      tf.height = 20;
      tf.clearMark();
      tf.addMark(this.currentStr, Color.black, fontBold, 1, 1);
      tf.renderToTexture(null);
      
   }   
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Renders the texture, take into account the current offset (scroll amount)
   ////////////////////////////////////////////////////////////////////////////////
   public void render(GL2 gl2) {
      // If not visible, don't render....duh ?
      if (! this.masterVisible) return;
      if (! this.visible) return;
      
      if (dirty) {
         this.renderToTexture(Color.BLACK);
         this.dirty = false;
      }
      
      tf.anchorX = this.anchorX+10;
      tf.anchorY = this.anchorY-18;
      
      Texture t = texture.getTexture();
      t.enable(gl2);
      t.bind(gl2);
      TextureCoords tc = t.getImageTexCoords();
      
      //gl2.glEnable(GL2.GL_BLEND);
      gl2.glDisable(GL2.GL_BLEND);
      gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);            
      
      
      gl2.glColor4d(1.0, 1.0, 1.0, 0.8);
      gl2.glEnable(GL2.GL_TEXTURE_2D);
      
      if (direction == UP) {
	      gl2.glBegin(GL2.GL_QUADS);
	         //gl2.glTexCoord2f(0, 1);
	         gl2.glTexCoord2f(0, yoffset/texPanelHeight);
	         gl2.glVertex3f(anchorX, anchorY,depth);
	         
	         //gl2.glTexCoord2f(1, 1);
	         gl2.glTexCoord2f(1, yoffset/texPanelHeight);
	         gl2.glVertex3f(anchorX+width, anchorY,depth);
	         
	         //gl2.glTexCoord2f(1, 0);
	         gl2.glTexCoord2f(1, (yoffset-height)/texPanelHeight);
	         gl2.glVertex3f(anchorX+width, anchorY+height,depth);
	         
	         //gl2.glTexCoord2f(0, 0);
	         gl2.glTexCoord2f(0, (yoffset-height)/texPanelHeight);
	         gl2.glVertex3f(anchorX, anchorY+height,depth);
	      gl2.glEnd();
      } else {
 	      gl2.glBegin(GL2.GL_QUADS);
	         //gl2.glTexCoord2f(0, 1);
	         gl2.glTexCoord2f(0, yoffset/texPanelHeight);
	         gl2.glVertex3f(anchorX, anchorY-height-SSM.scrollHeight,depth);
	         
	         //gl2.glTexCoord2f(1, 1);
	         gl2.glTexCoord2f(1, yoffset/texPanelHeight);
	         gl2.glVertex3f(anchorX+width, anchorY-height-SSM.scrollHeight,depth);
	         
	         //gl2.glTexCoord2f(1, 0);
	         gl2.glTexCoord2f(1, (yoffset-height)/texPanelHeight);
	         gl2.glVertex3f(anchorX+width, anchorY-SSM.scrollHeight,depth);
	         
	         //gl2.glTexCoord2f(0, 0);
	         gl2.glTexCoord2f(0, (yoffset-height)/texPanelHeight);
	         gl2.glVertex3f(anchorX, anchorY-SSM.scrollHeight,depth);
	      gl2.glEnd();     	
      }
      t.disable(gl2);      
      
      // To give it a more widget-y feel
      gl2.glColor4d(0.4, 0.4, 0.4, 0.5);
      if (direction == DCScrollPane.UP) {
         gl2.glBegin(GL2.GL_LINES);
            gl2.glVertex2d( anchorX+width+1, anchorY);
            gl2.glVertex2d( anchorX+width+1, anchorY+height);
         gl2.glEnd();
      } else {
         gl2.glBegin(GL2.GL_LINES);
	         gl2.glVertex3d(anchorX+width+1, anchorY-SSM.scrollHeight,depth);
	         gl2.glVertex3d(anchorX+width+1, anchorY-height-SSM.scrollHeight,depth);
         gl2.glEnd();
      }
      
      
      // Draw the buttons and stuff
      GraphicUtil.drawRoundedRect(gl2, anchorX+(width/2), anchorY-(SSM.scrollHeight/2), 0, (width/2), SSM.scrollHeight/2, 5, 6,
            DCColour.fromDouble(0.68, 0.68, 0.68, 0.65).toArray(), 
            DCColour.fromDouble(0.77, 0.77, 0.77, 0.65).toArray());
      
      
      // Draw a indicator to tell whether the drop down is open or not
      gl2.glColor4d(0.5, 0.5, 0.5, 0.5);
      if (this.height > 0) {
         gl2.glBegin(GL2.GL_TRIANGLES);
            gl2.glVertex2d(anchorX+(width)-25, anchorY-(SSM.scrollHeight/2)-5);
            gl2.glVertex2d(anchorX+(width)-5, anchorY-(SSM.scrollHeight/2)-5);
            gl2.glVertex2d(anchorX+(width)-15, anchorY-(SSM.scrollHeight/2)+5);
         gl2.glEnd();
      } else {
         gl2.glBegin(GL2.GL_TRIANGLES);
            gl2.glVertex2d(anchorX+(width)-25, anchorY-(SSM.scrollHeight/2)+5);
            gl2.glVertex2d(anchorX+(width)-5, anchorY-(SSM.scrollHeight/2)+5);
            gl2.glVertex2d(anchorX+(width)-15, anchorY-(SSM.scrollHeight/2)-5);
         gl2.glEnd();
      }
      
      
      if (current >= 0 && tagList.size() > 0)  {
         tf.render(gl2, false); // Do not use blending for this, to prevent labels showing through multiple layers
      }
      
      
   }
   
   

}
