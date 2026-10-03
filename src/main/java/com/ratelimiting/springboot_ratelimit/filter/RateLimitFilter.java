package com.ratelimiting.springboot_ratelimit.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ratelimiting.springboot_ratelimit.service.RateLimitingService;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimitingService rateLimitingService;

  @Override
  protected void doFilterInternal(
    HttpServletRequest request, 
    HttpServletResponse response, 
    FilterChain filterChain
  )
    throws ServletException, IOException {
    //extract client ip address
    String clientIp = getClientIp(request);
    //get bucket for the specific IP
    Bucket tokenBucket = rateLimitingService.resolveBucket(clientIp);
    var probe = tokenBucket.tryConsumeAndReturnRemaining(1);

    if(probe.isConsumed()){
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
    } else {
      //calculate how long the client needs to take
      var waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
      response.setContentType("application/json");

      String jsonResponse = """
          {
            "status": %s,
            "error": "Too Many Requests",
            "message": "You have exhausted your requets quota",
            "retryAfterSeconds": %s
          }
          """.formatted(
            HttpStatus.TOO_MANY_REQUESTS.value(), 
            String.valueOf(waitForRefill)
          );

      response.getWriter().write(jsonResponse);
    }
  }

  private String getClientIp(HttpServletRequest request){
    //check for X-Forwarded-For
    String xHeader = request.getHeader("X-Forwarded-For");
    if(xHeader == null || xHeader.isEmpty()){
      return request.getRemoteAddr();
    }
    return xHeader.split(",")[0].trim();
  }

}
