/*
 * Copyright 2014 Squarespace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squarespace.jersey2.guice.resource;

import static com.squarespace.jersey2.guice.resource.HelloServiceImpl.ANNOTATED_HELLO;
import static com.squarespace.jersey2.guice.resource.HelloServiceImpl.DEFAULT_HELLO;
import static com.squarespace.jersey2.guice.resource.HelloServiceImpl.NAMED_HELLO;
import static com.squarespace.jersey2.guice.resource.HelloServiceImpl.PROVIDED_HELLO;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.hk2.api.ServiceLocator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.google.inject.util.Modules;
import com.squarespace.jersey2.guice.BootstrapUtils;
import com.squarespace.jersey2.guice.utils.HttpServer;
import com.squarespace.jersey2.guice.utils.HttpServerUtils;

public class ResourceWithNamedInjectionTest {

  private static HttpServer SERVER;
  
  private static Injector injector;

  @BeforeClass
  public void setUp() throws IOException {
    ServiceLocator locator = BootstrapUtils.newServiceLocator();
    List<Module> modules = new ArrayList<>();
    modules.add(new ServletModule());
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HelloResource.class);
        
        bind(HelloService.class).toInstance(new HelloServiceImpl(DEFAULT_HELLO));
        bind(HelloService.class).annotatedWith(Names.named("simple")).toInstance(new HelloServiceImpl(NAMED_HELLO));
        bind(HelloService.class).annotatedWith(Other.class).toInstance(new HelloServiceImpl(ANNOTATED_HELLO));
        bind(HelloService.class).annotatedWith(Names.named("overridden")).toInstance(new HelloServiceImpl(NAMED_HELLO));
      }

      @Provides
      public HelloProvidedService providesService(final HelloService service) {
        return new HelloProvidedService() {
            
            @Override
            public String helloProvided() {
                return service.hello() + " (provided)";
            }
        };
      };
    });
    
    modules = Arrays.asList( Modules.override(modules).with(new AbstractModule() {

        @Override
        protected void configure() {
            bind(HelloService.class).annotatedWith(Names.named("overridden")).toInstance(new HelloServiceImpl(NAMED_HELLO + " overridden"));
        }
        
    }));

    injector = BootstrapUtils.newInjector(locator, modules);

    BootstrapUtils.install(locator);

    SERVER = HttpServerUtils.newHttpServer(HelloResource.class);
  }

  @AfterClass
  public void tearDown() throws IOException {
    SERVER.close();
    BootstrapUtils.reset();
  }

  private Client client;

  @AfterTest
  public void closeClient() {
    client.close();
  }

  private WebTarget getWebTarget() {
    String url = "http://localhost:" + HttpServerUtils.PORT;
    client = ClientBuilder.newClient();
    return client.target(url).path(UriBuilder.fromResource(HelloResource.class).toString());
  }

  @Test
  public void baseTest() throws IOException {
    WebTarget target = getWebTarget();
    String value = target.request(MediaType.TEXT_PLAIN).get(String.class);
    assertNotNull(value);
    assertEquals(value, DEFAULT_HELLO);
  }

  @Test
  public void namedTest() throws IOException {
    WebTarget target = getWebTarget();
    String value = target.path("named").request(MediaType.TEXT_PLAIN).get(String.class);
    assertNotNull(value);
    assertEquals(value, NAMED_HELLO);
  }

  @Test
  public void annotatedTest() throws IOException {
    WebTarget target = getWebTarget();
    String value = target.path("annotaded").request(MediaType.TEXT_PLAIN).get(String.class);
    assertNotNull(value);
    assertEquals(value, ANNOTATED_HELLO);
  }
  
  @Test
  public void providesTest() throws IOException {
    WebTarget target = getWebTarget();
    String value = target.path("provided").request(MediaType.TEXT_PLAIN).get(String.class);
    assertNotNull(value);
    assertEquals(value, PROVIDED_HELLO);
  }
  
  @Test
  public void overriddenTest() throws IOException {
    WebTarget target = getWebTarget();
    String value = target.path("overridden").request(MediaType.TEXT_PLAIN).get(String.class);
    assertNotNull(value);
    assertEquals(value, NAMED_HELLO + " overridden");
  }
  
  @Test
  public void overriddenDirectlyTest() throws IOException {
    String value = injector.getInstance(Key.get(HelloService.class, Names.named("overridden"))).hello();
    assertNotNull(value);
    assertEquals(value, NAMED_HELLO + " overridden");
  }
}
