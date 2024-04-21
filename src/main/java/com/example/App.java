package com.example;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

class ServerManager implements Resource {
    Server server;

    private static int getPort() {
        return Integer.parseInt(System.getProperty("port", "8080"));
    }

    public ServerManager(Handler handler) throws Exception {
        server = new Server(getPort());
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
        Arrays.asList(server.getConnectors()).forEach(c -> {
            if (c instanceof ServerConnector) ((ServerConnector) c).setPort(getPort());
            LifeCycle.start(c);
        });
    }
}

public class App extends AbstractHandler
{
    private static final Logger LOG = Log.getLogger(App.class);

    static ServerManager serverManager;
    static boolean shouldCheckpoint = false;

    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static final Date launchDate = new Date(System.currentTimeMillis());
    private static Date restoreDate = null;

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException {
        LOG.info("Handling target: " + target);
        switch (target) {
            case "/":
                response.setContentType("text/html; charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                final var currentDate = new Date(System.currentTimeMillis());
                final var writer = response.getWriter();
                writer.print("<p>");
                writer.println("Launched: " + dateFormat.format(launchDate));
                writer.print("<br>");
                writer.println("Restored: " + (restoreDate != null ? dateFormat.format(restoreDate) : "never"));
                writer.print("<br>");
                writer.println("Now: " + dateFormat.format(currentDate));
                writer.print("</p>");
                baseRequest.setHandled(true);
                break;
            case "/checkpoint":
                response.setContentType("text/html; charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("<p>Checkpointing...</p>");
                baseRequest.setHandled(true);
                shouldCheckpoint = true;
                synchronized (serverManager) {
                    serverManager.notifyAll();
                }
                break;
        }
    }

    public static void main( String[] args ) throws Exception {
        serverManager = new ServerManager(new App());
        while (true) {
            synchronized (serverManager) {
                serverManager.wait();
            }
            if (shouldCheckpoint) {
                try {
                    LOG.info("Initiating checkpoint");
                    Core.checkpointRestore();
                    restoreDate = new Date(System.currentTimeMillis());
                    LOG.info("Restored");
                } catch (Exception e) {
                    LOG.warn("C/R failed", e);
                } finally {
                    shouldCheckpoint = false;
                }
            }
        }
    }
}
