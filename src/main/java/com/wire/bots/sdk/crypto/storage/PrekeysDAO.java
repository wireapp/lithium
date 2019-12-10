package com.wire.bots.sdk.crypto.storage;

import com.wire.bots.cryptobox.PreKey;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface PrekeysDAO {
    @SqlUpdate("INSERT INTO Prekeys (id, kid, data) VALUES (:id, :kid, :data) ON CONFLICT (id, kid) DO UPDATE SET data = EXCLUDED.data")
    int insert(@Bind("id") String id,
               @Bind("kid") int kid,
               @Bind("data") byte[] data);

    @SqlQuery("SELECT kid, data FROM Prekeys WHERE id = :id")
    @RegisterMapper(_Mapper.class)
    List<PreKey> get(@Bind("id") String id);

    @SqlUpdate("DELETE FROM Prekeys WHERE id = :id")
    int delete(@Bind("id") String id);

    class _Mapper implements ResultSetMapper<PreKey> {
        @Override
        public PreKey map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
            int kid = rs.getInt("kid");
            byte[] data = rs.getBytes("data");
            return new PreKey(kid, data);
        }
    }
}
