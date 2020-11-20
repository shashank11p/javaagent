/*
 * Copyright The Hypertrace Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.hypertrace.servlet.v3_1;

import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * TODO https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1395 is resolved
 * move this to org.hypertrace package.
 */
@AutoService(InstrumentationModule.class)
public class Servlet31BodyInstrumentationModule extends InstrumentationModule {

  public Servlet31BodyInstrumentationModule() {
    super(InstrumentationName.INSTRUMENTATION_NAME[0], InstrumentationName.INSTRUMENTATION_NAME[1]);
  }

  public Servlet31BodyInstrumentationModule(
      String mainInstrumentationName, String... otherInstrumentationNames) {
    super(mainInstrumentationName, otherInstrumentationNames);
  }

  @Override
  public int getOrder() {
    /**
     * Order 1 assures that this instrumentation runs after OTEL servlet instrumentation so we can
     * access current span in our advice.
     */
    return 1;
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.ByteBufferData",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.CharBufferData",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.BufferedWriterWrapper",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.BufferedReaderWrapper",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.ServletSpanDecorator",
      // This class is extended in spark-web. The package name would be resolved to spark web
      // package
      // and it would cause failure.
      //        packageName + ".InstrumentationName",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.InstrumentationName",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletResponse",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletResponse$BufferingServletOutputStream",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletRequest",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletRequest$ServletInputStreamWrapper",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BodyCaptureAsyncListener",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.Servlet31Advice",
    };
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new Servlet31BodyInstrumentation());
  }

  private static final class Servlet31BodyInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderMatcher() {
      // Optimization for expensive typeMatcher.
      return hasClassesNamed("javax.servlet.http.HttpServlet", "javax.servlet.ReadListener");
    }

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return safeHasSuperType(
          namedOneOf("javax.servlet.FilterChain", "javax.servlet.http.HttpServlet"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          namedOneOf("doFilter", "service")
              .and(takesArgument(0, named("javax.servlet.ServletRequest")))
              .and(takesArgument(1, named("javax.servlet.ServletResponse")))
              .and(isPublic()),
          Servlet31Advice.class.getName());
    }
  }
}