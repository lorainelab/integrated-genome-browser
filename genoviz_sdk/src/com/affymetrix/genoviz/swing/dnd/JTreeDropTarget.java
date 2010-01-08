package com.affymetrix.genoviz.swing.dnd;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.tree.*;

/**
 * An abstract implementation of DropTargetListener suitable for use with
 * a JTree.  To use this, you must supply an implementation of dropImpl()
 * and initialize with a JTree where isEnabled() is true.  
 * 
 * This implementation does not require that
 * you also set JTree.setEditable(true).
 *
 * Original version derived from Kim Topley "Core Swing: Advanced Programming",
 * Prentice-Hall, 1999.
 *
 * @author  ed
 */
public abstract class JTreeDropTarget implements DropTargetListener, PropertyChangeListener {
  /** Keeps track of the value set in checkTransferType() */
  private boolean acceptable_type = false;

  protected JTree tree;
  private TreePath[] selections;
  private TreePath lead_selection;
  private DropTarget dropTarget;
  private DataFlavor[] my_flavors = null;
  
  /** Set this to true if you want acceptOrRejectDrag() to send 
   *  acceptDrag(DnDConstants.ACTION_COPY) rather than rejectDrag()
   *  in cases where the DragSource is offering a drag that is NOT 
   *  a COPY or MOVE type.  I know of no benefit to doing so.
   *  The only expected ill effect is that certain versions of the 
   *  JRE (like Windows) may not give the correct visual feedback.
   */
  private static final boolean PROVISIONAL_ACCEPT = false;
  private final static boolean DEBUG = false;

  /** This constructor provided for sub-classing.
   *  Must call setTree() and setFlavors() after using this.
   */
  protected JTreeDropTarget() {
  }

  /** Sets the JTree that will be used.  Drop action will be turned on
   *  ONLY if the JTree is enabled.
   */
  public void setTree(JTree tree) {
    if (this.tree != null) {
      this.tree.removePropertyChangeListener(this);
    }
    this.tree = tree;
    dropTarget = new DropTarget(tree, DnDConstants.ACTION_COPY_OR_MOVE, 
      this, tree.isEnabled(), null);
    tree.addPropertyChangeListener(this);
  }
  
  /** Sets the data flavors that you want to be allowed to drag into the JTree. */
  public void setFlavors(DataFlavor[] flavors) {
    this.my_flavors = flavors;
  }
  
  public void dragEnter(DropTargetDragEvent dtde) {
    if (DEBUG) System.out.println("DropEnter: "+dtde);
    saveTreeSelection();
    checkTransferType(dtde);
    boolean acceptedDrag = acceptOrRejectDrag(dtde);
    dragUnderFeedback(dtde, acceptedDrag);
  }

  public void dragExit(DropTargetEvent dte) {
    if (DEBUG) System.out.println("DropExit: "+dte);
    dragUnderFeedback(null, false);
    restoreTreeSelection();
  }

  public void dragOver(DropTargetDragEvent dtde) {
    boolean acceptedDrag = acceptOrRejectDrag(dtde);
    dragUnderFeedback(dtde, acceptedDrag);
  }

  public void dropActionChanged(DropTargetDragEvent dtde) {
    boolean acceptedDrag = acceptOrRejectDrag(dtde);
    dragUnderFeedback(dtde, acceptedDrag);
  }

  protected void checkTransferType(DropTargetDragEvent dtde) {
    acceptable_type = false;
    if (my_flavors == null) {return;}
    for (int i=0; i<my_flavors.length; i++) {
      if (dtde.isDataFlavorSupported(my_flavors[i])) {
        acceptable_type = true;
        break;
      }
    }
    if (DEBUG) System.out.println("Data type acceptable - " + acceptable_type);
  }

  protected void saveTreeSelection() {
    selections = tree.getSelectionPaths();
    lead_selection = tree.getLeadSelectionPath();
    //tree.clearSelection();
  }

  protected void restoreTreeSelection() {
    tree.setSelectionPaths(selections);
    if (lead_selection != null) {
      tree.removeSelectionPath(lead_selection);
      tree.addSelectionPath(lead_selection);
    }
  }

  /** This default implementation returns true if path is not null.
   *  Called from {@link #isAcceptableDropLocation(DropTargetDragEvent)}.
   */
  protected boolean isAcceptableDropPath(TreePath path) {
    return (path != null);
  }
  
  /** This default implementation returns the result of {@link #isAcceptableDropPath(TreePath)}
   *  based on the path in the tree at the Point DropTargetDragEvent#getLocation().
   */
  protected boolean isAcceptableDropLocation(DropTargetDragEvent dtde) {
    Point location = dtde.getLocation();
    TreePath path = tree.getClosestPathForLocation(location.x, location.y);
    return isAcceptableDropPath(path);
  }

  /**
   *  Called by {@link #dragUnderFeedback(DropTargetDragEvent, boolean)} to
   *  visually indicate that "row" is selected.
   *  Default implementation calls tree.setSelectionRow(row) except
   *  when row == -1, when tree.clearSelection() is called instead.
   *  This default implementation is OK, but if you want to give more
   *  refined feedback, or want to avoid triggering selection events,
   *  override this method.  Note that when overriding, you should assume
   *  that row == -1 indicates that feedback should be cleared.
   */
  protected void dragUnderFeedback(int row) {
    if (row == -1) {
      tree.clearSelection();
    } else {
      tree.setSelectionRow(row);
    }
  }
  
  protected void dragUnderFeedback(DropTargetDragEvent dtde, boolean acceptedDrag) {
    if (dtde != null && acceptedDrag) {
      Point location = dtde.getLocation();
      if (isAcceptableDropLocation(dtde)) {
        dragUnderFeedback(tree.getClosestRowForLocation(location.x, location.y));
      } else {
        dragUnderFeedback(-1);
      }
    } else {
        dragUnderFeedback(-1);
    }
  }

  protected boolean acceptOrRejectDrag(DropTargetDragEvent dtde) {
    int dropAction = dtde.getDropAction();
    int sourceActions = dtde.getSourceActions();
    boolean acceptedDrag = false;
                //System.out.println("AcceptOrReject: "+dtde);

    boolean acceptableDropLocation = isAcceptableDropLocation(dtde);

    // Reject if the object being transferred 
    // or the operations available are not acceptable.
    if (!acceptable_type || 
      (sourceActions & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
      if (DEBUG) System.out.println("Drop target rejecting drag");
      dtde.rejectDrag();
    /*
    } else if (!tree.isEditable()) {
      // Can't drag to a read-only JTree
      if (DEBUG) System.out.println("Drop target rejecting drag because tree isn't editable");      
      dtde.rejectDrag();
    */
    } else if (!acceptableDropLocation) {
      // Can only drag to writable directory
      if (DEBUG) System.out.println("Drop target rejecting drag due to location");
      dtde.rejectDrag();
    } else if ((dropAction & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
      // If the source is not offering "copy" or "move", we provisionally
      // accept the drop while suggesting "copy" as the type, or 
      // we could simply reject it.  Rejecting it is perhaps better supported
      // on multiple platforms.
      if (PROVISIONAL_ACCEPT) {
        if (DEBUG) System.out.println("Drop target offering COPY");
        dtde.acceptDrag(DnDConstants.ACTION_COPY);
        acceptedDrag = true;
      } else {
        if (DEBUG) System.out.println("Drop target rejecting drag because not \"Copy\" or \"Move\" type");
        dtde.rejectDrag();
        acceptedDrag = false;
      }
    } else {
      // Offering an acceptable operation: accept
      if (DEBUG) System.out.println("Drop target accepting drag");
      dtde.acceptDrag(dropAction);
      acceptedDrag = true;
    }

    return acceptedDrag;
  }

   /** A wrapper around dropImpl() which actually performs the drop.
    *  The code provided here deals with giving visual feedback,
    *  such as changing the selection in the JTree and changing the
    *  Cursor during the operation.  Although this code can recover
    *  from any Exception thrown by dropImpl(), you should try to
    *  catch any that are expected.
    */
   public void drop(DropTargetDropEvent dtde) {
    if (DEBUG) System.out.println("JTreeDropTarget.drop() entered: "+dtde);

    // Check the drop action
    if ((dtde.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0) {
      // Accept the drop and get the transfer data
      dtde.acceptDrop(dtde.getDropAction());
      //Transferable transferable = dtde.getTransferable();

      boolean dropSucceeded = false;

      try {
        tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Save the user's selections
        saveTreeSelection();

        dropSucceeded = dropImpl(dtde);

        if (DEBUG) System.out.println("Drop completed, success: "  + dropSucceeded);
      } catch (Exception e) {
        tree.setCursor(Cursor.getDefaultCursor());
        System.out.println("Exception while handling drop:  " + e);
        e.printStackTrace();
      } finally {
        tree.setCursor(Cursor.getDefaultCursor());

        // Restore the user's selections
        dragUnderFeedback(null, false);
        restoreTreeSelection();
        dtde.dropComplete(dropSucceeded);
      }
    } else {
      if (DEBUG) System.out.println("Drop target rejected drop");
      dtde.dropComplete(false);
    }
  }
  
  /** Subclasses must override this to perform the actual drop. */
  protected abstract boolean dropImpl(DropTargetDropEvent dtde);
   
 /** Enables or disables the drop target when the underlying tree becomes
  *  enabled or disabled.
  */
  public void propertyChange(java.beans.PropertyChangeEvent evt) {
    if (DEBUG) System.out.println("JTreeDropTarget got Prop change: "+evt.getPropertyName());
    if (evt.getSource()==tree && evt.getPropertyName().equals("enabled") && tree != null && dropTarget != null) {
      dropTarget.setActive(tree.isEnabled());
    }
  }
}
