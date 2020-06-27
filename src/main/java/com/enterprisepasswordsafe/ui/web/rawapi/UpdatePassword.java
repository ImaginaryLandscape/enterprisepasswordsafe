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

package com.enterprisepasswordsafe.ui.web.rawapi;

import com.enterprisepasswordsafe.database.AccessControlDAO;
import com.enterprisepasswordsafe.database.Password;
import com.enterprisepasswordsafe.database.PasswordDAO;
import com.enterprisepasswordsafe.database.User;
import com.enterprisepasswordsafe.engine.accesscontrol.AccessControl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UpdatePassword extends RawAPIServlet {

	@Override
    protected void doPost(final HttpServletRequest request, HttpServletResponse response)
    	throws IOException {
    	try {
    		User user = super.getAndAuthenticateUser(request);
    		String passwordId = request.getParameter("id");
    		AccessControl ac = AccessControlDAO.getInstance().getAccessControl(user, passwordId);
    		Password password = PasswordDAO.getInstance().getById(passwordId, ac);

    		String newPassword = request.getParameter("newPassword");
    		if( newPassword == null ) {
    			throw new RuntimeException("The new password was not supplied");
    		}
    		password.setPassword(newPassword);
    		PasswordDAO.getInstance().update(password, user, ac);

    		response.setContentType("text/plain");
    		response.setCharacterEncoding("UTF-8");
    		PrintWriter writer = response.getWriter();
    		writer.println(password.getPassword());

    	} catch( Exception ex ) {
    		Logger.getAnonymousLogger().log(Level.WARNING, "Error during GetPassword", ex);
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    	}
    }

    @Override
	public String getServletInfo() {
        return "Raw API Servlet to find a password id from a x@y format";
    }
}
