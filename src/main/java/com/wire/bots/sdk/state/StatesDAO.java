package com.wire.bots.sdk.state;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.UUID;

public interface StatesDAO {
    @SqlUpdate("INSERT INTO States (botId, bot) VALUES (:botId, to_json(:bot::json)) ON CONFLICT (botId) DO UPDATE SET bot = EXCLUDED.bot")
    int insert(@Bind("botId") UUID botId,
               @Bind("bot") String bot);

    @SqlQuery("SELECT bot FROM States WHERE botId = :botId")
    String get(@Bind("botId") UUID botId);

    @SqlUpdate("DELETE FROM States WHERE botId = :botId")
    int delete(@Bind("botId") UUID botId);
}
