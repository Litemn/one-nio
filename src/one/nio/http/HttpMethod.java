package one.nio.http;

public enum HttpMethod {

    GET(Request.METHOD_GET),
    POST(Request.METHOD_POST),
    HEAD(Request.METHOD_HEAD),
    OPTIONS(Request.METHOD_OPTIONS),
    PUT(Request.METHOD_PUT),
    DELETE(Request.METHOD_DELETE),
    TRACE(Request.METHOD_TRACE),
    CONNECT(Request.METHOD_CONNECT),
    PATCH(Request.METHOD_PATCH),
    ANY(0);

    private int type;

    HttpMethod(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }


    public static HttpMethod fromMethod(int value) {
        for (HttpMethod method : values()) {
            if (method.type == value) {
                return method;
            }
        }
        throw new IllegalArgumentException("No method with value " + value);
    }

}
