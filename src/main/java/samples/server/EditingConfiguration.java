package samples.server;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EditingConfiguration {
    @Value("${collaborative.port}")
    private int port;

    @Value("${collaborative.hostname}")
    private String hostName;

    @Bean
    public com.corundumstudio.socketio.Configuration socketConfiguration() {
        return new com.corundumstudio.socketio.Configuration(){{
            setPort(port);
            setHostname(hostName);
        }};
    }

    @Bean
    public SocketIOServer socketIOServer(com.corundumstudio.socketio.Configuration socketConfiguration) {
        return new SocketIOServer(socketConfiguration);
    }
}
