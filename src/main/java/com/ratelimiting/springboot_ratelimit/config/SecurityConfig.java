package com.ratelimiting.springboot_ratelimit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ratelimiting.springboot_ratelimit.filter.RateLimitFilter;

import lombok.RequiredArgsConstructor;

@Configuration 
@EnableWebSecurity 
@RequiredArgsConstructor 
public class SecurityConfig {
  private final RateLimitFilter rateLimitFilter;

  @Bean 
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(
        auth -> 
          auth.requestMatchers("/api/public/**")
          .permitAll()
          .anyRequest()
          .authenticated()
      ).addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class); //run ratelimiting filter before authentication token

    return http.build();
  }
}
