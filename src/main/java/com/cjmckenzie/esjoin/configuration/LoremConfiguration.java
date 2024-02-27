package com.cjmckenzie.esjoin.configuration;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoremConfiguration {

  @Bean
  public Lorem lorem() {
    return LoremIpsum.getInstance();
  }

}
