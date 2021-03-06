/*
 * Copyright 2015-2016 Odnoklassniki Ltd, Mail.Ru Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package one.nio.http;

import one.nio.http.gen.RequestHandlerGenerator;
import one.nio.net.Socket;
import one.nio.server.RejectedSessionException;
import one.nio.server.Server;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class HttpServer extends Server {
    private final Map<String, Map<HttpMethod, RequestHandler>> defaultHandlers = new HashMap<>();
    private final Map<String, Map<String, Map<HttpMethod, RequestHandler>>> handlersByAlias = new HashMap<>();
    private final Map<String, Map<String, Map<HttpMethod, RequestHandler>>> handlersByHost = new HashMap<>();

    public HttpServer(HttpServerConfig config, Object... routers) throws IOException {
        super(config);

        if (config.virtualHosts != null) {
            for (Map.Entry<String, String[]> virtualHost : config.virtualHosts.entrySet()) {
                Map<String, Map<HttpMethod, RequestHandler>> handlers = new HashMap<>();
                handlersByAlias.put(virtualHost.getKey(), handlers);
                for (String host : virtualHost.getValue()) {
                    handlersByHost.put(host.toLowerCase(), handlers);
                }
            }
        }

        addRequestHandlers(this);
        for (Object router : routers) {
            addRequestHandlers(router);
        }
    }

    @Override
    public HttpSession createSession(Socket socket) throws RejectedSessionException {
        return new HttpSession(socket, this);
    }

    public void handleRequest(Request request, HttpSession session) throws IOException {
        RequestHandler handler = findHandlerByHost(request);
        if (handler == null) {
            handler = findHandlerByMethod(request);
        }

        if (handler != null) {
            handler.handleRequest(request, session);
        } else {
            handleDefault(request, session);
        }
    }

    private RequestHandler findHandlerByMethod(Request request) {
        Map<HttpMethod, RequestHandler> handlers = defaultHandlers.get(request.getPath());
        if (handlers == null) {
            return null;
        }
        if (handlers.containsKey(HttpMethod.fromMethod(request.getMethod()))) {
            return handlers.get(HttpMethod.fromMethod(request.getMethod()));
        } else {
            return handlers.get(HttpMethod.ANY);
        }
    }

    public void handleDefault(Request request, HttpSession session) throws IOException {
        Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
        session.sendResponse(response);
    }

    private RequestHandler findHandlerByHost(Request request) {
        if (handlersByHost.isEmpty()) {
            return null;
        }

        String host = request.getHost();
        if (host == null) {
            return null;
        }

        Map<String, Map<HttpMethod, RequestHandler>> handlers = handlersByHost.get(host.toLowerCase());
        if (handlers == null) {
            return null;
        }

        if (handlers.containsKey(request.getPath())) {
            RequestHandler requestHandler = handlers.get(request.getPath()).get(HttpMethod.fromMethod(request.getMethod()));
            if (requestHandler != null) {
                return requestHandler;
            }
            return handlers.get(request.getPath()).get(HttpMethod.ANY);

        }
        return null;
    }

    public void addRequestHandlers(Object router) {
        ArrayList<Class> supers = new ArrayList<>(4);
        for (Class<?> cls = router.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
            supers.add(cls);
        }

        RequestHandlerGenerator generator = new RequestHandlerGenerator();
        for (int i = supers.size(); --i >= 0; ) {
            Class<?> cls = supers.get(i);

            VirtualHost virtualHost = cls.getAnnotation(VirtualHost.class);
            String[] aliases = virtualHost == null ? null : virtualHost.value();

            for (Method m : cls.getMethods()) {
                Path annotation = m.getAnnotation(Path.class);
                if (annotation == null) {
                    continue;
                }

                RequestHandler requestHandler = generator.generateFor(m, router);
                for (String path : annotation.value()) {
                    if (!path.startsWith("/")) {
                        throw new IllegalArgumentException("Path '" + path + "' is not absolute");
                    }

                    if (aliases == null || aliases.length == 0) {
                        Map<HttpMethod, RequestHandler> handlers = defaultHandlers.get(path);
                        if (handlers == null) {
                            handlers = new EnumMap<>(HttpMethod.class);
                            defaultHandlers.put(path, handlers);
                        }
                        handlers.put(annotation.method(), requestHandler);
                    } else {
                        for (String alias : aliases) {
                            Map<String, Map<HttpMethod, RequestHandler>> map = handlersByAlias.get(alias);
                            if (map != null) {
                                Map<HttpMethod, RequestHandler> handlers = map.get(path);
                                if (handlers == null) {
                                    handlers = new EnumMap<>(HttpMethod.class);
                                    map.put(path, handlers);
                                }
                                handlers.put(annotation.method(), requestHandler);
                            }

                        }
                    }
                }
            }
        }
    }
}
