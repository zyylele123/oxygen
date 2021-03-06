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

/**
 * 注入策略
 *
 * @author wubo
 */
public interface Strategy {

  /**
   * 实例对象
   *
   * @param clazz 类
   * @return 实例
   */
  Object instance(Class<?> clazz);

  /**
   * 设置非必须
   */
  void nonRequired();

  /**
   * 是否需要
   *
   * @return true需要
   */
  boolean isRequired();

}
