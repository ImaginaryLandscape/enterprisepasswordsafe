/*
 * Copyright (c) 2017 Carbon Security Ltd. <opensource@carbonsecurity.co.uk>
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.enterprisepasswordsafe.engine.database.actions.password;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;

import com.enterprisepasswordsafe.engine.database.*;
import com.enterprisepasswordsafe.engine.database.actions.PasswordAction;
import com.enterprisepasswordsafe.engine.database.derived.ExpiringAccessiblePasswords;


/**
 * Class used to filter expiring passwords.
 */

public class ExpiringAccessiblePasswordsAction
	extends ExpiringAccessiblePasswords implements PasswordAction
{
    /**
     * The default number of days before expiry when a warning is produced.
     */

    private static final int DEFAULT_PASSWORD_EXPIRY_WARNING_DAYS = 7;

    /**
     * The current date.
     */

    private final long now;

    /**
     * The date for expiry warnings.
     */

    private final long expiryWarning;

    /**
     * The user involved.
     */

    private final User user;

    /**
     * The node id for the users password id.
     */

    private String personalNodeId;

    /**
     * Constructor. Sets up the expiring and expiry dates.
     *
     * @param newConn
     *            The connection to the database.
     * @param newUser
     *            The user for whom the search is being performed.
     *
     * @throws SQLException
     *             Thrown if there is a problem accessing the database.
     * @throws GeneralSecurityException
     */
    public ExpiringAccessiblePasswordsAction(final User user)
            throws SQLException, GeneralSecurityException {
        super();

        this.user = user;

        Calendar cal = Calendar.getInstance();
        now = cal.getTimeInMillis();

        Calendar expiryCal = Calendar.getInstance();
        String warningPeriod = ConfigurationDAO.getValue(ConfigurationOption.DAYS_BEFORE_EXPIRY_TO_WARN);
        if (warningPeriod != null && warningPeriod.length() > 0) {
            try {
                expiryCal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(warningPeriod));
            } catch (NumberFormatException ex) {
            	ConfigurationDAO.getInstance().delete(ConfigurationOption.DAYS_BEFORE_EXPIRY_TO_WARN);
            }
        } else {
            expiryCal.add(Calendar.DAY_OF_MONTH, DEFAULT_PASSWORD_EXPIRY_WARNING_DAYS);
        }
        expiryWarning = expiryCal.getTimeInMillis();

        HierarchyNode personalNode = HierarchyNodeDAO.getInstance().getPersonalNodeForUser(user);
        if( personalNode != null ) {
        	personalNodeId = personalNode.getNodeId();
        }
    }

    /**
     * Analyse a specific password and handle its expiry state.
     *
     * @param testPassword
     *            The password to analyse
     *
     *
     * @throws ParseException Thrown if there is a problem parsing the expiry date.
     * @throws GeneralSecurityException Thrown if there is a problm accessing the data.
     * @throws SQLException Thrown if there is a problem accessing the database.
     * @throws UnsupportedEncodingException
     */
    @Override
	public void process(final HierarchyNode node, final Password testPassword)
        throws ParseException, GeneralSecurityException, SQLException, UnsupportedEncodingException {
        if (testPassword == null || !testPassword.expires()) {
            return;
        }

        AccessControl ac = AccessControlDAO.getInstance().getReadAccessControl(user,testPassword.getId());
        if (ac == null) {
            return;
        }

        // Check that the password is not a personal password
        HierarchyNodeDAO hnDAO = HierarchyNodeDAO.getInstance();
        String ultimateParentId = hnDAO.getNodeIDForName(testPassword.getId());
        if(personalNodeId == null) {
            while( ultimateParentId != null && !ultimateParentId.equals(HierarchyNode.ROOT_NODE_ID)) {
            	ultimateParentId = hnDAO.getParentIdById(ultimateParentId);
            }
        } else {
            while(	ultimateParentId != null
    		&&		!ultimateParentId.equals(personalNodeId)
            &&		!ultimateParentId.equals(HierarchyNode.ROOT_NODE_ID)) {
            	ultimateParentId = hnDAO.getParentIdById(ultimateParentId);
            }

        }
        if(ultimateParentId == null) {
        	return;
        }

        testPassword.decrypt(ac);
        long expiryDate = testPassword.getExpiry();
        if (expiryDate < now) {
            getExpired().add(testPassword);
        } else if (expiryDate < expiryWarning) {
            getExpiring().add(testPassword);
        }
    }

}
