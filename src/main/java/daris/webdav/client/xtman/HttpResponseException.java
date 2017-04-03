package daris.webdav.client.xtman;

public class HttpResponseException extends Exception {

    private static final long serialVersionUID = 7410246280903710738L;

    HttpResponseException(int responseCode, String responseMessage) {
        super("Unexpected HTTP response: " + responseCode + " " + responseMessage);
    }

}
