package daris.webdav.client.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import daris.webdav.client.WebDAVClient;
import daris.webdav.client.WebDAVClientFactory;

public class WebDAVClientTest {

    private WebDAVClient client;
    private String filePath;
    private String destFilePath;

    @Before
    public void prepare() throws Throwable {
        Map<String, String> settings = TestUtils.getTestSettings();
        String uri = settings.get("uri");
        String username = settings.get("username");
        String password = settings.get("password");
        client = WebDAVClientFactory.create(uri, username, password);

        System.out.print("Creating temporary file...");
        File file = TestUtils.generateTempFile(1024);
        System.out.println("done.");
        filePath = file.getCanonicalPath();
        destFilePath = "/daris-webdav-plugin-test/test-" + System.currentTimeMillis() + ".dat";
    }

    @Test
    public void test() {

        ExecutorService executor = Executors.newFixedThreadPool(4);
        long duration = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            final int j = i + 1;
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String dstPath = destFilePath + "." + (j % 4);
                        System.out.println(Thread.currentThread().getName() + ": PUT: " + dstPath);
                        client.put(dstPath, new File(filePath));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        duration = System.currentTimeMillis() - duration;
        System.out.println("Duration: " + (((double) duration) / 1000.0) + " seconds.");
    }

    @After
    public void cleanup() throws IOException {
        System.out.print("Deleting temporary file...");
        Files.delete(Paths.get(filePath));
        System.out.println("done.");
    }
}
