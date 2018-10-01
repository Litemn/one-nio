package one.nio.http;

import one.nio.net.ConnectionString;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

public class RequstMethodTest {

    private static final String URL = "http://127.0.0.1:8181";

    private static final EnumSet<HttpMethod> HTTP_METHODS = EnumSet.of(
            HttpMethod.GET,
            HttpMethod.PATCH,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.OPTIONS,
            HttpMethod.DELETE,
            HttpMethod.CONNECT,
            HttpMethod.TRACE,
            HttpMethod.HEAD
    );


    public static class TestServer extends HttpServer {
        TestServer(HttpServerConfig config) throws IOException {
            super(config);
        }

        @Path(value = "/GET", method = HttpMethod.GET)
        public Response get(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/PUT", method = HttpMethod.PUT)
        public Response put(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/ANY")
        public Response any(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/POST", method = HttpMethod.POST)
        public Response post(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/CONNECT", method = HttpMethod.CONNECT)
        public Response connect(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/DELETE", method = HttpMethod.DELETE)
        public Response delete(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/HEAD", method = HttpMethod.HEAD)
        public Response head(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/OPTIONS", method = HttpMethod.OPTIONS)
        public Response options(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/PATCH", method = HttpMethod.PATCH)
        public Response patch(Request request) {
            return Response.ok(Response.EMPTY);
        }

        @Path(value = "/TRACE", method = HttpMethod.TRACE)
        public Response trace(Request request) {
            return Response.ok(Response.EMPTY);
        }
    }

    private static HttpServer server;
    private static HttpClient client;

    @BeforeClass
    public static void beforeAll() throws IOException {
        server = new TestServer(HttpServerConfigFactory.create(8181));
        server.start();
        client = new HttpClient(new ConnectionString(URL));
    }

    @AfterClass
    public static void afterAll() {
        client.close();
        server.stop();
    }

    @Test
    public void testAny() throws Exception {
        for (HttpMethod method : HTTP_METHODS) {
            verify(method.getType(), "/ANY", 200);
        }
    }

    @Test
    public void test() throws Exception{
        for (HttpMethod currentMethod : HTTP_METHODS) {
            verify(currentMethod.getType(), "/" + currentMethod.name(), 200);
            for (HttpMethod httpMethod : HTTP_METHODS) {
                if (httpMethod.equals(currentMethod)) {
                    continue;
                }
                verify(currentMethod.getType(), "/" + httpMethod.name(), 404);
            }

        }
    }
    private static void verify(int method, String path, int expectedStatus) throws Exception {
        String errorMsg = String.format("Expected status %d for path %s in %s", expectedStatus, path, HttpMethod.fromMethod(method));
        try {
            Request request = client.createRequest(method, path);
            Response response = client.invoke(request);
            assertEquals(errorMsg, expectedStatus, response.getStatus());
        } catch (Exception e) {
            Assert.fail(errorMsg);
            throw e;
        }
    }
}
