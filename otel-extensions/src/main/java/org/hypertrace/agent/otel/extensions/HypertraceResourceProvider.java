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

package org.hypertrace.agent.otel.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.config.HypertraceConfig;

@AutoService(ResourceProvider.class)
public class HypertraceResourceProvider implements ResourceProvider {

  private static final String HYPERTRACE = "hypertrace";
  private static final String JAVA = "java";
  private static final String HYPERTRACE_MODULE_NAME = "hypertrace.module.name";
  private static final String HYPERTRACE_MODULE_VERSION = "hypertrace.module.version";

  private final CgroupsReader cgroupsReader = new CgroupsReader();
  private final AgentConfig agentConfig = HypertraceConfig.get();

  @Override
  public Resource createResource(ConfigProperties config) {
    AttributesBuilder builder = Attributes.builder();
    String containerId = this.cgroupsReader.readContainerId();
    if (containerId != null && !containerId.isEmpty()) {
      builder.put(ResourceAttributes.CONTAINER_ID, containerId);
    }
    builder.put(ResourceAttributes.SERVICE_NAME, agentConfig.getServiceName().getValue());
    builder.put(ResourceAttributes.TELEMETRY_SDK_NAME, HYPERTRACE);
    builder.put(ResourceAttributes.TELEMETRY_SDK_LANGUAGE, JAVA);
    builder.put(
        ResourceAttributes.TELEMETRY_SDK_VERSION,
        HypertraceResourceProvider.class.getPackage().getImplementationVersion());
    builder.put(
        ResourceAttributes.TELEMETRY_AUTO_VERSION,
        HypertraceResourceProvider.class.getPackage().getImplementationVersion());
    builder.put(AttributeKey.stringKey(HYPERTRACE_MODULE_NAME), JAVA);
    builder.put(
        AttributeKey.stringKey(HYPERTRACE_MODULE_VERSION),
        HypertraceResourceProvider.class.getPackage().getImplementationVersion());
    return Resource.create(builder.build());
  }
}
