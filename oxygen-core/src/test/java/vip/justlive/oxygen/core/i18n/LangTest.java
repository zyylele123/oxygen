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
package vip.justlive.oxygen.core.i18n;

import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import vip.justlive.oxygen.core.Bootstrap;

/**
 * @author wubo
 */
public class LangTest {

  @Before
  public void before() {
    Bootstrap.start();
  }

  @Test
  public void getMessage() {
    Assert.assertEquals("key_zh_CN", Lang.getMessage("i18n.k1"));
    Lang.setThreadLocale(new Locale("en", "US"));
    Assert.assertEquals("key_en_US", Lang.getMessage("i18n.k1"));
    Lang.setThreadLocale(new Locale("en"));
    Assert.assertEquals("key", Lang.getMessage("i18n.k1"));

  }

  @Test
  public void getMessage1() {
    Assert.assertEquals("key2_en_US", Lang.getMessage("i18n.k2", new Locale("en", "US")));
    Assert.assertNull(Lang.getMessage("i18n.k2"));
  }

  @Test
  public void getMessage2() {
    Assert.assertEquals("key2_en_US", Lang.getMessage("i18n.k2", "en", "US"));
    Assert.assertNull(Lang.getMessage("i18n.k2"));
  }
}