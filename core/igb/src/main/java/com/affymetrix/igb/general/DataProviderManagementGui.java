package com.affymetrix.igb.general;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.util.LoadUtils;
import com.affymetrix.genoviz.swing.ButtonTableCellEditor;
import com.affymetrix.genoviz.swing.LabelTableCellRenderer;
import com.affymetrix.igb.action.AutoLoadFeatureAction;
import com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Enabled;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Name;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Refresh;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Type;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.URL;
import com.affymetrix.igb.prefs.AddDataProvider;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.jide.StyledJTable;
import com.affymetrix.igb.util.IGBAuthenticator;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.Authenticator.RequestorType;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@aQute.bnd.annotation.component.Component(name = DataProviderManagementGui.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class DataProviderManagementGui extends JRPJPanel implements PreferencesPanelProvider {

    public static final String COMPONENT_NAME = "DataProviderManagementGui";
    private static final Logger logger = LoggerFactory.getLogger(DataProviderManagementGui.class);
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("igb");
    public static final String TAB_NAME = BUNDLE.getString("dataSourceTabName");
    private static final int TAB_POSITION = 4;
    private static final Icon REFRESH_ICON = CommonUtils.getInstance().getIcon("16x16/actions/refresh.png");
    private static final String SERVER_CREDENTIALS = BUNDLE.getString("serverCredentials");
    private final String EDIT_DATA_SOURCE = BUNDLE.getString("editDataSource");
    private final String ADD_DATA_SOURCE = BUNDLE.getString("addDataSource");
    private DataProviderManager dataProviderManager;
    private final StyledJTable dataSourcesTable;
    private DataProviderTableModel dataProviderTableModel;
    private final JPanel dataSourcesPanel;
    private final JPanel synonymsPanel;
    private final JPanel cachePanel;
    private JButton loadPriorityUpBtn;
    private JButton loadPriorityDownBtn;
    private JButton removeBtn;
    private JButton authBtn;
    private JButton editBtn;
    private JButton addBtn;
    private AddDataProvider addDataProvider;

    public DataProviderManagementGui() {
        super(COMPONENT_NAME);
        setLayout(new MigLayout("fill"));
        dataSourcesTable = getStyledJTable();
        dataSourcesPanel = initilizeDataSourcesPanel();
        synonymsPanel = new SynonymsControlPanel(this).getPanel();
        cachePanel = CacheControlPanel.getCachePanel();
    }

    @Activate
    public void activate() {
        dataSourcesTable.setModel(dataProviderTableModel);
        initializeTable();
        initilizeLayout();
    }

    private StyledJTable getStyledJTable() {
        StyledJTable table = new StyledJTable() {

            @Override
            public Component prepareRenderer(
                    TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                JComponent jc = (JComponent) c;
                final DataProvider selectedDataProvider = dataProviderTableModel.getElementAt(row);
                if (selectedDataProvider.getStatus() == LoadUtils.ResourceStatus.NotResponding) {
                    jc.setToolTipText(BUNDLE.getString("siteNotResponding"));
                    c.setBackground(new Color(204, 102, 119));
                    c.setForeground(Color.WHITE);
                } else if (selectedDataProvider.useMirrorUrl()) {
                    jc.setToolTipText(BUNDLE.getString("mirrorUrlMessage"));
                    //c.setBackground(new Color(227,201,89));
                    c.setBackground(new Color(237,227,85));
                } else {
                    jc.setToolTipText("");
                }
                return c;
            }
        };
        return table;
    }

    private void initilizeLayout() {
        add(dataSourcesPanel, "grow, wrap");
        add(synonymsPanel, "grow, wrap");
        add(cachePanel, "grow");
    }

    private JPanel initilizeDataSourcesPanel() {
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[grow][grow]"));
        panel.setBorder(BorderFactory.createTitledBorder("Data Sources"));
        JScrollPane sourcesScrollPane = new JScrollPane(dataSourcesTable);
        initLoadPriorityUpBtn();
        initLoadPriorityDownBtn();
        initAddBtn();
        initEditBtn();
        initAuthBtn();
        initRemoveBtn();
        JCheckBox autoload = AutoLoadFeatureAction.getActionCB();
        JPanel btnPanel = new JPanel(new MigLayout());
        btnPanel.add(autoload, "span6, right, wrap");
        btnPanel.add(loadPriorityUpBtn, "right rel");
        btnPanel.add(loadPriorityDownBtn, "right rel");
        btnPanel.add(addBtn, "right rel");
        btnPanel.add(editBtn, "right rel");
        btnPanel.add(authBtn, "right rel");
        btnPanel.add(removeBtn, "right");
        panel.add(sourcesScrollPane, "grow, wrap");
        panel.add(btnPanel, "right");
        return panel;
    }

    private void initLoadPriorityUpBtn() {
        ImageIcon up_icon = MenuUtil.getIcon("16x16/actions/up.png");
        loadPriorityUpBtn = new JButton(up_icon);
        loadPriorityUpBtn.setToolTipText(BUNDLE.getString("increaseLoadPriority"));
        loadPriorityUpBtn.addActionListener(e -> {
            dataSourcesTable.stopCellEditing();
            int row = dataSourcesTable.getSelectedRow();
            if (row >= 1 && row < dataSourcesTable.getRowCount()) {
                final DataProvider selectedDataProvider = dataProviderTableModel.getElementAt(row);
                final DataProvider dataProviderForSwap = dataProviderTableModel.getElementAt(row - 1);
                int loadPriority = selectedDataProvider.getLoadPriority();
                int loadPrioritySwap = dataProviderForSwap.getLoadPriority();
                selectedDataProvider.setLoadPriority(loadPrioritySwap);
                dataProviderForSwap.setLoadPriority(loadPriority);
                dataProviderTableModel.sortDataSources();
                dataSourcesTable.getSelectionModel().setSelectionInterval(row - 1, row - 1);
            }
        });
        loadPriorityUpBtn.setEnabled(false);
    }

    private void initLoadPriorityDownBtn() {
        ImageIcon downIcon = MenuUtil.getIcon("16x16/actions/down.png");
        loadPriorityDownBtn = new JButton(downIcon);
        loadPriorityDownBtn.setToolTipText(BUNDLE.getString("decreaseSeqServerPriority"));
        loadPriorityDownBtn.addActionListener(e -> {
            dataSourcesTable.stopCellEditing();
            int row = dataSourcesTable.getSelectedRow();
            if (row >= 0 && row < dataSourcesTable.getRowCount() - 1) {
                final DataProvider selectedDataProvider = dataProviderTableModel.getElementAt(row);
                final DataProvider dataProviderForSwap = dataProviderTableModel.getElementAt(row + 1);
                int loadPriority = selectedDataProvider.getLoadPriority();
                int loadPrioritySwap = dataProviderForSwap.getLoadPriority();
                selectedDataProvider.setLoadPriority(loadPrioritySwap);
                dataProviderForSwap.setLoadPriority(loadPriority);
                dataProviderTableModel.sortDataSources();
                dataSourcesTable.getSelectionModel().setSelectionInterval(row + 1, row + 1);
            }
        });
        loadPriorityDownBtn.setEnabled(false);
    }

    private void initRemoveBtn() {
        removeBtn = new JButton(BUNDLE.getString("removeButton"));
        removeBtn.addActionListener((ActionEvent e) -> {
            dataSourcesTable.stopCellEditing();
            int row = dataSourcesTable.getSelectedRow();
            final DataProvider selectedDataProvider = dataProviderTableModel.getElementAt(row);
            dataProviderManager.removeDataProvider(selectedDataProvider);
        });
        removeBtn.setEnabled(false);
    }

    private void initAuthBtn() {
        authBtn = new JButton(BUNDLE.getString("authenticateButton"));
        authBtn.addActionListener((ActionEvent e) -> {
            handleEditAuthenticationEvent();
        });
        authBtn.setEnabled(false);
    }

    private void initEditBtn() {
        editBtn = new JButton(BUNDLE.getString("editButton"));
        editBtn.addActionListener((ActionEvent e) -> {
            dataSourcesTable.stopCellEditing();
            int row = dataSourcesTable.getSelectedRow();
            final DataProvider selectedDataProvider = dataProviderTableModel.getElementAt(row);
            addDataProvider.init(true, EDIT_DATA_SOURCE, selectedDataProvider);
        });
        editBtn.setEnabled(false);
    }

    private void initAddBtn() {
        addBtn = new JButton(BUNDLE.getString("addButton"));
        addBtn.addActionListener((ActionEvent e) -> {
            dataSourcesTable.stopCellEditing();
            int row = dataSourcesTable.getSelectedRow();
            addDataProvider.init(false, ADD_DATA_SOURCE, null);
        });
    }

    private void initializeTable() {
        JTableHeader header = dataSourcesTable.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer(dataSourcesTable));
        initializeDefaultCellRenderer();
        Collections.list(dataSourcesTable.getColumnModel().getColumns()).forEach(column -> {
            DataProviderTableColumn current = DataProviderTableColumn.valueOf((String) column.getHeaderValue());
            switch (current) {
                case Refresh:
                    column.setMaxWidth(20);
                    column.setCellRenderer(getRefreshRenderer());
                    column.setCellEditor(new ButtonTableCellEditor(REFRESH_ICON));
                    break;
                case Name:
                    column.setPreferredWidth(100);
                    break;
                case URL:
                    column.setPreferredWidth(310);
                    break;
                case Enabled:
                    column.setPreferredWidth(25);
                    break;
                case Type:
                    column.setPreferredWidth(40);
                    break;
            }
        });
        dataSourcesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataSourcesTable.setCellSelectionEnabled(false);
        dataSourcesTable.setRowSelectionAllowed(true);
        dataSourcesTable.getSelectionModel().addListSelectionListener(this::processListSelectionEvent);
    }

    private void processListSelectionEvent(ListSelectionEvent event) {
        loadPriorityUpBtn.setEnabled(dataSourcesTable.getSelectedRow() > 0);
        loadPriorityDownBtn.setEnabled(dataSourcesTable.getSelectedRow() < dataSourcesTable.getRowCount() - 1);
        authBtn.setEnabled(dataSourcesTable.getSelectedRowCount() == 1);
        editBtn.setEnabled(dataSourcesTable.getSelectedRowCount() == 1 && dataSourcesTable.isCellEditable(dataSourcesTable.getSelectedRow(), 1));
        removeBtn.setEnabled(dataSourcesTable.getSelectedRowCount() == 1);
    }

    private TableCellRenderer getRefreshRenderer() {
        TableCellRenderer refreshRenderer = new LabelTableCellRenderer(REFRESH_ICON, true) {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                int modelRow = table.convertRowIndexToModel(row);
                int enabledColumnIndex = dataProviderTableModel.getColumnIndex(DataProviderTableModel.DataProviderTableColumn.Enabled);
                this.setEnabled((Boolean) dataProviderTableModel.getValueAt(modelRow, enabledColumnIndex));
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            }
        };
        return refreshRenderer;
    }

    private void initializeDefaultCellRenderer() {
        TableCellRenderer renderer = new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                int modelRow = table.convertRowIndexToModel(row);
                int enabledColumnIndex = dataProviderTableModel.getColumnIndex(DataProviderTableModel.DataProviderTableColumn.Enabled);
                this.setEnabled((Boolean) dataProviderTableModel.getValueAt(modelRow, enabledColumnIndex));
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            }
        };
        dataSourcesTable.setDefaultRenderer(String.class, renderer);
    }

    @Reference
    public void setDataProviderManager(DataProviderManager dataProviderManager) {
        this.dataProviderManager = dataProviderManager;
    }

    @Reference
    public void setDataProviderTableModel(DataProviderTableModel dataProviderTableModel) {
        this.dataProviderTableModel = dataProviderTableModel;
    }

    @Reference
    public void setAddDataProvider(AddDataProvider addDataProvider) {
        this.addDataProvider = addDataProvider;
    }

    private void handleEditAuthenticationEvent() {
        dataSourcesTable.stopCellEditing();
        DataProvider selectedDataProvider = dataProviderTableModel.getElementAt(dataSourcesTable.getSelectedRow());
        try {
            URL u = new URL(selectedDataProvider.getUrl());
            IGBAuthenticator.resetAuthentication(selectedDataProvider);
            PasswordAuthentication pa = IGBAuthenticator.requestPasswordAuthentication(
                    u.getHost(),
                    null,
                    u.getPort(),
                    u.getProtocol(),
                    SERVER_CREDENTIALS,
                    null,
                    u,
                    RequestorType.SERVER);
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public String getName() {
        return TAB_NAME;
    }

    @Override
    public JRPJPanel getPanel() {
        return this;
    }

    @Override
    public int getWeight() {
        return TAB_POSITION;
    }

    @Override
    public void refresh() {
        //do nothing
    }

    private static class HeaderRenderer implements TableCellRenderer {

        DefaultTableCellRenderer renderer;
        DefaultTableCellRenderer iconRenderer;

        public HeaderRenderer(JTable dataSourcesTable) {
            renderer = (DefaultTableCellRenderer) dataSourcesTable.getTableHeader().getDefaultRenderer();
            iconRenderer = new DefaultTableCellRenderer();
            iconRenderer.setIcon(REFRESH_ICON);
            iconRenderer.setHorizontalAlignment(JLabel.CENTER);
            renderer.setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (col == 0) {
                return iconRenderer;
            }
            return renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        }
    }

}
