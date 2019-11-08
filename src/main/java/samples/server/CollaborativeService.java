package samples.server;

import collaborative.engine.operation.EditOperationRequest;
import collaborative.engine.vcs.CommitStream;
import collaborative.engine.vcs.EditVersionControl;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static samples.server.VcsUtils.COMMIT_STREAM;
import static samples.server.VcsUtils.ifCommitStreamPresent;

/**
 * Socket.io server
 *
 * @author XyParaCrim
 */
@Component
public final class CollaborativeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollaborativeService.class);

    @Autowired
    private SocketIOServer ioServer;
    @Autowired
    private EditVersionControl versionControl;

    @PostConstruct
    public void start() {
        ioServer.addListeners(new OnConnection());
        ioServer.addListeners(new OnCollaborative());
        ioServer.start();
    }

    @PreDestroy
    public void stop() {
        ioServer.stop();
    }

    /**
     * Manage connection issue
     */
    @SuppressWarnings("unused")
    private static class OnConnection {
        @OnConnect
        public void onConnect(SocketIOClient client) {
            LOGGER.info("A socket client is connected: {}", client.getSessionId());
        }

        @OnDisconnect
        public void onDisconnect(SocketIOClient client) {
            ifCommitStreamPresent(client, CommitStream::close);
            LOGGER.info("A socket client is disconnected: {}", client.getSessionId());
        }
    }

    /**
     * About collaborative-editing listener
     */
    @SuppressWarnings("unused")
    private class OnCollaborative {
        @OnEvent("open")
        public void onOpen(SocketIOClient client, AckRequest ackRequest) {
            if (!client.has(COMMIT_STREAM)) {
                client.set(COMMIT_STREAM, newCommitStream(client));
            }
            final CommitStream commitStream = client.get(COMMIT_STREAM);

            // 取消所有新版本的订阅
            commitStream.pause();
            commitStream.currentCommit(ackRequest::sendAckData);
        }

        private CommitStream newCommitStream(final SocketIOClient client) {
            return versionControl.newCommitStream()
                    .onClose(() -> client.del(COMMIT_STREAM))
                    .onNewVersion(commit -> client.sendEvent("update", new UpdateAckCallback(commit, client), commit));
        }

        @OnEvent("sure-open")
        public void onSureOpen(SocketIOClient client) {
            CommitStream commitStream = client.get(COMMIT_STREAM);
            if (commitStream != null) {
                commitStream.resume();
            } else {
                LOGGER.error("No commitStream when client try to sure open");
            }
        }

        @OnEvent("edit")
        public void onEdit(EditOperationRequest editOperationRequest) {
            versionControl.handle(editOperationRequest);
        }
    }

}
