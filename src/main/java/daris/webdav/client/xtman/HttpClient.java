package daris.webdav.client.xtman;

import java.io.InputStream;
import java.util.Map;

public interface HttpClient {

    <T> T execute(String requestUri, String requestMethod, Map<String, String> requestHeaders,
            InputStream requestContentInputStream, long requestContentLength, String requestContentType,
            HttpResponseHandler<T> responseHandler) throws Throwable;

}
