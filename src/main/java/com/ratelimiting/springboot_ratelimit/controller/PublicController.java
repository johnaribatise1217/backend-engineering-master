package com.ratelimiting.springboot_ratelimit.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController 
@RequestMapping ("/api/public")
public class PublicController {

  @GetMapping("/hello")
  public String hello() {
    return "Hello! This is the rate limit service";
  }
  
}
