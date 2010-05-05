package com.affymetrix.genoviz.swing;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class DragDropTree extends JTree implements DragSourceListener, DropTargetListener, DragGestureListener {

	DragSource source;
	DropTarget target;
	DragGestureRecognizer recognizer;
	TransferableTreeNode transferable;
	DefaultMutableTreeNode oldNode;

	public DragDropTree() {
		super();
		source = new DragSource();
		target = new DropTarget(this, this);
		recognizer = source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
	}

	/*
	 * Drag Gesture Handler
	 */
	public void dragGestureRecognized(DragGestureEvent dge) {
		TreePath path = getSelectionPath();
		if ((path == null) || (path.getPathCount() <= 1)) {
			// We can't move the root node or an empty selection
			return;
		}
		oldNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		transferable = new TransferableTreeNode(path);
		source.startDrag(dge, DragSource.DefaultMoveNoDrop, transferable, this);

		// If you support dropping the node anywhere, you should probably
		// start with a valid move cursor:
		//source.startDrag(dge, DragSource.DefaultMoveDrop, transferable,
		// this);
	}

	/*
	 * Drag Event Handlers
	 */
	public void dragEnter(DragSourceDragEvent dsde) {
	}

	public void dragExit(DragSourceEvent dse) {
	}

	public void dragOver(DragSourceDragEvent dsde) {
	}

	public void dropActionChanged(DragSourceDragEvent dsde) {
		System.out.println("Action: " + dsde.getDropAction());
		System.out.println("Target Action: " + dsde.getTargetActions());
		System.out.println("User Action: " + dsde.getUserAction());
	}

	public void dragDropEnd(DragSourceDropEvent dsde) {
		/*
		 * to support move or copy, we have to check which occurred:
		 */
		System.out.println("Drop Action: " + dsde.getDropAction());
		if (dsde.getDropSuccess() && (dsde.getDropAction() == DnDConstants.ACTION_MOVE)) {
			((DefaultTreeModel) getModel()).removeNodeFromParent(oldNode);
		}

	}

	/*
	 * Drop Event Handlers
	 */
	private TreeNode getNodeForEvent(DropTargetDragEvent dtde) {
		Point p = dtde.getLocation();
		DropTargetContext dtc = dtde.getDropTargetContext();
		JTree tree = (JTree) dtc.getComponent();
		TreePath path = tree.getClosestPathForLocation(p.x, p.y);
		return (TreeNode) path.getLastPathComponent();
	}

	public void dragEnter(DropTargetDragEvent dtde) {
		TreeNode node = getNodeForEvent(dtde);
		if (node.isLeaf()) {
			dtde.rejectDrag();
		} else {
			// start by supporting move operations
			//dtde.acceptDrag(DnDConstants.ACTION_MOVE);
			dtde.acceptDrag(dtde.getDropAction());
		}
	}

	public void dragOver(DropTargetDragEvent dtde) {
		TreeNode node = getNodeForEvent(dtde);
		if (node.isLeaf()) {
			dtde.rejectDrag();
		} else {
			// start by supporting move operations
			//dtde.acceptDrag(DnDConstants.ACTION_MOVE);
			dtde.acceptDrag(dtde.getDropAction());
		}
	}

	public void dragExit(DropTargetEvent dte) {
	}

	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	public void drop(DropTargetDropEvent dtde) {
		Point pt = dtde.getLocation();
		DropTargetContext dtc = dtde.getDropTargetContext();
		JTree tree = (JTree) dtc.getComponent();
		TreePath parentpath = tree.getClosestPathForLocation(pt.x, pt.y);
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) parentpath.getLastPathComponent();

		if (parent.isLeaf()) {
			dtde.rejectDrop();
			return;
		}

		try {
			Transferable tr = dtde.getTransferable();
			DataFlavor[] flavors = tr.getTransferDataFlavors();
			for (int i = 0; i < flavors.length; i++) {
				if (tr.isDataFlavorSupported(flavors[i])) {
					dtde.acceptDrop(dtde.getDropAction());
					TreePath p = (TreePath) tr.getTransferData(flavors[i]);
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) p.getLastPathComponent();
					DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
					model.insertNodeInto(node, parent, 0);
					dtde.dropComplete(true);
					return;
				}
			}
			dtde.rejectDrop();
		} catch (Exception e) {
			e.printStackTrace();
			dtde.rejectDrop();
		}
	}

	//TransferableTreeNode.java
	//A Transferable TreePath to be used with Drag & Drop applications.
	//
	class TransferableTreeNode implements Transferable {

		public DataFlavor TREE_PATH_FLAVOR = new DataFlavor(TreePath.class, "Tree Path");
		DataFlavor flavors[] = {TREE_PATH_FLAVOR};
		TreePath path;

		public TransferableTreeNode(TreePath tp) {
			path = tp;
		}

		public synchronized DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return (flavor.getRepresentationClass() == TreePath.class);
		}

		public synchronized Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(flavor)) {
				return (Object) path;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}
	}
}

