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

package com.enterprisepasswordsafe.engine.database;

import com.enterprisepasswordsafe.proguard.ExternalInterface;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class IPZoneDAO
	implements ExternalInterface {

    private static final String GET_ZONES =
        "SELECT ip_zone_id, name, ip_version, ip_start, ip_end FROM ip_zones ORDER BY name ";

    private static final String GET_ZONE_BY_ID  =
        "SELECT ip_zone_id, name, ip_version, ip_start, ip_end FROM ip_zones WHERE ip_zone_id = ? ";

    private static final String STORE_ZONE =
        "INSERT INTO ip_zones(ip_zone_id, name, ip_version, ip_start, ip_end) VALUES( ?, ?, ?, ?, ?)";

    private static final String UPDATE_ZONE =
        "UPDATE ip_zones SET name = ?, ip_start = ?, ip_end = ? WHERE ip_zone_id = ?";

    private static final String DELETE_ZONE =
        "DELETE FROM ip_zones WHERE ip_zone_id = ?";

	private IPZoneDAO() {
		super();
	}

	public IPZone create( String name, int version, String firstIp, String lastIp )
		throws SQLException {
		IPZone newZone = new IPZone(name, version, firstIp, lastIp);
		store(newZone);
		return newZone;
	}

    public void store( final IPZone zone )
        throws SQLException {
        try(PreparedStatement ps = BOMFactory.getCurrentConntection().prepareStatement(STORE_ZONE)) {
            int idx = 1;
            ps.setString(idx++, zone.getId());
            ps.setString(idx++, zone.getName());
            ps.setInt   (idx++, zone.getIpVersion());
            ps.setString(idx++, zone.getStartIp());
            ps.setString(idx,   zone.getEndIp());
            ps.executeUpdate();
        }
    }

    public void update( final IPZone zone)
        throws SQLException {
        try(PreparedStatement ps = BOMFactory.getCurrentConntection().prepareStatement(UPDATE_ZONE)) {
            int idx = 1;
            ps.setString(idx++, zone.getName());
            ps.setString(idx++, zone.getStartIp());
            ps.setString(idx++, zone.getEndIp());
            ps.setString(idx, zone.getId());
            ps.executeUpdate();
        }
    }

    public void delete( final IPZone zone )
        throws SQLException {
        try(PreparedStatement ps = BOMFactory.getCurrentConntection().prepareStatement(DELETE_ZONE)) {
            ps.setString(1, zone.getId());
            ps.executeUpdate();
        }
    }

    public IPZone getById( final String id )
        throws SQLException {
        try(PreparedStatement ps = BOMFactory.getCurrentConntection().prepareStatement(GET_ZONE_BY_ID)) {
            ps.setString(1, id);
            ps.setMaxRows(1);
            try(ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new IPZone(rs) : null;
            }
        }
    }

    public List<IPZone> getAll( )
        throws SQLException {
        List<IPZone> zones = new ArrayList<IPZone>();
        try(Statement stmt = BOMFactory.getCurrentConntection().createStatement()) {
            try(ResultSet rs = stmt.executeQuery(GET_ZONES)) {
                while (rs.next()) {
                    zones.add(new IPZone(rs));
                }
            }
        }
        return zones;
    }


    private static final class InstanceHolder {
        private static final IPZoneDAO INSTANCE = new IPZoneDAO();
    }

    public static IPZoneDAO getInstance() {
		return InstanceHolder.INSTANCE;
    }
}
