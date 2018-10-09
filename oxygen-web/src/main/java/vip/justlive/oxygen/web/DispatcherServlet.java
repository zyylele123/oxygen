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
package vip.justlive.oxygen.web;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import vip.justlive.oxygen.core.exception.Exceptions;
import vip.justlive.oxygen.web.http.Request;
import vip.justlive.oxygen.web.http.RequestParse;
import vip.justlive.oxygen.web.http.Response;
import vip.justlive.oxygen.web.mapping.Action;
import vip.justlive.oxygen.web.mapping.Request.HttpMethod;
import vip.justlive.oxygen.web.view.ViewResolver;

/**
 * 请求分发器
 *
 * @author wubo
 */
@Slf4j
public class DispatcherServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private void doService(HttpServletRequest req, HttpServletResponse resp, HttpMethod httpMethod) {
    String requestPath = getRequestPath(req);
    if (log.isDebugEnabled()) {
      log.debug("DispatcherServlet accept request for [{}] on method [{}]", requestPath,
          httpMethod);
    }

    String contentPath = req.getContextPath();
    if (contentPath.length() > 0 && requestPath.startsWith(contentPath)) {
      requestPath = requestPath.substring(contentPath.length());
    }
    Action action = WebPlugin.findActionByPath(requestPath, httpMethod);
    if (action == null) {
      handlerNotFound(req, resp);
      return;
    }

    handlerAction(action, req, resp);
  }

  void handlerAction(Action action, HttpServletRequest req, HttpServletResponse resp) {
    Request.set(req);
    Response.set(resp);

    for (RequestParse requestParse : WebPlugin.REQUEST_PARSES) {
      if (requestParse.supported(req)) {
        requestParse.handle(req);
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("Request parsed -> {}", JSON.toJSONString(Request.current()));
    }

    try {
      Object result = action.invoke();
      if (action.needRenderView()) {
        ViewResolver viewResolver = WebPlugin.findViewResolver(result);
        if (viewResolver == null) {
          hanlderNoViewResolver(resp);
          return;
        }
        viewResolver.resolveView(req, resp, result);
      }
    } catch (Exception e) {
      handlerError(req, resp, e);
    } finally {
      Request.clear();
      Response.clear();
    }

  }

  private void hanlderNoViewResolver(HttpServletResponse resp) {
    String msg = String
        .format("no ViewResolver found in container for [%s]", Request.current().getPath());
    log.error(msg);
    try {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
    } catch (IOException e) {
      throw Exceptions.wrap(e);
    }
  }

  private void handlerNotFound(HttpServletRequest req, HttpServletResponse resp) {
    if (log.isDebugEnabled()) {
      log.debug("DispatcherServlet not found path [{}] on method [{}]", getRequestPath(req),
          req.getMethod());
    }
    try {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (IOException e) {
      throw Exceptions.wrap(e);
    }
  }

  private void handlerError(HttpServletRequest req, HttpServletResponse resp, Exception e) {
    log.error("DispatcherServlet occurs an error for path [{}]", getRequestPath(req), e);
    try {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (IOException ie) {
      throw Exceptions.wrap(ie);
    }
  }

  private String getRequestPath(HttpServletRequest req) {
    return req.getRequestURI();
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (HttpMethod.PATCH.name().equals(req.getMethod())) {
      doService(req, resp, HttpMethod.PATCH);
    } else {
      super.service(req, resp);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    doService(req, resp, HttpMethod.GET);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    doService(req, resp, HttpMethod.POST);
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
    doService(req, resp, HttpMethod.DELETE);
  }

  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
    doService(req, resp, HttpMethod.HEAD);
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
    doService(req, resp, HttpMethod.PUT);
  }

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
    doService(req, resp, HttpMethod.OPTIONS);
  }


  @Override
  protected void doTrace(HttpServletRequest req, HttpServletResponse resp) {
    doService(req, resp, HttpMethod.TRACE);
  }


}