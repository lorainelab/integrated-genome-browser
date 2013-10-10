package com.affymetrix.genoviz.swing;

import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.JToolBar;

/**
 *
 * @author hiralv
 */
public class DragDropToolBar extends JToolBar implements DragSourceListener, DropTargetListener, DragGestureListener {

	private final DragSource source;
	private TransferableComponent transferable;
	
	public DragDropToolBar() {
		source = new DragSource();
		setDropTarget(new DropTarget(this, this));
	}
	
	/** Drag Source Listener method start */
	public void dragEnter(DragSourceDragEvent dsde) { }
	public void dropActionChanged(DragSourceDragEvent dsde) { }
	public void dragExit(DragSourceEvent dse) { }
	public void dragOver(DragSourceDragEvent dsde) { }
	public void dragDropEnd(DragSourceDropEvent dsde) { }
	/** Drag Source Listener method end */
	
	/** Drag Target Listener method start */
	public void dragEnter(DropTargetDragEvent dtde) { }
	public void dropActionChanged(DropTargetDragEvent dtde) { }
	public void dragExit(DropTargetEvent dte) { }
	
	@Override
	public void dragOver(DropTargetDragEvent dtde) { 	
		moveTo(dtde.getLocation());
	}
	
	@Override
	public void drop(DropTargetDropEvent dtde) {
		dtde.acceptDrop(dtde.getDropAction());
		dtde.dropComplete(true);
		moveTo(dtde.getLocation());
		transferable = null;
	}
	/** Drag Target Listener method end */
	
	/** Drag Gesture Listener method start */
	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		transferable = new TransferableComponent(dge.getComponent());
		Image image = getImage(dge.getComponent());
		Point imageOffset = image == null ? null : new Point(dge.getDragOrigin().x - image.getWidth(null), dge.getDragOrigin().y - image.getHeight(null));
		source.startDrag(dge, DragSource.DefaultMoveDrop, image, imageOffset, transferable, this);
	}
	/** Drag Gesture Listener method end */
	
	@Override
	public Component add(Component comp, int index) {
		source.createDefaultDragGestureRecognizer(comp, DnDConstants.ACTION_MOVE, this);
		return super.add(comp, index);
    }
	
	private void moveTo(Point point){
		Component target = this.getComponentAt(point);
		if(target == this) {
			return;
		}
		
		int source_index = 0;
		while (source_index < getComponentCount()-1 && transferable.comp != getComponent(source_index)) {
			source_index++;
		}
		int target_index = 0;
		while (target_index < getComponentCount()-1 && target != getComponent(target_index)) {
			target_index++;
		}
		
		int diff = target_index - source_index;
		
		if(diff == 0) {
			return;
		}
		
		int index = source_index + diff;
		this.remove(transferable.comp);
		super.add(transferable.comp, index);
		
		validate();
	}
	

	private static Image getImage(Component comp) {
		BufferedImage image = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_ARGB);
		comp.paintAll(image.getGraphics());
		return image;
	}
	
	class TransferableComponent implements Transferable {

		public DataFlavor COMPONENT_FLAVOR = new DataFlavor(Component.class, "Component");
		DataFlavor flavors[] = {COMPONENT_FLAVOR};
		Component comp;

		public TransferableComponent(Component comp) {
			this.comp = comp;
		}

		public synchronized DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return (flavor == COMPONENT_FLAVOR);
		}

		public synchronized Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(flavor)) {
				// return (Object) comp; // This throws NotSerializableError for random classes
				return null;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}
	}
}
