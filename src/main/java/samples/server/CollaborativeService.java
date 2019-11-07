package samples.server;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Socket.io server
 * @author XyParaCrim
 */
@Component
public final class CollaborativeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollaborativeService.class);

    @Autowired
    private SocketIOServer ioServer;

    private ConcurrentLinkedQueue<SocketIOClient> clients = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void start() {
        ioServer.addListeners(new OnConnection());
        ioServer.start();
    }

    @PreDestroy
    public void stop() {
        ioServer.stop();
    }

    private class OnConnection {
        @OnConnect
        public void onConnect(SocketIOClient client) {
            clients.add(client);
            LOGGER.info("A socket client is connected: {}", client.getSessionId());
        }

        @OnDisconnect
        public void onDisconnect(SocketIOClient client) {
            clients.remove(client);
            LOGGER.info("A socket client is disconnected: {}", client.getSessionId());
        }
    }
}
