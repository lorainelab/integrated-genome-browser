package com.affymetrix.genoviz.swing;

import com.affymetrix.genoviz.util.ErrorHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * Ref: http://stackoverflow.com/questions/4588109/drag-and-drop-nodes-in-jtree
 */
public class TreeTransferHandler extends TransferHandler {
    private DataFlavor nodesFlavor;
    private DataFlavor[] flavors = new DataFlavor[1];
 
    public TreeTransferHandler() {
        try {
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                              ";class=\"" +
                javax.swing.tree.DefaultMutableTreeNode[].class.getName() +
                              "\"";
            nodesFlavor = new DataFlavor(mimeType);
            flavors[0] = nodesFlavor;
        } catch(ClassNotFoundException e) {
            System.out.println("ClassNotFound: " + e.getMessage());
        }
    }

	@Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if(!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(true);
        if(!support.isDataFlavorSupported(nodesFlavor)) {
            return false;
        }
        // Do not allow a drop on the drag source selections.
        JTree.DropLocation dl =
                (JTree.DropLocation)support.getDropLocation();
        JTree tree = (JTree)support.getComponent();
        int dropRow = tree.getRowForPath(dl.getPath());
        int[] selRows = tree.getSelectionRows();
        for(int i = 0; i < selRows.length; i++) {
            if(selRows[i] == dropRow) {
                return false;
            }
        }
//        // Do not allow MOVE-action drops if a non-leaf node is
//        // selected unless all of its children are also selected.
//        int action = support.getDropAction();
//        if(action == MOVE) {
//            return haveCompleteNode(tree);
//        }
//        // Do not allow a non-leaf node to be copied to a level
//        // which is less than its source level.
//        TreePath dest = dl.getPath();
//        DefaultMutableTreeNode target =
//            (DefaultMutableTreeNode)dest.getLastPathComponent();
//        TreePath path = tree.getPathForRow(selRows[0]);
//        DefaultMutableTreeNode firstNode =
//            (DefaultMutableTreeNode)path.getLastPathComponent();
//        if(firstNode.getChildCount() > 0 &&
//               target.getLevel() < firstNode.getLevel()) {
//            return false;
//        }
        return true;
    }

    private boolean haveCompleteNode(JTree tree) {
        int[] selRows = tree.getSelectionRows();
        TreePath path = tree.getPathForRow(selRows[0]);
        DefaultMutableTreeNode first =
            (DefaultMutableTreeNode)path.getLastPathComponent();
        int childCount = first.getChildCount();
        // first has children and no children are selected.
        if(childCount > 0 && selRows.length == 1) {
			return false;
		}
        // first may have children.
        for(int i = 0; i < selRows.length; i++) {
            path = tree.getPathForRow(selRows[i]);
            DefaultMutableTreeNode next =
                (DefaultMutableTreeNode)path.getLastPathComponent();
            if(first.isNodeChild(next)) {
                // Found a child of first.
                if(childCount > selRows.length-1) {
                    // Not all children of first are selected.
                    return false;
                }
            }
        }
        return true;
    }

	@Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree)c;
        TreePath[] paths = tree.getSelectionPaths();
        if(paths != null) {
            return new NodesTransferable(tree.getSelectionPaths());
        }
        return null;
    }

	@Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if(!canImport(support)) {
            return false;
        }
        // Extract transfer data.
        TreePath[] selectedPaths = null;
		TreePath selectedPath;
		
		
        try {
            Transferable t = support.getTransferable();
            selectedPaths = (TreePath[])t.getTransferData(nodesFlavor);
        } catch(UnsupportedFlavorException ufe) {
            System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch(java.io.IOException ioe) {
            System.out.println("I/O error: " + ioe.getMessage());
        }
        // Get drop location info.
        JTree.DropLocation dl =
                (JTree.DropLocation)support.getDropLocation();
        JTree tree = (JTree)support.getComponent();
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
  
		
		TreePath parentPath = dl.getPath();
		TreePath rootPath = tree.getPathForRow(0);
		
		TreePath[] newPaths = new TreePath[selectedPaths.length];
		TreePath newPath;

		int parentRow = tree.getRowForPath(parentPath);
		int selectedRow;
		
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
		DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) rootPath.getLastPathComponent();
		DefaultMutableTreeNode dragNode;
		
        for (int j = selectedPaths.length; j > 0; j--) {
			selectedPath = selectedPaths[j - 1];
			selectedRow = tree.getRowForPath(selectedPath);

			dragNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();

			if (parentNode.isNodeAncestor(dragNode) && !parentNode.isLeaf()) {
				ErrorHandler.errorPanel("NOTICE", "The folder \""
						+ dragNode.getUserObject().toString()
						+ "\" can't be moved into one of its subfolders.");
				return false;
			}

			model.removeNodeFromParent(dragNode);

			if (parentNode.isLeaf()) {
				MutableTreeNode actualparent = (MutableTreeNode) parentNode.getParent();
				if (actualparent != null) {
					int index = model.getIndexOfChild(actualparent, parentNode);
					model.insertNodeInto(dragNode, actualparent, index + 1);
				} else {
					model.insertNodeInto(dragNode, rootNode, rootNode.getChildCount());
				}
			} else if (parentRow == selectedRow) {
				model.insertNodeInto(dragNode, rootNode, rootNode.getChildCount());
			} else if (dl.getChildIndex() > 0) {
				// Special condition when child is moved to last position.
				if(dl.getChildIndex() >= parentNode.getChildCount()){
					model.insertNodeInto(dragNode, parentNode, dl.getChildIndex() - 1);
				} else {
					model.insertNodeInto(dragNode, parentNode, dl.getChildIndex());
				}
			} else {
				model.insertNodeInto(dragNode, parentNode, 0);
			}

			newPath = new TreePath(model.getPathToRoot(dragNode));
			newPaths[j - 1] = newPath;
			
			tree.setSelectionPaths(newPaths);
		}
        return true;
    }

	@Override
	public int getSourceActions(JComponent c) {
        return MOVE;
    }
	
	@Override
    public String toString() {
        return getClass().getName();
    }

    public class NodesTransferable implements Transferable {
		TreePath[] paths;

        public NodesTransferable(TreePath[] paths) {
            this.paths = paths;
         }

        public Object getTransferData(DataFlavor flavor)
                                 throws UnsupportedFlavorException {
            if(!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
            return paths;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return nodesFlavor.equals(flavor);
        }
	}
}
