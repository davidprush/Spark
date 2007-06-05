/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Lesser Public License (LGPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.sparkimpl.plugin.alerts;

import org.jivesoftware.resource.Res;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.component.CheckNode;
import org.jivesoftware.spark.component.CheckTree;
import org.jivesoftware.spark.component.TitlePanel;
import org.jivesoftware.spark.ui.ContactGroup;
import org.jivesoftware.spark.ui.ContactItem;
import org.jivesoftware.spark.ui.ContactList;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Allows for better selective broadcasting.
 *
 * @author Derek DeMoro
 */
public class BroadcastDialog extends JPanel {

    private CheckTree checkTree;
    private CheckNode rosterNode;

    private JTextArea messageBox;

    private JRadioButton normalMessageButton;
    private JRadioButton alertMessageButton;

    private List<CheckNode> nodes = new ArrayList<CheckNode>();

    public BroadcastDialog() {
        setLayout(new GridBagLayout());

        rosterNode = new CheckNode("Roster");
        checkTree = new CheckTree(rosterNode);

        // Build out from Roster
        final ContactList contactList = SparkManager.getWorkspace().getContactList();
        for (ContactGroup group : contactList.getContactGroups()) {
            String groupName = group.getGroupName();
            if (!group.hasAvailableContacts()) {
                continue;
            }
            CheckNode groupNode = new CheckNode(groupName);
            rosterNode.add(groupNode);

            // Now add contact items from contact group.
            for (ContactItem item : group.getContactItems()) {
                CheckNode itemNode = new CheckNode(item.getNickname(), false, false);
                itemNode.setAssociatedObject(item.getJID());
                groupNode.add(itemNode);
                nodes.add(itemNode);
            }
        }

        messageBox = new JTextArea();
        normalMessageButton = new JRadioButton("Normal Message");
        alertMessageButton = new JRadioButton("Alert Notification");

        ButtonGroup group = new ButtonGroup();
        group.add(normalMessageButton);
        group.add(alertMessageButton);

        final JScrollPane pane = new JScrollPane(messageBox);
        pane.setBorder(BorderFactory.createTitledBorder("Message"));

        final JScrollPane treePane = new JScrollPane(checkTree);
        treePane.setBorder(BorderFactory.createTitledBorder("Send to these people"));

        // Add to UI
        add(pane, new GridBagConstraints(0, 0, 1, 1, 0.5, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        add(normalMessageButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(alertMessageButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 5, 2, 5), 0, 0));
        add(treePane, new GridBagConstraints(1, 0, 1, 3, 0.5, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 5, 2, 5), 0, 0));

        normalMessageButton.setSelected(true);
        checkTree.expandTree();
    }

    /**
     * Displays the broadcast dialog.
     */
    public void invokeDialog() {
        final JOptionPane pane;
        final JDialog dlg;

        TitlePanel titlePanel;

        // Create the title panel for this dialog
        titlePanel = new TitlePanel(Res.getString("title.broadcast.message"), Res.getString("message.enter.message.to.broadcast"), null, true);

        // Construct main panel w/ layout.
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // The user should only be able to close this dialog.
        Object[] options = {Res.getString("ok"), Res.getString("close")};
        pane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);

        mainPanel.add(pane, BorderLayout.CENTER);

        JOptionPane p = new JOptionPane();
        dlg = p.createDialog(SparkManager.getMainWindow(), "Broadcast");
        dlg.setModal(true);

        dlg.pack();
        dlg.setSize(600, 400);
        dlg.setResizable(true);
        dlg.setContentPane(mainPanel);
        dlg.setLocationRelativeTo(SparkManager.getMainWindow());

        PropertyChangeListener changeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String value = (String)pane.getValue();
                if (Res.getString("close").equals(value)) {
                    dlg.setVisible(false);
                }
                else {
                    dlg.setVisible(false);
                    sendBroadcasts();
                }
            }
        };

        pane.addPropertyChangeListener(changeListener);

        dlg.setVisible(true);
        dlg.toFront();
        dlg.requestFocus();
    }

    /**
     * Sends a broadcast message to all users selected.
     */
    private void sendBroadcasts() {
        final Set<String> jids = new HashSet<String>();

        for (CheckNode node : nodes) {
            if (node.isSelected()) {
                String jid = (String)node.getAssociatedObject();
                jids.add(jid);
            }
        }

        String text = messageBox.getText();
        for (String jid : jids) {
            final Message message = new Message();
            message.setTo(jid);
            message.setBody(text);

            if (normalMessageButton.isSelected()) {
                message.setType(Message.Type.normal);
            }
            else {
                message.setType(Message.Type.headline);
            }
            SparkManager.getConnection().sendPacket(message);
        }
    }
}