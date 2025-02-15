package wxdgaming.boot2.starter.net.server.http;

import java.lang.reflect.Method;

/**
 * http 请求接口过滤器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-13 16:32
 **/
public abstract class HttpFilter {

    public abstract boolean doFilter(String url, Method method, HttpContext httpContext);

}
