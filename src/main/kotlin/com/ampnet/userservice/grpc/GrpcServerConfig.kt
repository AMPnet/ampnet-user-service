package com.ampnet.userservice.grpc

import org.springframework.context.annotation.Configuration
import net.devh.boot.grpc.server.security.authentication.BasicGrpcAuthenticationReader
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader
import org.springframework.context.annotation.Bean

@Configuration
class GrpcServerConfig {

    @Bean
    fun authenticationReader(): GrpcAuthenticationReader {
        return BasicGrpcAuthenticationReader()
    }
}
