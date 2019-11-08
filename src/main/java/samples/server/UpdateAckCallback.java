package samples.server;

import collaborative.engine.vcs.Commit;
import collaborative.engine.vcs.commitStream;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.VoidAckCallback;

import static samples.server.VcsUtils.ifCommitStreamPresent;

class UpdateAckCallback extends VoidAckCallback {

    private final Commit commit;
    private final SocketIOClient client;

    UpdateAckCallback(Commit commit, SocketIOClient client) {
        this.commit = commit;
        this.client = client;
    }

    @Override
    protected void onSuccess() {
        ifCommitStreamPresent(client, commitStream -> commitStream.moveTo(commit).resume());
    }

    @Override
    public void onTimeout() {
        ifCommitStreamPresent(client, commitStream::resume);
    }
}
