package samples.server;

import collaborative.engine.vcs.CommitStream;
import com.corundumstudio.socketio.SocketIOClient;

import java.util.function.Consumer;

final class VcsUtils {

    static final String COMMIT_STREAM = "commit";

    static void ifCommitStreamPresent(SocketIOClient client, Consumer<CommitStream> handler) {
        CommitStream commitStream = client.get(COMMIT_STREAM);
        if (commitStream != null) {
            handler.accept(commitStream);
        }
    }

}
