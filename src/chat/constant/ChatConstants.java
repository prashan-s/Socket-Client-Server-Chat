package chat.constant;

public class ChatConstants {

    public static class Event {
        // HANDSHAKE EVENTS
        public static final String E_SUBMIT_NAME = "SUBMIT_NAME";
        public static final String E_NAME_ACCEPTED = "NAME_ACCEPTED";
        public static final String E_USER_LIST = "USER_LIST";
        public static final String E_FORCE_EXIT = "FORCE_EXIT";

        // GENERAL MESSAGES
        public static final String E_MESSAGE = "MESSAGE";

        // MESSAGE MODE EVENTS
        public static final String E_BROADCAST = "BROADCAST";
        public static final String E_P2P = "P2P";
        public static final String E_ERROR = "ERROR";
    }

    public static class Config {
        // CONFIG
        public static final int APP_PORT = 9001;
        public static final String APP_CLIENT_NAME = "Chatter";
        public static final String APP_STRING_BROADCAST_MODE = "Broadcast Mode";
        public static final String APP_STRING_P2P_MODE = "P2P Mode";

    }
}
