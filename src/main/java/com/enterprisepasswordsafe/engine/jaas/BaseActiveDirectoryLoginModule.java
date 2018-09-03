package com.enterprisepasswordsafe.engine.jaas;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Set;

public class BaseActiveDirectoryLoginModule {
    boolean commitOK;
    boolean loginOK;
    Subject subject;


    public boolean abort() {
        // If we didn't log in ignore this module
        if (!loginOK) {
            return false;
        }

        if (commitOK) {
            // If the login was OK, and the commit was OK we need to log out
            // again.
            logout();
        } else {
            // If the commit hasn't happened clear out any stored info
            loginOK = false;
        }

        return true;
    }

    public boolean commit() {
        commitOK = false;
        if (!loginOK) {
            return false;
        }

        DatabaseLoginPrincipal principal = DatabaseLoginPrincipal.getInstance();
        Set<Principal> principals = subject.getPrincipals();
        if (!principals.contains(principal)) {
            principals.add(principal);
        }

        commitOK = true;
        return true;
    }

    public boolean logout() {
        DatabaseLoginPrincipal principal = DatabaseLoginPrincipal.getInstance();
        subject.getPrincipals().remove(principal);
        loginOK = false;
        commitOK = false;

        return true;
    }

}
