package samples.server;

import collaborative.engine.operation.EditOperationRequest;
import collaborative.engine.vcs.Commit;
import collaborative.engine.vcs.CommitStream;
import collaborative.engine.vcs.EditVersionControl;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.function.Consumer;

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

    private static final String COMMIT_STREAM = "commit";

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
                    .onNext(commit -> client.sendEvent("update", new UpdateAckCallback(commit, client), commit));
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

    private static void ifCommitStreamPresent(SocketIOClient client, Consumer<CommitStream> handler) {
        CommitStream commitStream = client.get(COMMIT_STREAM);
        if (commitStream != null) {
            handler.accept(commitStream);
        }
    }

    private static class UpdateAckCallback extends VoidAckCallback {

        final Commit commit;
        final SocketIOClient client;

        UpdateAckCallback(Commit commit, SocketIOClient client) {
            this.commit = commit;
            this.client = client;
        }

        @Override
        protected void onSuccess() {
            ifCommitStreamPresent(client, commitStream -> {
                commitStream.moveTo(commit);
                commitStream.resume();
            });
        }

        @Override
        public void onTimeout() {
            ifCommitStreamPresent(client, CommitStream::resume);
        }
    }
}
