import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.state.RedisState;
import org.junit.Test;

import java.util.UUID;

public class RedisStorageTest {

    @Test
    public void test() throws Exception {
        Configuration.DB conf = new Configuration.DB();
        conf.host = "localhost";
        conf.port = 6379;
        conf.password = "password";

        String botId = UUID.randomUUID().toString();

        RedisState storage = new RedisState(botId, conf);
        NewBot bot = new NewBot();
        bot.id = botId;
        bot.client = "client";
        bot.locale = "en";
        bot.token = "token";
        bot.conversation = new Conversation();
        bot.conversation.id = UUID.randomUUID().toString();
        bot.conversation.name = "conv";

        boolean b = storage.saveState(bot);
        assert b;

        NewBot state = storage.getState();
        assert state != null;
        assert state.id.equals(bot.id);
        assert state.conversation.name.equals(bot.conversation.name);

        boolean removeState = storage.removeState();
        assert removeState;
    }
}
