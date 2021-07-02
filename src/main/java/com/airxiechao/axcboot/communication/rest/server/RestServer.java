package com.airxiechao.axcboot.communication.rest.server;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Auth;
import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.common.annotation.Params;
import com.airxiechao.axcboot.communication.common.security.IAuthTokenChecker;
import com.airxiechao.axcboot.communication.rest.annotation.*;
import com.airxiechao.axcboot.communication.rest.healthcheck.HealthCheckRestHandler;
import com.airxiechao.axcboot.communication.rest.aspect.PinHandler;
import com.airxiechao.axcboot.communication.rest.security.*;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import com.airxiechao.axcboot.communication.websocket.annotation.WsEndpoint;
import com.airxiechao.axcboot.communication.websocket.common.AbstractWsListener;
import com.airxiechao.axcboot.communication.websocket.common.WsCallback;
import com.airxiechao.axcboot.crypto.SslUtil;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.MapBuilder;
import com.airxiechao.axcboot.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static io.undertow.Handlers.*;
import static io.undertow.util.StatusCodes.METHOD_NOT_ALLOWED;
import static io.undertow.util.StatusCodes.NOT_FOUND;

public class RestServer {

    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private static final Logger accessLogger = LoggerFactory.getLogger("access");

    protected String name;
    protected String ip;
    protected int port;
    protected String basePath;
    protected IAuthTokenChecker authTokenChecker;
    protected Undertow server;
    protected RoutingHandler router = routing();
    protected PathHandler pather = path();

    public RestServer(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public RestServer config(
            String ip, int port, String basePath,
            KeyManagerFactory sslKeyManagerFactory,
            Consumer<Undertow.Builder> option,
            IAuthTokenChecker authTokenChecker){
        this.ip = ip;
        this.port = port;
        this.authTokenChecker = authTokenChecker;

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

        this.pather.addPrefixPath(this.basePath + "/api", new EagerFormParsingHandler(router));

        Undertow.Builder builder = Undertow.builder();

        if(null != sslKeyManagerFactory){
            logger.info("config ssl for rest-server-[{}]", this.name);
            try{
                SSLContext sslContext = SslUtil.createSslContext(
                        sslKeyManagerFactory,
                        SslUtil.buildAllowAllTrustManager());
                builder.addHttpsListener(this.port, ip, sslContext);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }else{
            builder.addHttpListener(this.port, this.ip);
        }

        builder.setHandler(addGzip(addAccessLog(addDefaultError(pather))));

        if(null != option){
            option.accept(builder);
        }

        this.server = builder.build();

        return this;
    }

    public void start(){
        server.start();
        logger.info("rest-server-[{}] has started at [{}:{}]", this.name, this.ip, this.port);
    }

    public void stop(){
        server.stop();
        logger.info("rest-server-[{}] has stopped", this.name);
    }

    public RestServer registerStatic(String urlPath, String resourcePath,
                                     String welcomeHtml, String loginHtml){

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
                addStaticSecurity(resourceHandler, loginPath)
        );

        return this;
    }

    public RestServer registerWs(String endpoint, AbstractWsListener listener) {
        String urlPath = this.basePath + endpoint;
        pather.addPrefixPath(urlPath, websocket(new WsCallback(listener, authTokenChecker)));
        return this;
    }

    public RestServer registerWs(Class<? extends AbstractWsListener> cls){

        WsEndpoint endpoint = cls.getAnnotation(WsEndpoint.class);
        if(null != endpoint){
            String urlPath = this.basePath + endpoint.value();

            try {
                AbstractWsListener listener = cls.getDeclaredConstructor().newInstance();
                pather.addPrefixPath(urlPath, websocket(new WsCallback(listener, authTokenChecker)));
            } catch (Exception e) {
                logger.error("register ws [{}] error", urlPath);
            }
        }

        return this;
    }

    public RestServer registerConsul(int ttl){
        this.registerHandler(HealthCheckRestHandler.class);

        // register to consul
        ConsulClient client = new ConsulClient("localhost");

        NewService newService = new NewService();
        newService.setName(this.name);
        newService.setPort(this.port);
        newService.setMeta(new MapBuilder()
                .put("basePath", this.basePath)
                .build());

        NewService.Check serviceCheck = new NewService.Check();
        serviceCheck.setHttp(
                String.format("http://%s:%d%s%s",
                        "localhost", this.port, this.basePath,
                        RestUtil.getMethodPath(HealthCheckRestHandler.getHealthCheckMethod())));
        serviceCheck.setInterval(ttl + "s");
        newService.setCheck(serviceCheck);

        client.agentServiceRegister(newService);

        logger.info("register consul service [{}]", this.name);

        return this;
    }

    public RestServer registerHandler(Class<?> cls){
        Method[] methods = cls.getDeclaredMethods();
        for(Method method : methods){
            method.setAccessible(true);
            HttpHandler httpHandler = getMethodHandler(method);

            Get get = AnnotationUtil.getMethodAnnotation(method, Get.class);
            if(null != get){
                String path = get.value();
                router.get(path, httpHandler);
            }

            Post post = AnnotationUtil.getMethodAnnotation(method, Post.class);
            if(null != post){
                String path = post.value();
                router.post(path, httpHandler);
            }

            Delete delete = AnnotationUtil.getMethodAnnotation(method, Delete.class);
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

                    Object invokeObj = null;
                    if(!Modifier.isStatic(method.getModifiers())){
                        Constructor constructor = method.getDeclaringClass().getDeclaredConstructor();
                        constructor.setAccessible(true);
                        invokeObj = constructor.newInstance();
                    }

                    Object ret;
                    if(1 == methodParamCount){
                        ret = method.invoke(invokeObj, httpServerExchange);
                    }else if(2 == methodParamCount){
                        ret = method.invoke(invokeObj, httpServerExchange, pinStore);
                    }else{
                        throw new Exception("rest method parameter count error");
                    }

                    if(null != ret){
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

    protected void checkAuth(Method method, HttpServerExchange httpServerExchange) throws AuthException{
        Auth auth = AnnotationUtil.getMethodAnnotation(method, Auth.class);
        if(null == auth){
            auth = method.getDeclaringClass().getAnnotation(Auth.class);
        }

        if(null != auth){
            // if ignore
            if(auth.ignore()){
                return;
            }

            // require check auth
            String token = RestUtil.getAuthToken(httpServerExchange);
            String scope = auth.scope();
            String item = auth.item();
            int mode = auth.mode();

            boolean validated = authTokenChecker.validate(token, scope, item, mode);
            if(!validated){
                throw new AuthException("invalid auth token");
            }
        }
    }

    protected void checkGuard(Method method, HttpServerExchange httpServerExchange) throws Exception{
        Guard guard = AnnotationUtil.getMethodAnnotation(method, Guard.class);
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

        GuardPrincipal guardPrincipal = RestUtil.getGuardPrincipal(httpServerExchange, "guard-key");
        if(null == guardPrincipal){
            throw new Exception("invalid guard token");
        }

        Date now = new Date();
        if(guardPrincipal.getExpireTime().before(now)){
            throw new Exception("guard token expired");
        }

        String methodPath = RestUtil.getMethodPath(method);
        if(guardPrincipal.getPath().equals(methodPath)){
            throw new Exception("mis-match method");
        }
    }

    protected void checkParameter(Method method, HttpServerExchange httpServerExchange) throws Exception {
        Annotation[] methodAnnos = AnnotationUtil.getMethodAnnotations(method);
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
        Annotation[] methodAnnos = AnnotationUtil.getMethodAnnotations(method);
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
        Annotation[] methodAnnos = AnnotationUtil.getMethodAnnotations(method);
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

    protected HttpHandler addStaticSecurity(final HttpHandler toWrap, String loginPath){
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
                String path = httpServerExchange.getRequestPath();
                String query = httpServerExchange.getQueryString();
                String fullPath = path + "?" + query;

                boolean ignore =
                        path.endsWith(".js") ||
                        path.endsWith(".css") ||
                        path.endsWith(".jpg") ||
                        path.endsWith(".png") ||
                        path.endsWith(".ico") ||
                        path.equals(loginPath);

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

                if(path.startsWith(basePath + "/api/") && !path.endsWith("/health/check")){
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
