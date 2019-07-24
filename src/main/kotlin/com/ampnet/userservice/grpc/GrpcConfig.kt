package com.ampnet.userservice.grpc

import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorConfigurer
import org.springframework.context.annotation.Configuration
import net.devh.boot.grpc.server.security.authentication.BasicGrpcAuthenticationReader
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader
import org.springframework.context.annotation.Bean

@Configuration
class GrpcConfig {

    @Bean
    fun authenticationReader(): GrpcAuthenticationReader {
        return BasicGrpcAuthenticationReader()
    }

    @Bean
    fun globalInterceptorConfigurerAdapter(): GlobalClientInterceptorConfigurer {
        return GlobalClientInterceptorConfigurer {
                registry -> registry.addClientInterceptors(GrpcLogInterceptor())
        }
    }
}
