package com.affymetrix.igb.general;

import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.igb.swing.JRPButton;
import java.awt.Color;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class CacheControlPanel {

    private static final Logger logger = LoggerFactory.getLogger(CacheControlPanel.class);

    public static JPanel getCachePanel() {
        final JLabel usageLabel = new JLabel("Cache Behavior");
        final JLabel emptyLabel = new JLabel();
        final JLabel cacheCleared = new JLabel("Cache Cleared");
        final JComboBox cacheUsage = new JComboBox(LocalUrlCacher.CacheUsage.values());
        final JRPButton clearCache = new JRPButton("DataLoadPrefsView_clearCache", "Empty Cache");
        cacheCleared.setVisible(false);
        cacheCleared.setForeground(Color.RED);
        clearCache.addActionListener(e -> {
            logger.info("Action performed :" + Thread.currentThread().getId());
            clearCache.setEnabled(false);
            LocalUrlCacher.clearCache();
            clearCache.setEnabled(true);
            cacheCleared.setVisible(true);
            CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>("clear cache") {

                @Override
                protected Object runInBackground() {
                    System.out.println("Runnable :" + Thread.currentThread().getId());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException x) {
                    }
                    return null;
                }

                @Override
                public void finished() {
                    cacheCleared.setVisible(false);
                }
            };
            CThreadHolder.getInstance().execute(cacheCleared, worker);
        });

        cacheUsage.setSelectedItem(LocalUrlCacher.getCacheUsage(LocalUrlCacher.getPreferredCacheUsage()));
        cacheUsage.addActionListener(e -> LocalUrlCacher.setPreferredCacheUsage(((LocalUrlCacher.CacheUsage) cacheUsage.getSelectedItem()).usage));

        final JPanel cachePanel = new JPanel();
        final GroupLayout layout = new GroupLayout(cachePanel);
        cachePanel.setLayout(layout);
        cachePanel.setBorder(new TitledBorder("Cache Settings"));
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.linkSize(usageLabel, emptyLabel);

        layout.setHorizontalGroup(layout.createParallelGroup(LEADING).addGroup(layout.createSequentialGroup().addComponent(usageLabel).addComponent(cacheUsage)).addGroup(layout.createSequentialGroup().addComponent(emptyLabel).addComponent(clearCache).addComponent(cacheCleared)));

        layout.setVerticalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(BASELINE).addComponent(usageLabel).addComponent(cacheUsage)).addGroup(layout.createParallelGroup(BASELINE).addComponent(emptyLabel).addComponent(clearCache).addComponent(cacheCleared)));

        return cachePanel;
    }
}
