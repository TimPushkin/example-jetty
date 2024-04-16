package com.example;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.component.LifeCycle;

class ServerManager implements Resource {
    Server server;

    public ServerManager(int port, Handler handler) throws Exception {
        server = new Server(port);
        server.setHandler(handler);
        server.start();
        Core.getGlobalContext().register(this);
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) {
        // Stop the connectors only and keep the expensive application running
        Arrays.asList(server.getConnectors()).forEach(c -> LifeCycle.stop(c));
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {
        Arrays.asList(server.getConnectors()).forEach(c -> LifeCycle.start(c));
    }
}

public class App extends AbstractHandler
{
    static ServerManager serverManager;

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println("Hello World");
    }

    public static void main( String[] args ) throws Exception {
        serverManager = new ServerManager(8080, new App());
        serverManager.server.join();
    }
}
