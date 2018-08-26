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

package com.enterprisepasswordsafe.ui.web.servlets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.enterprisepasswordsafe.engine.database.*;
import com.enterprisepasswordsafe.engine.database.derived.HierarchyNodeChildren;
import com.enterprisepasswordsafe.ui.web.servlets.authorisation.AccessApprover;
import com.enterprisepasswordsafe.ui.web.servlets.authorisation.UserLevelConditionalConfigurationAccessApprover;
import com.enterprisepasswordsafe.ui.web.utils.SecurityUtils;
import com.enterprisepasswordsafe.ui.web.utils.ServletUtils;


/**
 * Servlet to direct the user to the hierarchy editing screen.
 */

public final class EditHierarchy extends HttpServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = 5081239248366681865L;

	/**
	 * The access authenticator
	 */

	private static final AccessApprover accessApprover =
		new UserLevelConditionalConfigurationAccessApprover(ConfigurationOption.EDIT_USER_MINIMUM_USER_LEVEL);

    /**
     * The action text for an add action.
     */

    public static final String ADD_ACTION = "a";

    /**
     * The action text for a delete action.
     */

    public static final String DELETE_ACTION = "d";

    /**
     * The action text for copying an item.
     */

    public static final String COPY_ACTION = "c";

    /**
     * The action text for copying an item.
     */

    public static final String DEEP_COPY_ACTION = "o";

    /**
     * The action text for cuting an item.
     */

    public static final String CUT_ACTION = "u";

    /**
     * The action text for pasting an item.
     */

    public static final String PASTE_ACTION = "p";


    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
    		throws ServletException, IOException {
    	try {
			User user = SecurityUtils.getRemoteUser(request);
			SecurityUtils.isAllowedAccess(accessApprover, user);

			ServletUtils servletUtils = ServletUtils.getInstance();
		    String nodeId = servletUtils.getNodeId(request);
		    if(nodeId == null) {
		    	nodeId = HierarchyNode.ROOT_NODE_ID;
		    }

		    HierarchyNodeDAO hnDAO = HierarchyNodeDAO.getInstance();
		    HierarchyNode node = hnDAO.getById(nodeId);
		    if(node == null) {
		    	nodeId = HierarchyNode.ROOT_NODE_ID;
		    	node = hnDAO.getById(nodeId);
		    }

		    if( node.getType() == HierarchyNode.USER_CONTAINER_NODE ) {
		    	throw new ServletException("Access to that node is forbidden.");
		    }

		    HierarchyNodeAccessRuleDAO hnarDAO = HierarchyNodeAccessRuleDAO.getInstance();
		    List<HierarchyNode> parentage = hnDAO.getParentage(node);
		    if( !user.isAdministrator()
		    &&	hnarDAO.getAccessibilityForUser(node, user) == HierarchyNodeAccessRuleDAO.ACCESIBILITY_DENIED) {
		        for( HierarchyNode thisNode : parentage ) {
		        	if(hnarDAO.getAccessibilityForUser(thisNode, user) == HierarchyNodeAccessRuleDAO.ACCESIBILITY_DENIED) {
		        		break;
		        	}
		        	node = thisNode;
		    	}
		        servletUtils.generateErrorMessage(
		    			request,
		    			"You are not allowed access to the folder you requested. You have been diverted to a folder you can access."
		    		);
		    	parentage = hnDAO.getParentage(node);
		    }
		    servletUtils.setCurrentNodeId(request, node.getNodeId());

		    String action = request.getParameter(BaseServlet.ACTION_PARAMETER);

		    request.setAttribute("node", node);
		    request.setAttribute(BaseServlet.NODE_PARENTAGE, parentage);

		    HttpSession session = request.getSession(false);

		    // Check for movement.
		    String oldNodeId = servletUtils.getNodeId(request);
		    if (oldNodeId != null && !nodeId.equals(oldNodeId)) {
		        action = null;
		    }

		    // Check for an action
		    if (action != null && action.length() > 0) {
		        String nextPage = performAction(session, request, user, action, node);
		        if (nextPage != null) {
		        	response.sendRedirect(request.getContextPath()+nextPage);
		        	return;
		        }
		    }


		    String hideEmpty = ConfigurationDAO.getValue(ConfigurationOption.HIDE_EMPTY_FOLDERS);
		    boolean includeEmpty = (hideEmpty.equals(Configuration.HIDE_EMPTY_FOLDERS_OFF));

		    if	( user.isSubadministrator() ){
		        String displayEdit = ConfigurationDAO.getValue(ConfigurationOption.EDIT_USER_MINIMUM_USER_LEVEL);
		        if( displayEdit != null && 	displayEdit.equals("S") ) {
		        	includeEmpty = true;
		        }
		    }

		    HierarchyNodeChildren children = hnDAO.getChildrenValidForUser(node, user, includeEmpty, null, null);
		    request.setAttribute(BaseServlet.NODE_CHILDREN, children);
		    servletUtils.setCurrentNodeId(request, nodeId);
    	} catch(SQLException sqle) {
    		throw new ServletException("There was a problem editing the hierarchy.", sqle);
    	} catch(GeneralSecurityException gse) {
    		throw new ServletException("There was a problem editing the hierarchy.", gse);
    	} catch(CloneNotSupportedException cnse) {
    		throw new ServletException("There was a problem editing the hierarchy.", cnse);
    	}
        request.getRequestDispatcher("/subadmin/edit_subnodes.jsp").forward(request, response);
    }

    /**
     * Edit hierarchy may be posted to with an action, so the post is passed on to the get
     * handler.
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
    		throws ServletException, IOException {
    	doGet(request, response);
    }

    /**
     * Performs the user requested action.
     *
     * @param session The session for this request.
     * @param request The request holding the data about what to do.
     * @param user The user performing the action.
     * @param action The action being performed.
     * @param node The node the any action should be associated with.
     *
     * @return The next page to send the user to.
     *
     * @throws SQLException Thrown if there is a problem accessing the database.
     * @throws GeneralSecurityException Thrown if there is problem decrypting data.
     * @throws UnsupportedEncodingException
     * @throws CloneNotSupportedException
     */

    private String performAction(final HttpSession session, final HttpServletRequest request, final User user,
    		final String action, final HierarchyNode node)
            throws SQLException, GeneralSecurityException, IOException, CloneNotSupportedException {
        if (action.equals(EditHierarchy.ADD_ACTION)) {
            return "/subadmin/add_subnode.jsp";
        }

        ServletUtils servletUtils = ServletUtils.getInstance();
        if (action.equals(EditHierarchy.COPY_ACTION)
                || action.equals(EditHierarchy.DEEP_COPY_ACTION)
                || action.equals(EditHierarchy.CUT_ACTION)) {
            String[] nodes = request
                    .getParameterValues(BaseServlet.NODE_LIST_PARAMETER);
            if (nodes == null || nodes.length == 0) {
                StringBuilder message = new StringBuilder();
                if (action.equals(EditHierarchy.COPY_ACTION)
                 || action.equals(EditHierarchy.DEEP_COPY_ACTION)) {
                    message.append("Copy");
                } else if (action.equals(EditHierarchy.CUT_ACTION)) {
                    message.append("Cut");
                }
                message.append(" failed, No items were selected.");
                servletUtils.generateErrorMessage(request, message.toString());
                return null;
            }

            HierarchyNodeDAO hnDAO = HierarchyNodeDAO.getInstance();
            for (int i = 0; i < nodes.length; i++) {
                String thisNode = nodes[i];
                if (thisNode.startsWith("p_")) {
                    String passwordId = thisNode.substring(2);
                    nodes[i] = hnDAO.getNodeIDForObject(node.getNodeId(),passwordId);
                }
            }

            session.setAttribute(BaseServlet.ACTION_PARAMETER, action);
            session.setAttribute(BaseServlet.NODE_LIST_PARAMETER, nodes);

            StringBuilder message = new StringBuilder();
            message.append("The selected item");
            if (nodes.length > 1) {
                message.append("s have");
            } else {
                message.append(" has");
            }
            message.append(" been ");

            if (action.equals(EditHierarchy.COPY_ACTION)
             || action.equals(EditHierarchy.DEEP_COPY_ACTION)) {
                message.append("copied.");
            } else if (action.equals(EditHierarchy.CUT_ACTION)) {
                message.append("cut.");
            }

            servletUtils.generateMessage(request, message.toString());
        } else if (action.equals(EditHierarchy.PASTE_ACTION)) {
            String sessonAction = (String) session
                    .getAttribute(BaseServlet.ACTION_PARAMETER);
            String[] nodes = (String[]) session
                    .getAttribute(BaseServlet.NODE_LIST_PARAMETER);
            if (sessonAction != null && nodes != null && nodes.length > 0) {
                HierarchyNodeDAO hnDAO = HierarchyNodeDAO.getInstance();
                if (sessonAction.equals(EditHierarchy.COPY_ACTION)) {
                    pasteCopiedNodes(hnDAO, node, nodes);
                } else if (sessonAction.equals(EditHierarchy.DEEP_COPY_ACTION)) {
                    pasteDeepCopiedNodes(hnDAO, node, nodes);
                } else if (sessonAction.equals(EditHierarchy.CUT_ACTION)) {
                    pasteCutNodes(hnDAO, node, nodes);
                } else {
                	servletUtils.generateErrorMessage(request, "The items selected were not copied or pasted.");
                }
            } else {
            	servletUtils.generateErrorMessage(request, "Paste failed. No items have been cut or copied.");
            }

            session.removeAttribute(BaseServlet.ACTION_PARAMETER);
            session.removeAttribute(BaseServlet.NODE_LIST_PARAMETER);
        } else if (action.equals(EditHierarchy.DELETE_ACTION)) {
            String[] nodes = request
                    .getParameterValues(BaseServlet.NODE_LIST_PARAMETER);
            if (nodes == null || nodes.length == 0) {
            	servletUtils.generateErrorMessage(request, "Delete failed, No items were selected.");
                return null;
            }

            HierarchyNodeDAO hnDAO = HierarchyNodeDAO.getInstance();
            // TODO : Check permissions of subnodes, and block if
            // the user can not access a specific subnode.
            for (String thisNodeId : nodes) {
                // Skip deletiong of any non-existant nodes
                if (thisNodeId != null && thisNodeId.startsWith("p_")) {
                    thisNodeId = hnDAO.getNodeIDForObject(node.getNodeId(), thisNodeId.substring(2));
                }
                if(thisNodeId == null) {
                    continue;
                }
                HierarchyNode thisNode = hnDAO.getById(thisNodeId);
                if(thisNode == null) {
                    continue;
                }

                if(!isDeletableByUser(user, thisNode)) {
                    servletUtils.generateErrorMessage(request, "Delete failed, You do not have the permissions needed to delete all the objects specified.");
                    return null;
                }
            }

            // Now the permissions have been checked we can perform the delete.
            for (String thisNodeId : nodes) {
                if (thisNodeId != null && thisNodeId.startsWith("p_")) {
                    thisNodeId = hnDAO.getNodeIDForObject(node.getNodeId(), thisNodeId.substring(2));
                }
                if(thisNodeId == null) {
                    continue;
                }

                HierarchyNode thisNode = hnDAO.getById(thisNodeId);
                if (thisNode != null) {
                    hnDAO.deleteNode(thisNode, user);
                }
            }
        }

        return null;
    }

    /**
     * Verify the user has the permission to delete a hierarchy node.
     *
     * @param user The user trying to perform the delete.
     * @param node The Node being deleted.
     *
     * @return true if the node can be deleted by the user, false if not.
     */

    private boolean isDeletableByUser(final User user, final HierarchyNode node)
        throws SQLException, GeneralSecurityException, UnsupportedEncodingException {
        if(node.getType() == HierarchyNode.OBJECT_NODE) {
            AccessControl ac = AccessControlDAO.getInstance().getAccessControl(user, node.getName());
            return (ac != null && ac.getModifyKey() != null);
        }

        for(HierarchyNode thisNode : HierarchyNodeDAO.getInstance().getAllChildren(node)) {
            if(!isDeletableByUser(user,thisNode)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Paste a series of nodes which have been cut.
     *
     * @param hnDAO The DAO for manipulating HierarchyNode objects.
     * @param node The new parent node.
     * @param nodes The list of nodes which were cut.
     *
     * @throws SQLException Thrown if there is a problem accessing the database.
     */
    private void pasteCutNodes(final HierarchyNodeDAO hnDAO, final HierarchyNode node, final String[] nodes)
            throws SQLException, GeneralSecurityException {
        for(String nodeId : nodes) {
            if (nodeId == null) {
                continue;
            }

            HierarchyNode theNode = hnDAO.getById(nodeId);
            if (theNode != null) {
            	hnDAO.moveTo(theNode, node);
            } else {
                log("Unable to cut/paste " + nodeId + " - Node was null.");
            }
        }
    }

    /**
     * Paste a series of nodes which have been copied.
     *
     * @param hnDAO The DAO for manipulating HierarchyNode objects.
     * @param newParent The new parent node.
     * @param nodes The list of nodes which are to be copied.
     *
     * @throws SQLException Thrown if there is a problem accessing the database.
     * @throws CloneNotSupportedException
     */

    private void pasteCopiedNodes(final HierarchyNodeDAO hnDAO, final HierarchyNode newParent, final String[] nodes)
        throws SQLException, GeneralSecurityException, CloneNotSupportedException {
    	String newParentId = newParent.getNodeId();
        for(String nodeId : nodes) {
            if (nodeId == null) {
                continue;
            }

            HierarchyNode theNode = hnDAO.getById(nodeId);
            if (theNode != null) {
            	hnDAO.copyTo(theNode, newParentId);
            } else {
                log("Unable to copy " + nodeId + " - Node was null.");
            }
        }
    }

    /**
     * Paste a series of nodes which have been "deep" copied.
     *
     * @param nodes The list of nodes which were cut.
     *
     * @throws SQLException
     *             Thrown if there is a problem accessing the database.
     * @throws CloneNotSupportedException
     */

    private void pasteDeepCopiedNodes(final HierarchyNodeDAO hnDAO, final HierarchyNode node, final String[] nodes)
        throws SQLException, GeneralSecurityException, CloneNotSupportedException {
        for(String nodeId : nodes) {
            if (nodeId == null) {
                continue;
            }

            HierarchyNode theNode = hnDAO.getById(nodeId);
            if (theNode != null) {
            	hnDAO.deepCopyTo(theNode, node.getNodeId());
            } else {
                log("Unable to copy " + nodeId + " - Node was null.");
            }
        }
    }

    /**
     * @see javax.servlet.Servlet#getServletInfo()
     */
    @Override
	public String getServletInfo() {
        return "Allows the editing of a node in the hierarchy";
    }
}
