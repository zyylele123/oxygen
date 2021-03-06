/*
 * Copyright (C) 2018 justlive1
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package vip.justlive.oxygen.core.ioc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import vip.justlive.oxygen.core.Plugin;
import vip.justlive.oxygen.core.config.ConfigFactory;
import vip.justlive.oxygen.core.config.ValueConfig;
import vip.justlive.oxygen.core.scan.ClassScannerPlugin;

/**
 * IocPlugin
 * <br>
 * 当前只支持构造方法注入
 * <br>
 * 原因: 易于切换不同ioc实现
 *
 * @author wubo
 */
@Slf4j
public class IocPlugin implements Plugin {

  private static final Strategy STRATEGY = new ConstructorStrategy();
  private static final AtomicInteger TODO_INJECT = new AtomicInteger();

  /**
   * 实例bean， 通过构造函数实例
   *
   * @param clazz 类
   * @return 实例
   */
  public static Object instanceBean(Class<?> clazz) {
    return STRATEGY.instance(clazz);
  }

  @Override
  public int order() {
    return Integer.MIN_VALUE + 10;
  }

  @Override
  public void start() {
    scan();
    ioc();
    merge();
  }

  @Override
  public void stop() {
    BeanStore.BEANS.clear();
    TODO_INJECT.set(0);
  }

  private void scan() {
    // Configuration
    for (Class<?> clazz : ClassScannerPlugin.getTypesAnnotatedWith(Configuration.class)) {
      configBeans(clazz);
    }
    // Bean
    for (Class<?> clazz : ClassScannerPlugin.getTypesAnnotatedWith(Bean.class)) {
      Bean singleton = clazz.getAnnotation(Bean.class);
      String beanName = singleton.value();
      if (beanName.length() == 0) {
        beanName = clazz.getName();
      }
      BeanStore.seize(clazz, beanName);
      TODO_INJECT.incrementAndGet();
    }
  }

  private void configBeans(Class<?> clazz) {
    Object obj;
    try {
      obj = clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(String.format("@Configuration注解的类[%s]无法实例化", clazz), e);
    }
    try {
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.isAnnotationPresent(Bean.class)) {
          if (method.getParameterCount() > 0) {
            throw new IllegalStateException("@Configuration下实例Bean不支持有参方式");
          }
          method.setAccessible(true);
          Object bean = method.invoke(obj);
          if (method.isAnnotationPresent(ValueConfig.class)) {
            ConfigFactory.load(bean, method.getAnnotation(ValueConfig.class).value());
          } else {
            ConfigFactory.load(bean);
          }
          Bean singleton = method.getAnnotation(Bean.class);
          String name = singleton.value();
          if (name.length() == 0) {
            name = method.getName();
          }
          BeanStore.putBean(name, bean);
        }
      }
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException("@Configuration下实例方法出错", e);
    }
  }

  void ioc() {
    int pre = TODO_INJECT.get();
    while (TODO_INJECT.get() > 0) {
      instance();
      int now = TODO_INJECT.get();
      if (now > 0 && now == pre) {
        if (!STRATEGY.isRequired()) {
          if (log.isDebugEnabled()) {
            log.debug("ioc失败 出现循环依赖或缺失Bean TODO_INJECT={}, beans={}", now, BeanStore.BEANS);
          }
          throw new IllegalStateException("发生循环依赖或者缺失Bean ");
        } else {
          STRATEGY.nonRequired();
        }
      }
      pre = now;
    }
  }

  private void instance() {
    BeanStore.BEANS.forEach((clazz, value) -> value.forEach((name, v) -> {
      if (v == BeanStore.EMPTY) {
        Object inst = instanceBean(clazz);
        if (inst != null) {
          BeanStore.putBean(name, inst);
          TODO_INJECT.decrementAndGet();
        }
      }
    }));
  }

  private void merge() {
    for (Entry<Class<?>, ConcurrentMap<String, Object>> entry : BeanStore.BEANS.entrySet()) {
      Class<?> clazz = entry.getKey();
      BeanStore.merge(clazz, entry.getValue().values().iterator().next());
    }
  }

}
