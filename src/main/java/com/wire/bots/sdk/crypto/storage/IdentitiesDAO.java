package com.wire.bots.sdk.crypto.storage;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface IdentitiesDAO {
    @SqlUpdate("INSERT INTO Identities (id, data) VALUES (:id, :data) ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data")
    int insert(@Bind("id") String id,
               @Bind("data") byte[] data);

    @SqlQuery("SELECT data FROM Identities WHERE id = :id")
    byte[] get(@Bind("id") String id);

    @SqlUpdate("DELETE FROM Identities WHERE id = :id")
    int delete(@Bind("id") String id);
}
