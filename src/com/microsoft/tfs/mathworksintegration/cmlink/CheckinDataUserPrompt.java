// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.mathworks.cmlink.api.ConfigurationManagementException;

/**
 * Implementation of the {@link ICheckinDataProvider} interface which
 * prompts the user for checkin information.
 */
public class CheckinDataUserPrompt implements ICheckinDataProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckinData getData() throws ConfigurationManagementException {
        // TODO: Investigate integrating with TEE UIs.
        JLabel workItemsLabel = new JLabel("WorkItem Ids (use a comma to separate multiple Ids):");
        JTextField workItemsText = new JTextField();
        JLabel commentLabel = new JLabel("Checkin comment:");
        JTextField commentText = new JTextField();
        JComponent[] components = new JComponent[] { workItemsLabel, workItemsText, commentLabel, commentText };

        CheckinData data;
        int result = JOptionPane.showConfirmDialog(
            null,
            components,
            "Checkin", 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE,
            null);
        if (result == JOptionPane.OK_OPTION) {
            // TODO: In a more generic solution, we should have a setting to indicate
            // whether WorkItem linking and/or comments are required on checkins.
            String comment = commentText.getText();
            if (comment == null || comment.isEmpty()) {
                throw new ConfigurationManagementException("A checkin comment must be supplied.");
            }

            String workItemIdsString = workItemsText.getText();
            if (workItemIdsString == null || workItemIdsString.isEmpty()) {
                throw new ConfigurationManagementException("A WorkItem Id must be supplied.");
            }
            String[] idStrings = workItemIdsString.split(",");
            int[] workItemIds = new int[idStrings.length];
            for (int i = 0; i < idStrings.length; i++) {
                try {
                    String trimmedIdString = idStrings[i].trim();
                    int id = Integer.parseInt(trimmedIdString);
                    workItemIds[i] = id;
                }
                catch (NumberFormatException ex) {
                    throw new ConfigurationManagementException("Supplied WorkItem Id \"" + idStrings[i] + 
                        "\" could not be converted to an integer.");
                }
            }

            data = new CheckinData(workItemIds, comment);
        }
        else
        {
            data = CheckinData.NoSubmit();
        }
        return data;
    }
}
