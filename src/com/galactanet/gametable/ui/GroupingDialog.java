/*
 * DnDDialog.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.galactanet.gametable.GametableFrame;
import com.galactanet.gametable.Log;

/** **********************************************************************************************
 * DnD Dialog for quick edits of DnD Attributes
 */
public class GroupingDialog extends JDialog implements FocusListener
{
    
    private static final long serialVersionUID = 7635834193550405597L;

    private boolean         bAccepted;
    private final JButton   b_cancel    = new JButton();
    private final JButton   b_ok        = new JButton();
    
    
    private JLabel          label       = new JLabel("Select Group ");        
    private final JComboBox<String> groups = new JComboBox<String>();
    private JTextField      newGroup    = new JTextField(20);
    private JLabel          nlabel      = new JLabel("New Group "); 

    public GroupingDialog(final boolean ngrp) {    
        try {
            initialize(ngrp);
        }
        catch (final Exception e) {
            Log.log(Log.SYS, e);
        }

        // pack yourself
        pack();
        // center yourself
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension frameSize = getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        
    }

    /** **********************************************************************************************
     * 
     */
    public void focusGained(final FocusEvent e) {
        // only interested in JTextFields
        if (!(e.getSource() instanceof JTextField))
        {
            return;
        }

        final JTextField focused = (JTextField)e.getSource();
        focused.setSelectionStart(0);
        focused.setSelectionEnd(focused.getText().length());
    }

    /** **********************************************************************************************
     * 
     */
    public void focusLost(final FocusEvent e) {
    }

    /** **********************************************************************************************
     * 
     */
    private void initialize(final boolean ngrp) {
        setTitle("Select Group");
        setResizable(false);

        b_ok.setText("Ok");
        b_ok.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                bAccepted = true;
                dispose();
            }
        });

        b_cancel.setText("Cancel");
        b_cancel.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                dispose();
            }
        });

        final int PADDING = 5;

        final Box outmostBox = Box.createHorizontalBox();
        getContentPane().add(outmostBox, BorderLayout.CENTER);
        outmostBox.add(Box.createHorizontalStrut(PADDING));
        final Box outerBox = Box.createVerticalBox();
        outmostBox.add(outerBox);
        outmostBox.add(Box.createHorizontalStrut(PADDING));
        outerBox.add(Box.createVerticalStrut(PADDING));        
       
       
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if(ngrp) {
            panel.add(nlabel);
            outerBox.add(Box.createHorizontalStrut(PADDING));
            panel.add(newGroup);            
            outerBox.add(panel);
            outerBox.add(Box.createVerticalStrut(PADDING));

            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        }
        panel.add(label);
        outerBox.add(Box.createHorizontalStrut(PADDING));
        panel.add(groups);            
        outerBox.add(panel);
        outerBox.add(Box.createVerticalStrut(PADDING));
        
        panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        outerBox.add(panel);
        panel.add(b_ok);
        panel.add(Box.createHorizontalStrut(PADDING));
        panel.add(b_cancel);        
        outerBox.add(Box.createVerticalStrut(PADDING));
        
        Iterator glist = GametableFrame.getGametableFrame().getGrouping().getGroups();        
        while(glist.hasNext()) {            
            groups.addItem(glist.next().toString());
        }        
        groups.setMinimumSize(new Dimension(10,20));
        setModal(true);
    }

 
    /** **********************************************************************************************
     * 
     * @return
     */
    public boolean isAccepted() {
        return bAccepted;
    }
    
    /** **********************************************************************************************
     * 
     * @return
     */
    public String getGroup() {
      String n = newGroup.getText();
      if((n == null) || (n.length() == 0)) return (String)groups.getSelectedItem();
      return n;
    }
}
