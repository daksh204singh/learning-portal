package com.daksh.ibm.intenship.learningportal.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.WebSecurity;
//import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.server.SecurityWebFilterChain;

//@EnableWebFluxSecurity
//public class SecurityConfig {
//
//    private static final String[] AUTH_WHITELIST = {
//            // root
//            "/",
//
//            // -- swagger ui
//            "**/swagger-resources/**",
//            "/swagger-ui.html",
//            "/v2/**",
//            "/webjars/**"
//    };
//
//    @Bean
//    public MapReactiveUserDetailsService userDetailsService() {
//        UserDetails user = User
//                .withUsername("user")
//                .password(passwordEncoder().encode("password"))
//                .roles("USER")
//                .build();
//        return new MapReactiveUserDetailsService(user);
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(
//            ServerHttpSecurity http) {
//        return http.authorizeExchange()
//                .pathMatchers(AUTH_WHITELIST)
//                .permitAll()
//                .anyExchange()
//                .authenticated()
//                .and()
//                .formLogin()
//                .and().build();
//    }
//}
