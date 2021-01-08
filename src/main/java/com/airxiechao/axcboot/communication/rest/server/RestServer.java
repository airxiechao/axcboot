package com.airxiechao.axcboot.communication.rest.server;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Auth;
import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.common.annotation.Params;
import com.airxiechao.axcboot.communication.rest.annotation.*;
import com.airxiechao.axcboot.communication.rest.aspect.PinHandler;
import com.airxiechao.axcboot.communication.rest.security.*;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import com.airxiechao.axcboot.communication.websocket.annotation.Endpoint;
import com.airxiechao.axcboot.communication.websocket.common.AbsWsListener;
import com.airxiechao.axcboot.util.StringUtil;
import com.alibaba.fastjson.JSON;
import io.undertow.Undertow;
import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static io.undertow.Handlers.*;
import static io.undertow.util.StatusCodes.METHOD_NOT_ALLOWED;
import static io.undertow.util.StatusCodes.NOT_FOUND;

public class RestServer {

    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private static final Logger accessLogger = LoggerFactory.getLogger("access");

    protected String ip;
    protected int port;
    protected String basePath;
    protected AuthRoleChecker authRoleChecker;
    protected Undertow server;
    protected RoutingHandler router = routing();
    protected PathHandler pather = path();

    public RestServer(String ip, int port, String basePath, Consumer<Undertow.Builder> option, AuthRoleChecker authRoleChecker){
        this.ip = ip;
        this.port = port;
        this.authRoleChecker = authRoleChecker;

        if(StringUtil.isBlank(basePath) || "/".equals(basePath)){
            this.basePath = "";
        }else{
            if(!basePath.startsWith("/")){
                basePath = "/" + basePath;
            }

            if(basePath.endsWith("/")){
                basePath = basePath.substring(0, basePath.length() - 1);
            }

            this.basePath = basePath;
        }

        this.pather.addPrefixPath(this.basePath + "/rest", new EagerFormParsingHandler(router));

        Undertow.Builder builder = Undertow.builder()
                .addHttpListener(this.port, this.ip)
                .setHandler(addGzip(addAccessLog(addDefaultError(pather))));

        if(null != option){
            option.accept(builder);
        }

        this.server = builder.build();
    }

    public void start(){
        server.start();
    }

    public void stop(){
        server.stop();
    }

    public RestServer registerStatic(String urlPath, String resourcePath,
                                     String welcomeHtml, String loginHtml,
                                     String[] roles){

        ResourceHandler resourceHandler = resource(new PathResourceManager(Path.of(resourcePath)));
        if(null != welcomeHtml){
            resourceHandler.addWelcomeFiles(welcomeHtml);
        }

        if(!urlPath.startsWith("/")){
            urlPath = "/" + urlPath;
        }

        urlPath = this.basePath + urlPath;

        String loginPath = null;
        if(null != loginHtml){
            loginPath = urlPath+(urlPath.endsWith("/")?"":"/")+loginHtml;
        }

        pather.addPrefixPath(
                urlPath,
                addStaticSecurity(resourceHandler, roles, loginPath)
        );

        return this;
    }

    public RestServer registerWs(Class<? extends AbsWsListener> cls){

        Endpoint endpoint = cls.getAnnotation(Endpoint.class);
        if(null != endpoint){
            String urlPath = this.basePath + endpoint.value();

            try {
                AbsWsListener listener = cls.getConstructor().newInstance();
                pather.addPrefixPath(urlPath, websocket((exchange, channel) -> {

                    try {
                        // check auth
                        checkWsAuth(listener, exchange);

                        // on connect
                        listener.onConnect(exchange, channel);

                        channel.getReceiveSetter().set(listener);
                        channel.resumeReceives();

                    } catch (Exception e) {
                        logger.error("ws connect error: {}", e.getMessage());

                        try {
                            channel.close();
                        } catch (Exception ioException) {
                            logger.error("ws connect close error", e);
                        }
                    }

                }));
            } catch (Exception e) {
                logger.error("register websocket [{}] error", urlPath);
            }
        }

        return this;
    }

    public RestServer registerHandler(Class<?> cls){
        Method[] methods = cls.getDeclaredMethods();
        for(Method method : methods){
            method.setAccessible(true);
            HttpHandler httpHandler = getMethodHandler(method);

            Get get = method.getAnnotation(Get.class);
            if(null != get){
                String path = get.value();
                router.get(path, httpHandler);
            }

            Post post = method.getAnnotation(Post.class);
            if(null != post){
                String path = post.value();
                router.post(path, httpHandler);
            }

            Delete delete = method.getAnnotation(Delete.class);
            if(null != delete){
                String path = delete.value();
                router.delete(path, httpHandler);
            }
        }

        return this;
    }

    protected HttpHandler getMethodHandler(Method method){
        HttpHandler httpHandler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

                if (httpServerExchange.isInIoThread()) {
                    httpServerExchange.dispatch(this);
                    return;
                }

                try{
                    // check auth
                    checkAuth(method, httpServerExchange);

                    // check guard
                    checkGuard(method, httpServerExchange);

                    // check parameters
                    checkParameter(method, httpServerExchange);

                    // handle aspect before invoke
                    Map<String, Object> pinStore = new HashMap<>();
                    handleAspectBeforeInvoke(method, httpServerExchange, pinStore);

                    int methodParamCount = method.getParameterCount();
                    String queryPath = httpServerExchange.getRequestPath();
                    if(queryPath.endsWith(".bin")){
                        httpServerExchange.startBlocking();
                        if(1 == methodParamCount){
                            method.invoke(null, httpServerExchange);
                        }else if(2 == methodParamCount){
                            method.invoke(null, httpServerExchange, pinStore);
                        }else{
                            throw new Exception("rest method parameter count error");
                        }

                    }else{
                        Object ret;
                        if(1 == methodParamCount){
                            ret = method.invoke(null, httpServerExchange);
                        }else if(2 == methodParamCount){
                            ret = method.invoke(null, httpServerExchange, pinStore);
                        }else{
                            throw new Exception("rest method parameter count error");
                        }

                        httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                        httpServerExchange.getResponseSender().send(JSON.toJSONString(ret));
                    }

                    // handle aspect after invoke
                    handleAspectAfterInvoke(method, httpServerExchange, pinStore);
                }catch (Exception e){

                    logger.error("rest handler error: {}", e.getMessage(), e);

                    Response resp = new Response();

                    if(e instanceof AuthException){
                        resp.authError(e.getMessage());
                    }else{
                        String errMessage = e.getMessage();
                        if(null == errMessage || errMessage.isBlank()){
                            errMessage = e.getCause().getMessage();
                        }
                        resp.error(errMessage);
                    }

                    httpServerExchange.getResponseHeaders().clear();
                    httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

                    httpServerExchange.getResponseSender().send(JSON.toJSONString(resp));
                }

            }
        };

        return httpHandler;
    }

    protected void checkWsAuth(AbsWsListener wsListener, WebSocketHttpExchange exchange) throws AuthException{
        Auth auth = wsListener.getClass().getAnnotation(Auth.class);
        if(null != auth){
            if(auth.ignore()){
                return;
            }

            String[] roles = auth.roles();
            checkWsAuthToken(wsListener, exchange, roles);
        }
    }

    protected void checkWsAuthToken(AbsWsListener wsListener, WebSocketHttpExchange exchange, String[] roles) throws AuthException{

        AuthPrincipal authPrincipal = RestUtil.getWsAuthPrincipal(exchange);
        if(null == authPrincipal){
            throw new AuthException("invalid ws auth token");
        }

        Date now = new Date();
        if(authPrincipal.getExpireTime().before(now)){
            throw new AuthException("ws auth token expired");
        }

        boolean hasRole = wsListener.hasRole(exchange, authPrincipal, roles);
        if(!hasRole){
            throw new AuthException("ws no user or mis-match role");
        }
    }

    protected void checkAuth(Method method, HttpServerExchange httpServerExchange) throws AuthException{
        Auth auth = method.getAnnotation(Auth.class);
        if(null == auth){
            auth = method.getDeclaringClass().getAnnotation(Auth.class);
        }

        if(null != auth){
            // if ignore
            if(auth.ignore()){
                return;
            }

            // require check auth
            String[] roles = auth.roles();
            checkAuthToken(httpServerExchange, roles);
        }
    }

    protected void checkAuthToken(HttpServerExchange httpServerExchange, String[] roles) throws AuthException {

        AuthPrincipal authPrincipal = RestUtil.getAuthPrincipal(httpServerExchange);
        if(null == authPrincipal){
            throw new AuthException("invalid auth token");
        }

        Date now = new Date();
        if(authPrincipal.getExpireTime().before(now)){
            throw new AuthException("auth token expired");
        }

        boolean hasRole = authRoleChecker.hasRole(httpServerExchange, authPrincipal, roles);
        if(!hasRole){
            throw new AuthException("no user or mis-match role");
        }
    }

    protected void checkGuard(Method method, HttpServerExchange httpServerExchange) throws Exception{
        Guard guard = method.getAnnotation(Guard.class);
        if(null == guard){
            guard = method.getDeclaringClass().getAnnotation(Guard.class);
        }

        if(null != guard){
            // if ignore
            if(guard.ignore()){
                return;
            }

            // require check guard
            checkGuardToken(method, httpServerExchange);
        }
    }

    protected void checkGuardToken(Method method, HttpServerExchange httpServerExchange) throws Exception {

        GuardPrincipal guardPrincipal = RestUtil.getGuardPrincipal(httpServerExchange);
        if(null == guardPrincipal){
            throw new Exception("invalid guard token");
        }

        Date now = new Date();
        if(guardPrincipal.getExpireTime().before(now)){
            throw new Exception("guard token expired");
        }

        String restPath = RestUtil.getRestPath(method);
        if(guardPrincipal.getPath().equals(restPath)){
            throw new Exception("mis-match method");
        }
    }

    protected void checkParameter(Method method, HttpServerExchange httpServerExchange) throws Exception {
        Annotation[] methodAnnos = method.getAnnotations();
        List<Param> params = new ArrayList<>();
        for(Annotation anno : methodAnnos){
            if(anno instanceof Params){
                params.addAll(Arrays.asList(((Params) anno).value()));
            }else if(anno instanceof Param){
                Param param = (Param)anno;
                params.add(param);
            }
        }

        Map<String, Deque<String>> queryParam = httpServerExchange.getQueryParameters();
        FormData formData = httpServerExchange.getAttachment(FormDataParser.FORM_DATA);

        for(Param param : params){
            String name = param.value();
            boolean required = param.required();


            if(required){
                Deque<String> dv = queryParam.get(name);
                boolean queryExisted = null != dv && null != dv.getFirst() && !dv.getFirst().isBlank();
                boolean formExisted = false;
                if(null != formData && null != formData.get(name) &&
                        null != formData.get(name).getFirst()){
                    if(formData.get(name).getFirst().isFileItem()){
                        // is file
                        if(null != formData.get(name).getFirst().getFileItem()){
                            formExisted = true;
                        }
                    }else{
                        // not file
                        if(null != formData.get(name).getFirst().getValue() &&
                                !formData.get(name).getFirst().getValue().isBlank()){
                            formExisted = true;
                        }
                    }
                }

                if(!queryExisted && !formExisted){
                    throw new Exception("parameter [" + name + "] is required");
                }
            }
        }
    }

    protected void handleAspectBeforeInvoke(Method method, HttpServerExchange exchange, Map pinStore) throws Exception{
        Annotation[] methodAnnos = method.getAnnotations();
        List<Pin> pins = new ArrayList<>();
        for(Annotation anno : methodAnnos){
            if(anno instanceof Pins){
                pins.addAll(Arrays.asList(((Pins) anno).value()));
            }else if(anno instanceof Pin){
                Pin pin = (Pin)anno;
                pins.add(pin);
            }
        }

        for(Pin pin : pins){
            if(pin.when().equals(PinWhen.BEFORE_INVOKE)){
                Class handlerCls = pin.handler();
                PinHandler handler = (PinHandler)handlerCls.getConstructor().newInstance();
                handler.handle(exchange, pinStore);
            }
        }
    }

    protected void handleAspectAfterInvoke(Method method, HttpServerExchange exchange, Map pinStore) {
        Annotation[] methodAnnos = method.getAnnotations();
        List<Pin> pins = new ArrayList<>();
        for(Annotation anno : methodAnnos){
            if(anno instanceof Pins){
                pins.addAll(Arrays.asList(((Pins) anno).value()));
            }else if(anno instanceof Pin){
                Pin pin = (Pin)anno;
                pins.add(pin);
            }
        }

        for(Pin pin : pins){
            if(pin.when().equals(PinWhen.AFTER_INVOKE)){
                try{
                    Class handlerCls = pin.handler();
                    PinHandler handler = (PinHandler)handlerCls.getConstructor().newInstance();
                    handler.handle(exchange, pinStore);
                }catch (Exception e){
                    logger.error("handle aspect after invoke error", e);
                }
            }
        }
    }

    protected HttpHandler addStaticSecurity(final HttpHandler toWrap, String[] roles, String loginPath){
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
                String path = httpServerExchange.getRequestPath();
                String query = httpServerExchange.getQueryString();
                String fullPaht = path + "?" + query;

                boolean ignore =
                        path.endsWith(".js") ||
                        path.endsWith(".css") ||
                        path.endsWith(".jpg") ||
                        path.endsWith(".png") ||
                        path.endsWith(".ico") ||
                        path.equals(loginPath);

                if(!ignore && null != roles && roles.length > 0){
                    try{
                        checkAuthToken(httpServerExchange, roles);
                    }catch (AuthException e){
                        if(null != loginPath){
                            RestUtil.redirect(httpServerExchange, loginPath+"?redirect="+ URLEncoder.encode(fullPaht, "UTF-8"));
                            return;
                        }

                        throw e;
                    }

                }

                toWrap.handleRequest(httpServerExchange);
            }
        };
    }

    protected HttpHandler addAccessLog(final HttpHandler toWrap){
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
                String path = httpServerExchange.getRequestPath();
                String remoteIp = RestUtil.getRemoteIp(httpServerExchange);

                if(path.startsWith(basePath + "/rest/")){
                    accessLogger.info(remoteIp + " => " + path);
                }

                toWrap.handleRequest(httpServerExchange);
            }
        };
    }

    protected HttpHandler addGzip(final HttpHandler toWrap){
        HttpHandler encodingHandler = new EncodingHandler.Builder().build(null)
                .wrap(toWrap);
        return encodingHandler;
    }

    protected HttpHandler addDefaultError(final HttpHandler toWrap){
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

                httpServerExchange.addDefaultResponseListener(new DefaultResponseListener() {
                    @Override
                    public boolean handleDefaultResponse(final HttpServerExchange exchange) {
                        if (!exchange.isResponseChannelAvailable()) {
                            return false;
                        }

                        int statusCode = exchange.getStatusCode();
                        if(NOT_FOUND != statusCode && METHOD_NOT_ALLOWED != statusCode){
                            return false;
                        }

                        httpServerExchange.getResponseHeaders().clear();
                        httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

                        Response resp = new Response();
                        resp.error("not found");

                        httpServerExchange.getResponseSender().send(JSON.toJSONString(resp));
                        return true;
                    }
                });

                toWrap.handleRequest(httpServerExchange);
            }
        };
    }
}
