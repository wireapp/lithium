package com.wire.bots.sdk;

import org.junit.Test;

import static com.wire.xenon.tools.Util.mentionLen;
import static com.wire.xenon.tools.Util.mentionStart;

public class MentionTest {
    @Test
    public void beginExtractMentionTest() {
        String txt = "@dejan This is a mention";
        int offset = mentionStart(txt);
        int len = mentionLen(txt);
        String mention = txt.substring(offset, offset + len);

        assert offset == 0;
        assert len == 6;
        assert mention.equals("@dejan");
    }

    @Test
    public void middleExtractMentionTest() {
        String txt = "Hey @dejan_wire This is a mention";
        int offset = mentionStart(txt);
        int len = mentionLen(txt);
        String mention = txt.substring(offset, offset + len);

        assert offset == 4;
        assert len == 11;
        assert mention.equals("@dejan_wire");
    }

    @Test
    public void endExtractMentionTest() {
        String txt = "This is a mention @dejan";
        int offset = mentionStart(txt);
        int len = mentionLen(txt);
        String mention = txt.substring(offset, offset + len);

        assert offset == 18;
        assert len == 6;

        assert mention.equals("@dejan");
    }

    @Test
    public void specialExtractMentionTest() {
        String txt = "@ This is @dejan: A mention ";
        int offset = mentionStart(txt);
        int len = mentionLen(txt);
        String mention = txt.substring(offset, offset + len);

        assert mention.equals("@dejan");
    }
}
