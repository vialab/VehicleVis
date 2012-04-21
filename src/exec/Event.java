package exec;

import model.DCTriple;
import model.LensAttrib;
import model.PaneAttrib;
import util.DCCamera;
import util.DCUtil;
import util.MatrixUtil;
import Jama.Matrix;
import datastore.CacheManager;
import datastore.SSM;


////////////////////////////////////////////////////////////////////////////////
// Holds methods to handle discrete events
////////////////////////////////////////////////////////////////////////////////
public class Event {
   ////////////////////////////////////////////////////////////////////////////////
   // Creates a lens at (posX, posY)
   ////////////////////////////////////////////////////////////////////////////////
   public static void createLens(int posX, int posY) {
      LensAttrib la = new LensAttrib( posX, posY, 100.0f, 0);      
      la.magicLensType = LensAttrib.LENS_DEPTH;
      SSM.instance().lensList.add( la );
      SSM.instance().refreshMagicLens = true;
   }
   
   ////////////////////////////////////////////////////////////////////////////////
   // Remove a lens at (posX, posY)
   ////////////////////////////////////////////////////////////////////////////////
   public static void removeLens(int posX, int posY) {
      // TODO: This is a bit buggy due to the removal while still iterating the list
      for (int i=0; i < SSM.instance().lensList.size(); i++) {
         float x = (float)posX - (float)SSM.instance().lensList.elementAt(i).magicLensX;
         float y = (float)posY - (float)SSM.instance().lensList.elementAt(i).magicLensY;
         float r = (float)SSM.instance().lensList.elementAt(i).magicLensRadius;
         float d = (float)Math.sqrt(x*x + y*y);            
         if (d < r) {
            SSM.instance().lensList.remove(i);   
         }
      }
   }
   
   ////////////////////////////////////////////////////////////////////////////////
   // Move lens by delta
   ////////////////////////////////////////////////////////////////////////////////
   public static void moveLens(int posX, int posY, int oldPosX, int oldPosY) {
      for (int i=0; i < SSM.instance().lensList.size(); i++) {
         if (SSM.instance().lensList.elementAt(i).magicLensSelected == 1) {
            SSM.instance().lensList.elementAt(i).magicLensX += posX - oldPosX;   
            SSM.instance().lensList.elementAt(i).magicLensY += posY - oldPosY;   
            SSM.instance().lensList.elementAt(i).start = 0;
         }
      }      
   }
   
   public static void moveLensTUIO(int posX, int posY, int oldPosX, int oldPosY) {
      for (int i=0; i < SSM.instance().lensList.size(); i++) {
         float x = (float)posX - (float)SSM.instance().lensList.elementAt(i).magicLensX;
         float y = (float)posY - (float)SSM.instance().lensList.elementAt(i).magicLensY;
         float r = (float)SSM.instance().lensList.elementAt(i).magicLensRadius;
         float d = (float)Math.sqrt(x*x + y*y);            
         if (d < r) {
            SSM.instance().lensList.elementAt(i).magicLensX += posX - oldPosX;   
            SSM.instance().lensList.elementAt(i).magicLensY += posY - oldPosY;   
            SSM.instance().lensList.elementAt(i).start = 0;
         }
      }      
   }
  
   ////////////////////////////////////////////////////////////////////////////////
   // Resize lens by delta
   ////////////////////////////////////////////////////////////////////////////////
   public static void resizeLens(int posX, int posY, int oldPosX, int oldPosY) {
      for (int i=0; i < SSM.instance().lensList.size(); i++) {
         if (SSM.instance().lensList.elementAt(i).magicLensSelected == 1) {
            float x = (float)posX - (float)SSM.instance().lensList.elementAt(i).magicLensX;
            float y = (float)posY - (float)SSM.instance().lensList.elementAt(i).magicLensY;
            float d = (float)Math.sqrt(x*x + y*y);         
            SSM.instance().lensList.elementAt(i).magicLensRadius = d;  
         }
      }
   }
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Change lens cutting plane
   ////////////////////////////////////////////////////////////////////////////////
   public static int scrollLens(int posX, int posY, int unit) {
      int flag = 0;
      for (int i=0; i < SSM.instance().lensList.size(); i++) {
         float x = (float)posX - (float)SSM.instance().lensList.elementAt(i).magicLensX;
         float y = (float)posY - (float)SSM.instance().lensList.elementAt(i).magicLensY;
         float r = (float)SSM.instance().lensList.elementAt(i).magicLensRadius;
         float d = (float)Math.sqrt(x*x + y*y);
         
         if ( d <= r ) {
            flag = 1;
            LensAttrib la = SSM.instance().lensList.elementAt(i);
            
            if (la != null ) {
               if (unit < 0) {
                  double totalD = DCCamera.instance().eye.sub(new DCTriple(0,0,0)).mag();
                  double remainD = totalD - la.nearPlane;
                  double advD    = Math.max(0.3f, remainD*0.05);
                  
                  if (la.nearPlane + advD < totalD)
                     la.nearPlane += advD;
               } else {
                  double totalD = DCCamera.instance().eye.sub(new DCTriple(0,0,0)).mag();
                  double remainD = totalD - la.nearPlane;
                  double advD    = Math.max(0.3f, remainD*0.05);
                  
                  if (la.nearPlane - advD > 0) 
                     la.nearPlane -= advD;               
               }
               SSM.instance().refreshMagicLens = true;
            }            
         }
      }        
      return flag;
   }
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Change camera position
   ////////////////////////////////////////////////////////////////////////////////
   public static void setCamera(int posX, int posY, int oldPosX, int oldPosY) {
      double basis[][] = {
            { DCCamera.instance().right.x, DCCamera.instance().right.y, DCCamera.instance().right.z, 0 },      
            { DCCamera.instance().up.x, DCCamera.instance().up.y, DCCamera.instance().up.z, 0 },      
            { DCCamera.instance().look.x, DCCamera.instance().look.y, DCCamera.instance().look.z, 0 },      
            { 0, 0, 0, 1}
         };
      Matrix m_basis      = new Matrix(basis);
      Matrix m_basisT     = m_basis.inverse();         

      if ( oldPosX != posX ) {
         float factor; 
         if (SSM.instance().useDualDepthPeeling) {
            factor= oldPosX > posX ? -3.0f : 3.0f; 
         } else {
            factor= oldPosX > posX ? -1.0f : 1.0f; 
         }
         Matrix m_rotation    = MatrixUtil.rotationMatrix(factor*2.0, "Y");
         Matrix m = m_basisT.times(m_rotation).times(m_basis);
         double[] newPosition = MatrixUtil.multVector(m, DCCamera.instance().eye.toArrayd());
         DCCamera.instance().eye = new DCTriple(newPosition);
         DCTriple newDir = new DCTriple(0.0f, 0.0f, 0.0f).sub(DCCamera.instance().eye);
         newDir.normalize();
         DCCamera.instance().look = new DCTriple(newDir);
         DCCamera.instance().right = DCCamera.instance().look.cross(DCCamera.instance().up);
         DCCamera.instance().right.normalize();
         
         SSM.instance().refreshOITTexture = true;
      }
      
      if ( oldPosY != posY) {
         float factor;
         if (SSM.instance().useDualDepthPeeling) {
            factor = oldPosY > posY ? -3.0f : 3.0f; 
         } else {
            factor = oldPosY > posY ? -1.0f : 1.0f; 
         }
         Matrix m_rotation    = MatrixUtil.rotationMatrix(factor*2.0, "X");
         Matrix m = m_basisT.times(m_rotation).times(m_basis);
         double[] newPosition = MatrixUtil.multVector(m, DCCamera.instance().eye.toArrayd());
         DCCamera.instance().eye = new DCTriple(newPosition);
         DCTriple newDir = new DCTriple(0.0f, 0.0f, 0.0f).sub(DCCamera.instance().eye);
         newDir.normalize();
         DCCamera.instance().look = new DCTriple(newDir);
         DCCamera.instance().up = DCCamera.instance().look.cross(DCCamera.instance().right).mult(-1.0f);
         DCCamera.instance().up.normalize();
         
         SSM.instance().refreshOITTexture = true;
      }      
   }
   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Scroll filter panel
   ////////////////////////////////////////////////////////////////////////////////
   public static void setScrollPanelOffset(PaneAttrib attrib, int posY, int oldPosY) {
      attrib.yOffset -= (posY - oldPosY);   
      if (attrib.yOffset < attrib.height)
         attrib.yOffset = attrib.height;
      if (attrib.yOffset > attrib.textureHeight)
         attrib.yOffset = attrib.textureHeight;   
   }   
   
   ////////////////////////////////////////////////////////////////////////////////
   // Check drag movement against GUI elements
   ////////////////////////////////////////////////////////////////////////////////
   public static void checkGUIDrag(int posX, int posY, int oldPosX, int oldPosY) {
      // Check the top level UI elements
      if (SSM.instance().topElement == SSM.ELEMENT_DOCUMENT) {
         SSM.docAnchorX += (posX - oldPosX);   
         SSM.docAnchorY -= (posY - oldPosY);   
      // For default filter   
      } else if (SSM.instance().topElement == SSM.ELEMENT_MANUFACTURE_SCROLL) {
         setScrollPanelOffset(SSM.instance().manufactureAttrib, posY, oldPosY);
      } else if (SSM.instance().topElement == SSM.ELEMENT_MAKE_SCROLL) {
         setScrollPanelOffset(SSM.instance().makeAttrib, posY, oldPosY);
      } else if (SSM.instance().topElement == SSM.ELEMENT_MODEL_SCROLL)  {
         setScrollPanelOffset(SSM.instance().modelAttrib, posY, oldPosY);
      } else if (SSM.instance().topElement == SSM.ELEMENT_YEAR_SCROLL)  {
         setScrollPanelOffset(SSM.instance().yearAttrib, posY, oldPosY);
      // For comparisons   
      } else if (SSM.instance().topElement == SSM.ELEMENT_CMANUFACTURE_SCROLL) {
         setScrollPanelOffset(SSM.instance().c_manufactureAttrib, posY, oldPosY);
      } else if (SSM.instance().topElement == SSM.ELEMENT_CMAKE_SCROLL) {
         setScrollPanelOffset(SSM.instance().c_makeAttrib, posY, oldPosY);
      } else if (SSM.instance().topElement == SSM.ELEMENT_CMODEL_SCROLL)  {
         setScrollPanelOffset(SSM.instance().c_modelAttrib, posY, oldPosY);
      } else if (SSM.instance().topElement == SSM.ELEMENT_CYEAR_SCROLL)  {
         setScrollPanelOffset(SSM.instance().c_yearAttrib, posY, oldPosY);
      // Save and load stuff         
      } else if (SSM.instance().topElement == SSM.ELEMENT_SAVELOAD_SCROLL) {
         SSM.instance().saveLoadYOffset -= (SSM.instance().mouseY - SSM.instance().oldMouseY);   
         if (SSM.instance().saveLoadYOffset < SSM.instance().saveLoadHeight)
            SSM.instance().saveLoadYOffset = SSM.instance().saveLoadHeight;
         if (SSM.instance().saveLoadYOffset > SSM.instance().saveLoadTexHeight)
            SSM.instance().saveLoadYOffset = SSM.instance().saveLoadTexHeight;
      }      
   }
   
   ////////////////////////////////////////////////////////////////////////////////
   // Scroll document panel
   ////////////////////////////////////////////////////////////////////////////////
   public static void checkDocumentScroll(int posX, int posY, int unit) {
      if (unit < 0) {
         // Prevent underflow
         if (SSM.yoffset <= SSM.docHeight) return;
         
         if (SSM.yoffset <= SSM.instance().t1Height && SSM.instance().t1Start > 0 ) {
            SSM.instance().t1Start = Math.max(0, SSM.instance().t1Start - SSM.instance().globalFetchSize);
            SSM.instance().t2Start = Math.max(SSM.instance().globalFetchSize, SSM.instance().t2Start - SSM.instance().globalFetchSize);
            SSM.instance().docAction = 1;   
            SSM.instance().dirtyGL = 1;
         } else {
            SSM.yoffset += unit*5;
         }            
      } else {
         if (SSM.yoffset > SSM.instance().t1Height && SSM.instance().t2Height <= 0) return;
         
         // Check to see if we have run off the number allocated for the period
         if (SSM.instance().t2Start + SSM.instance().globalFetchSize > SSM.instance().docMaxSize) {
            if (SSM.yoffset >= SSM.instance().t1Height + SSM.instance().t2Height)
               return;
         }
         
         if (SSM.yoffset - SSM.docHeight > SSM.instance().t1Height) {
            SSM.yoffset -= SSM.instance().t1Height;
            SSM.instance().t1Start += SSM.instance().globalFetchSize;
            SSM.instance().t2Start += SSM.instance().globalFetchSize;
            SSM.instance().docAction = 2;   
            SSM.instance().dirtyGL = 1;
         } else {
            SSM.yoffset += unit*5;
         }            
      }      
   }
   
   ////////////////////////////////////////////////////////////////////////////////
   // Check to see if the mouse cursor is in the area where
   // the text is drawn
   ////////////////////////////////////////////////////////////////////////////////
   public static boolean inDocContext(int posX, int posY) {
      if ( ! SSM.instance().docActive ) return false;
      float mX = posX;
      float mY = SSM.windowHeight - posY;
      if (DCUtil.between( mX, SSM.docAnchorX, SSM.docAnchorX+SSM.docWidth)) {
         if (DCUtil.between( mY, SSM.docAnchorY, SSM.docAnchorY+SSM.docHeight)) {
            return true;   
         }
      }
      return false;
   }  
   
   
   // Set the top element to id if mouse is clicked over the panel
   public static int checkScrollPanels(int posX, int posY, PaneAttrib attrib, int id) {
      float mx = posX;
      float my = SSM.windowHeight - posY;
      
      float anchorX = attrib.anchorX;
      float anchorY = attrib.anchorY;
      
      if (DCUtil.between(mx, anchorX, anchorX+SSM.instance().scrollWidth)) {
         if (attrib.direction == 1) {
            if (DCUtil.between(my, anchorY-20, anchorY+attrib.height)) {
               if (attrib.active) SSM.instance().topElement = id;
               return id;
            }
         } else {
            if (DCUtil.between(my, anchorY-20-attrib.height, anchorY)) {
               if (attrib.active) SSM.instance().topElement = id;
               return id;
            }
         }
      }
      return SSM.ELEMENT_NONE;
   }
   
   public static int checkDocumentPanel(int posX, int posY) {
     // Detecting the document text area
     if (Event.inDocContext(posX, posY)) {
        SSM.instance().topElement = SSM.ELEMENT_DOCUMENT;   
        return SSM.ELEMENT_DOCUMENT;
     }
     float mx = posX;
     float my = SSM.windowHeight - posY;
     float anchorX = SSM.docAnchorX;
     float anchorY = SSM.docAnchorY;
     float docWidth  = SSM.docWidth;
     float docHeight = SSM.docHeight;
     float padding   = SSM.docPadding;         
     // Detecting the document borders
     if (DCUtil.between(mx, anchorX-padding, anchorX) || DCUtil.between(mx, anchorX+docWidth, anchorX+docWidth+padding)) {
        if (DCUtil.between(my, anchorY-padding, anchorY+docHeight+padding)) {
           SSM.instance().topElement = SSM.ELEMENT_DOCUMENT;   
           return SSM.ELEMENT_DOCUMENT;
        }
     }
     if (DCUtil.between(my, anchorY-padding, anchorY) || DCUtil.between(my, anchorY+docHeight, anchorY+docHeight+padding)) {
        if (DCUtil.between(mx, anchorX-padding, anchorX+docWidth+padding)) {
           SSM.instance().topElement = SSM.ELEMENT_DOCUMENT;   
           return SSM.ELEMENT_DOCUMENT;
        }
     }     
     return SSM.ELEMENT_NONE;
   }
   
   
   public static int checkLens(int posX, int posY) {
      float mx = posX;
      float my = posY;
      
      
      for (int i=0; i < SSM.instance().lensList.size(); i++) {
         float x = (float)mx - (float)SSM.instance().lensList.elementAt(i).magicLensX;
         float y = (float)my - (float)SSM.instance().lensList.elementAt(i).magicLensY;
         float r = (float)SSM.instance().lensList.elementAt(i).magicLensRadius;
         float d = (float)Math.sqrt(x*x + y*y);
         
         if (d <= r) {
            SSM.instance().lensList.elementAt(i).magicLensSelected = 1;
            SSM.instance().topElement = SSM.ELEMENT_LENS;
System.out.println("=============================================> Found Lens");            
            return SSM.ELEMENT_LENS;
         }
      }      
      return SSM.ELEMENT_NONE; 
   }
   
   public static int checkSlider(int posX, int posY) {
      float mx = posX;
      float my = SSM.windowHeight - posY;      
      
      float yf_anchorX = SSM.instance().getYearAnchorX();
      float yf_anchorY = SSM.instance().getYearAnchorY();
      if (DCUtil.between(mx, yf_anchorX, yf_anchorX + (CacheManager.instance().timeLineSize/12)*SSM.instance().rangeFilterWidth)) {
         if (DCUtil.between(my, yf_anchorY-15, yf_anchorY+SSM.instance().rangeFilterHeight)) {
            SSM.instance().topElement = SSM.ELEMENT_FILTER;
            return SSM.ELEMENT_FILTER;
         }
      }
      
      float mf_anchorX = SSM.instance().getMonthAnchorX();
      float mf_anchorY = SSM.instance().getMonthAnchorY();
      // Always 12 month
      if (DCUtil.between(mx, mf_anchorX, mf_anchorX + 12*SSM.instance().rangeFilterWidth)) {
         if (DCUtil.between(my, mf_anchorY-15, mf_anchorY+SSM.instance().rangeFilterHeight)) {
            SSM.instance().topElement = SSM.ELEMENT_FILTER;
            return SSM.ELEMENT_FILTER;
         }
      }      
      return SSM.ELEMENT_NONE; 
   }
   
   
   
}