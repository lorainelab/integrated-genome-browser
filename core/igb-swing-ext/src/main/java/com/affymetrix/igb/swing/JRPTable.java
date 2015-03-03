package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class JRPTable extends JTable implements JRPWidget {

    private static final long serialVersionUID = 1L;
    private final String id;

    public JRPTable(String id) {
        super();
        this.id = id;
        init();
    }

    public JRPTable(String id, int numRows, int numColumns) {
        super(numRows, numColumns);
        this.id = id;
        init();
    }

    public JRPTable(String id, Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        this.id = id;
        init();
    }

    public JRPTable(String id, TableModel dm) {
        super(dm);
        this.id = id;
        init();
    }

    public JRPTable(String id, TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        this.id = id;
        init();
    }

    public JRPTable(String id, TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        this.id = id;
        init();
    }

    @SuppressWarnings("rawtypes")
    public JRPTable(String id, Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        this.id = id;
        init();
    }

    private void addSelectionListener(ListSelectionModel newModel) {
        newModel.addListSelectionListener(
                e -> {
//					RecordPlaybackHolder.getInstance().recordOperation(new Operation(id, "setValue(" + getValue() + ")"));
                }
        );
    }

    private void addColumnSelectionListener(ListSelectionModel newModel) {
        newModel.addListSelectionListener(
                e -> {
//					RecordPlaybackHolder.getInstance().recordOperation(new Operation(id, "setValue(" + getValue() + ")"));
                }
        );
    }

    private void init() {
        ScriptManager.getInstance().addWidget(this);
        if (getSelectionModel() != null) {
            addSelectionListener(getSelectionModel());
        }
        if (getColumnModel() != null) {
            TableColumnModel tableColumnModel = getColumnModel();
            if (tableColumnModel.getSelectionModel() != null) {
                addColumnSelectionListener(tableColumnModel.getSelectionModel());
            }
        }
    }

    @Override
    public void setColumnModel(TableColumnModel columnModel) {
        super.setColumnModel(columnModel);
        addColumnSelectionListener(columnModel.getSelectionModel());
    }

    @Override
    public void setSelectionModel(ListSelectionModel newModel) {
        super.setSelectionModel(newModel);
        addSelectionListener(newModel);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean consecutiveOK() {
        return false;
    }
}
