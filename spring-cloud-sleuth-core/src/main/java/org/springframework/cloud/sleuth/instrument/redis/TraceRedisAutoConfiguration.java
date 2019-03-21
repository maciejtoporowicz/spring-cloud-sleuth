/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.redis;

import brave.Tracing;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.BraveTracing;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables Redis span information propagation.
 *
 * @author Chao Chang
 * @since 2.2.0
 */
@Configuration
@OnRedisEnabled
@ConditionalOnBean(Tracing.class)
@AutoConfigureAfter({ TraceAutoConfiguration.class })
public class TraceRedisAutoConfiguration {

	@Configuration
	@ConditionalOnClass(ClientResources.class)
	static class LettuceConfig {

		@Bean
		static TraceLettuceClientResourcesBeanPostProcessor traceLettuceClientResourcesBeanPostProcessor(
				Tracing tracing) {
			return new TraceLettuceClientResourcesBeanPostProcessor(tracing);
		}

	}

}

class TraceLettuceClientResourcesBeanPostProcessor implements BeanPostProcessor {

	private final Tracing tracing;

	TraceLettuceClientResourcesBeanPostProcessor(Tracing tracing) {
		this.tracing = tracing;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof ClientResources) {
			ClientResources cr = (ClientResources) bean;
			// tracing of ClientResources instance created by default is `disabled()`
			if (cr.tracing() == null
					|| cr.tracing() == io.lettuce.core.tracing.Tracing.disabled()) {
				return cr.mutate().tracing(BraveTracing.create(this.tracing)).build();
			}
		}
		return bean;
	}

}
