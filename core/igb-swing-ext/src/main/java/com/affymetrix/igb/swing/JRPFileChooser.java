package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class JRPFileChooser extends JFileChooser implements JRPWidget {

    private static final long serialVersionUID = 1L;
    private final String id;

    public JRPFileChooser(String id) {
        super();
        this.id = id;
        init();
    }

    public JRPFileChooser(String id, File currentDirectory) {
        super(currentDirectory);
        this.id = id;
        init();
    }

    public JRPFileChooser(String id, File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);
        this.id = id;
        init();
    }

    public JRPFileChooser(String id, FileSystemView fsv) {
        super(fsv);
        this.id = id;
        init();
    }

    public JRPFileChooser(String id, String currentDirectoryPath) {
        super(currentDirectoryPath);
        this.id = id;
        init();
    }

    public JRPFileChooser(String id, String currentDirectoryPath, FileSystemView fsv) {
        super(currentDirectoryPath, fsv);
        this.id = id;
        init();
    }

    private void init() {
        ScriptManager.getInstance().addWidget(this);
        addActionListener(e -> {
            if (JFileChooser.CANCEL_SELECTION.equals(e.getActionCommand())) {
                ScriptManager.getInstance().recordOperation(new Operation(JRPFileChooser.this, "cancelSelection()"));
                return;
            }
            if (!JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                return;
            }
            ScriptManager.getInstance().recordOperation(new Operation(JRPFileChooser.this, "setCurrentDirectory(\"" + getCurrentDirectory() + "\")"));
            if (getSelectedFiles() == null || getSelectedFiles().length == 0) {
                if (getSelectedFile() == null) {
                } else {
                    ScriptManager.getInstance().recordOperation(new Operation(JRPFileChooser.this, "setSelectedFiles(\"" + getSelectedFile() + "\")"));
                }
            } else {
                StringBuilder selectedFiles = new StringBuilder();
                selectedFiles.append("new File[]{");
                boolean first = true;
                for (File file : getSelectedFiles()) {
                    if (first) {
                        first = false;
                    } else {
                        selectedFiles.append(",");
                    }
                    selectedFiles.append("new File(\"").append(file.getAbsolutePath()).append("\")");
                }
                selectedFiles.append("}");
                ScriptManager.getInstance().recordOperation(new Operation(JRPFileChooser.this, "setSelectedFiles(\"" + selectedFiles.toString() + "\")"));
            }
            ScriptManager.getInstance().recordOperation(new Operation(JRPFileChooser.this, "approveSelection()"));
        });
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean consecutiveOK() {
        return true;
    }
}
