package daris.webdav.client;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.xtman.io.util.StreamUtils;

public abstract class HttpClientBase implements HttpClient {

    public static <T> T execute(String username, String password, String requestUri, String requestMethod,
            Map<String, String> requestHeaders, InputStream requestContentInputStream, long requestContentlength,
            String requestContentType, HttpResponseHandler<T> responseHandler) throws Throwable {
        URI uri = URI.create(requestUri);
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        InputStream responseInputStream = null;
        try {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            setRequestMethod(conn, requestMethod);
            String authorization = getAuthorization(username, password);
            conn.setRequestProperty("Authorization", authorization);
            if (requestHeaders != null) {
                Set<String> hns = requestHeaders.keySet();
                for (String hn : hns) {
                    conn.setRequestProperty(hn, requestHeaders.get(hn));
                }
            }
            if (requestContentInputStream != null) {
                try {
                    StreamUtils.copy(requestContentInputStream, conn.getOutputStream());
                } finally {
                    conn.getOutputStream().close();
                }
            }
            int responseCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();
            try {
                responseInputStream = conn.getInputStream();
            } catch (Throwable e) {
                responseInputStream = null;
            }
            Map<String, List<String>> responseHeaders = conn.getHeaderFields();
            try {
                if (responseHandler != null) {
                    return responseHandler.handleResponse(responseCode, responseMessage, responseHeaders,
                            responseInputStream);
                } else {
                    return null;
                }
            } finally {
                if (responseInputStream != null) {
                    responseInputStream.close();
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    public static String getAuthorization(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    private static void setRequestMethod(HttpURLConnection conn, String method) throws Throwable {
        try {
            conn.setRequestMethod(method);
        } catch (ProtocolException e) {
            Class<?> c = conn.getClass();
            Field methodField = null;
            Field delegateField = null;
            try {
                delegateField = c.getDeclaredField("delegate");
            } catch (NoSuchFieldException nsfe) {

            }
            while (c != null && methodField == null) {
                try {
                    methodField = c.getDeclaredField("method");
                } catch (NoSuchFieldException nsfe) {

                }
                if (methodField == null) {
                    c = c.getSuperclass();
                }
            }
            if (methodField != null) {
                methodField.setAccessible(true);
                methodField.set(conn, method);
            }

            if (delegateField != null) {
                delegateField.setAccessible(true);
                HttpURLConnection delegate = (HttpURLConnection) delegateField.get(conn);
                setRequestMethod(delegate, method);
            }
        }
    }

}
