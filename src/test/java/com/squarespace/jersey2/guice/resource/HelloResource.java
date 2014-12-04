package com.squarespace.jersey2.guice.resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("hello")
public final class HelloResource {

  @Inject
  HelloService service;

  @Inject
  @Named("simple")
  HelloService namedService;

  @Inject
  @Other
  HelloService annotadedService;
  
  @Inject
  HelloProvidedService provider;
  
  @Inject
  @Named("overridden")
  HelloService overridden;

  @GET
  public String hello() {
    return service.hello();
  }

  @GET
  @Path("named")
  public String helloNamed() {
    return namedService.hello();
  }

  @GET
  @Path("annotaded")
  public String helloAnnotaded() {
    return annotadedService.hello();
  }
  
  @GET
  @Path("provided")
  public String helloProvided() {
    return provider.helloProvided();
  }
  
  @GET
  @Path("overridden")
  public String helloOverrided() {
    return overridden.hello();
  }
}