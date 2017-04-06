package daris.webdav.client;

import java.net.HttpURLConnection;

public class HttpResponseException extends Exception {

    private static final long serialVersionUID = 7410246280903710738L;

    private int _responseCode;
    private String _responseMessage;

    HttpResponseException(int responseCode, String responseMessage) {
        super("Unexpected HTTP response: " + responseCode + " " + responseMessage);
        _responseCode = responseCode;
        _responseMessage = responseMessage;
    }

    public int responseCode() {
        return _responseCode;
    }

    public String responseMessage() {
        return _responseMessage;
    }

    public boolean isUnauthorized() {
        return _responseCode == HttpURLConnection.HTTP_UNAUTHORIZED;
    }

}
