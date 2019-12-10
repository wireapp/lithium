package com.wire.bots.sdk.crypto.storage;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SessionsDAO {
    @SqlUpdate("INSERT INTO Sessions (id, sid, data) VALUES (:id, :sid, :data) ON CONFLICT (id, sid) DO UPDATE SET data = EXCLUDED.data")
    int insert(@Bind("id") String id,
               @Bind("sid") String sid,
               @Bind("data") byte[] data);

    @SqlQuery("SELECT * FROM Sessions WHERE id = :id AND sid = :sid FOR UPDATE")
    @RegisterMapper(_Mapper.class)
    Session get(@Bind("id") String id,
                @Bind("sid") String sid);

    @SqlUpdate("DELETE FROM Sessions WHERE id = :id")
    int delete(@Bind("id") String id);

    class _Mapper implements ResultSetMapper<Session> {
        @Override
        public Session map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
            Session session = new Session();
            session.id = rs.getString("id");
            session.sid = rs.getString("sid");
            session.data = rs.getBytes("data");
            return session;
        }
    }
}
