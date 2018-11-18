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

package com.enterprisepasswordsafe.engine.configuration;

import com.enterprisepasswordsafe.engine.dbabstraction.SupportedDatabase;
import com.enterprisepasswordsafe.engine.preferences.PreferencesRepository;
import com.enterprisepasswordsafe.engine.preferences.UserPreferencesRepository;
import com.enterprisepasswordsafe.proguard.ExternalInterface;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class EnvironmentVariableBackedJDBCConfigurationRepository
        implements JDBCConfigurationRepository, ExternalInterface {

	private JDBCConnectionInformation connectionInformation;

	public JDBCConnectionInformation load()
			throws GeneralSecurityException {
		synchronized (this) {
			if(connectionInformation == null) {
				connectionInformation = new GenericJDBCConnectionInformation(
						System.getenv("EPS_DATABASE_TYPE"),
						System.getenv("EPS_JDBC_DRIVER_CLASS"),
						System.getenv("EPS_JDBC_URL"),
						System.getenv("EPS_DATABASE_USERNAME"),
						System.getenv("EPS_DATABASE_PASSWORD"));
			}
		}

		return connectionInformation;
	}
}
