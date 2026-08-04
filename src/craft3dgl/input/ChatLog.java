package craft3dgl.input;

import java.util.ArrayList;

/**
 * Dane chatu: kolejka wyswietlanych wiadomosci + bufor wpisywany.
 */
public final class ChatLog {
    public static final int MAX_MESSAGES = 60;
    public static final long FADE_AFTER_MS = 7000;
    public static final long HIDE_AFTER_MS = 10000;

    public final ArrayList<ChatMessage> log = new ArrayList<>();
    public final StringBuilder input = new StringBuilder();
    public boolean open = false;
    public boolean ignoreNextChar = false;

    public void add(String msg) {
        log.add(new ChatMessage(msg));
        if (log.size() > MAX_MESSAGES) log.remove(0);
    }

    public void clear() {
        log.clear();
        input.setLength(0);
        open = false;
    }

    public static final class ChatMessage {
        public final String text;
        public final long shownAt;

        public ChatMessage(String text) {
            this.text = text;
            this.shownAt = System.currentTimeMillis();
        }
    }
}
