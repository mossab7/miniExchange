package dev.miniExchange.infra.grpc;


import dev.miniExchange.infra.grpc.generated.MiniExchangeServiceGrpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;

@Configuration
@Profile("!test")
public class MatchingEngineGrpcConfig {

    @Value("${grpc.matching-engine.host:localhost}")
    String host;
    @Value("${grpc.matching-engine.port:50051}")
    int port;

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel channel(){
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    @Bean
    public MiniExchangeServiceGrpc.MiniExchangeServiceBlockingStub stub(ManagedChannel channel)
    {
        return MiniExchangeServiceGrpc.newBlockingStub(channel);
    }

}
