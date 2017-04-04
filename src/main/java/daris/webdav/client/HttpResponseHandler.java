package daris.webdav.client;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface HttpResponseHandler<T> {

    T handleResponse(int responseCode, String responseMessage, Map<String, List<String>> headers,
            InputStream responseContentInputStream) throws Throwable;

}
