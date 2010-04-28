package com.affymetrix.genoviz.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.TransferHandler;

/**
 *
 * @author hiralv
 */
public abstract class FileDropHandler extends TransferHandler {

	@Override
	public boolean canImport(TransferSupport support) {
		return (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || support.isDataFlavorSupported(DataFlavor.stringFlavor));

	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}

		Transferable t = support.getTransferable();
		try {
			if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {

				List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				for (File f : files) {
					openFileAction(f);
				}

			} else {
				String url = (String) t.getTransferData(DataFlavor.stringFlavor);
				openURLAction(url);
			}
		} catch (UnsupportedFlavorException ex) {
			return false;
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	abstract public void openFileAction(File f);

	abstract public void openURLAction(String url);
}
