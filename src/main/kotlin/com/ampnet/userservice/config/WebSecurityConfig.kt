package com.ampnet.userservice.config

import com.ampnet.core.jwt.UnauthorizedEntryPoint
import com.ampnet.core.jwt.filter.DisabledProfileFilter
import com.ampnet.core.jwt.filter.JwtAuthenticationFilter
import com.ampnet.core.jwt.provider.JwtAuthenticationProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Override
    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    @Autowired
    fun globalUserDetails(
        authBuilder: AuthenticationManagerBuilder,
        applicationProperties: ApplicationProperties
    ) {
        val authenticationProvider = JwtAuthenticationProvider(applicationProperties.jwt.signingKey)
        authBuilder.authenticationProvider(authenticationProvider)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf(
            HttpMethod.HEAD.name,
            HttpMethod.GET.name,
            HttpMethod.POST.name,
            HttpMethod.PUT.name,
            HttpMethod.DELETE.name
        )
        configuration.allowedHeaders = listOf(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.CACHE_CONTROL
        )

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    override fun configure(http: HttpSecurity) {
        val unauthorizedHandler = UnauthorizedEntryPoint()
        val authenticationTokenFilter = JwtAuthenticationFilter()
        val profileFilter = DisabledProfileFilter()

        http.cors().and().csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable()
            .authorizeRequests()
            .antMatchers("/actuator/**").permitAll()
            .antMatchers("/docs/index.html").permitAll()
            .antMatchers("/public/**").permitAll()
            .antMatchers("/token/**", "/signup").permitAll()
            .antMatchers(HttpMethod.POST, "/forgot-password", "/forgot-password/token").permitAll()
            .antMatchers("/mail-confirmation", "/mail-check", "/mail-user-pending/*").permitAll()
            .antMatchers("/identyum/**").permitAll()
            .antMatchers(HttpMethod.POST, "/tx_broadcast").permitAll()
            .antMatchers("/test/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler)
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        http
            .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(profileFilter, FilterSecurityInterceptor::class.java)
    }
}
