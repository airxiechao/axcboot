package com.airxiechao.axcboot.communication.websocket.common;

import com.airxiechao.axcboot.communication.common.annotation.Auth;
import com.airxiechao.axcboot.communication.common.security.IAuthTokenChecker;
import com.airxiechao.axcboot.communication.rest.security.AuthException;
import com.airxiechao.axcboot.communication.rest.security.AuthPrincipal;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class WsCallback implements WebSocketConnectionCallback {

    private static final Logger logger = LoggerFactory.getLogger(WsCallback.class);

    private AbstractWsListener listener;
    private IAuthTokenChecker authTokenChecker;

    public WsCallback(AbstractWsListener listener, IAuthTokenChecker authTokenChecker){
        this.listener = listener;
        this.authTokenChecker = authTokenChecker;
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        try {
            // check auth
            checkWsAuth(listener, exchange);

            // on connect
            listener.onConnect(exchange, channel);

            channel.getReceiveSetter().set(listener);
            channel.resumeReceives();

        } catch (Exception e) {
            logger.error("ws connect error", e);

            try {
                channel.close();
            } catch (Exception ioException) {
                logger.error("ws connect close error", e);
            }
        }
    }

    protected void checkWsAuth(AbstractWsListener wsListener, WebSocketHttpExchange exchange) throws AuthException {
        Auth auth = wsListener.getClass().getAnnotation(Auth.class);
        if(null != auth){
            if(auth.ignore()){
                return;
            }

            String token = RestUtil.getWsAuthToken(exchange);
            String scope = auth.scope();
            String item = auth.item();
            int mode = auth.mode();

            boolean validated = authTokenChecker.validate(token, scope, item, mode);
            if(!validated){
                throw new AuthException("ws auth token error");
            }
        }
    }

}
