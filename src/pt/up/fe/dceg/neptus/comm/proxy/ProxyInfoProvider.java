/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 10 de Mai de 2012
 */
package pt.up.fe.dceg.neptus.comm.proxy;

import java.awt.Window;
import java.io.File;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.comm.ssh.SSHConnectionDialog;

/**
 * @author pdias
 *
 */
public class ProxyInfoProvider {

    @NeptusProperty
    public static boolean enableProxy = false;
    @NeptusProperty
    public static String httpProxyHost = "localhost";
    @NeptusProperty
    public static short httpProxyPort = 8080;

    @NeptusProperty
    public static String username = "user";
    private static String password = null;
    
    private static final String ROOT_PREFIX;
    static {
        if (new File("../" + "conf").exists())
            ROOT_PREFIX = "../";
        else {
            ROOT_PREFIX = "";
            new File("conf").mkdir();
        }
    }
    
    static {
        try {
            String confFx = ROOT_PREFIX + "conf/" + ProxyInfoProvider.class.getSimpleName().toLowerCase() + ".properties";
            if (new File(confFx).exists())
                PluginUtils.loadProperties(confFx, ProxyInfoProvider.class);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Not possible to open \"conf/" + ProxyInfoProvider.class.getSimpleName().toLowerCase()
                    + ".properties\"");
        }
    }

    private ProxyInfoProvider() {
        // Don't allow initialization
    }
    
    private static synchronized void savePropertiesToDisk() {
        try {
            PluginUtils.saveProperties(ROOT_PREFIX + "conf/" + ProxyInfoProvider.class.getSimpleName().toLowerCase() + ".properties",
                    ProxyInfoProvider.class);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Not possible to open \"conf/" + ProxyInfoProvider.class.getSimpleName().toLowerCase()
                    + ".properties\"");
        }
    }

    /**
     * @return the enableProxy
     */
    public static boolean isEnableProxy() {
        return enableProxy;
    }
    
    /**
     * @param enableProxy the enableProxy to set
     */
    public static void setEnableProxy(boolean enableProxy) {
        ProxyInfoProvider.enableProxy = enableProxy;
        savePropertiesToDisk();
    }
    
    /**
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    public static String[] showConfigurations() {
        return showOrNotConfiguratonDialogAndReturnConfigurationWorker(I18n.text("Proxy Configuration"), true, null);
    }

    /**
     * @param parentWindow
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    public static String[] showConfigurations(Window parentWindow) {
        return showOrNotConfiguratonDialogAndReturnConfigurationWorker(I18n.text("Proxy Configuration"), true, parentWindow);
    }

    /**
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    public static String[] getConfiguratons() {
        return enableProxy ? showOrNotConfiguratonDialogAndReturnConfigurationWorker(I18n.text("Proxy Configuration"), false, null)
                : new String[0];
    }

    /**
     * @param parentWindow
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    public static String[] getConfiguratons(Window parentWindow) {
        return enableProxy ? showOrNotConfiguratonDialogAndReturnConfigurationWorker(I18n.text("Proxy Configuration"), false,
                parentWindow) : new String[0];
    }

    /**
     * @param title
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    private synchronized static String[] showOrNotConfiguratonDialogAndReturnConfigurationWorker(String title,
            boolean forceShow, Window parentWindow) {
        
        if (forceShow || password == null) {
            String[] ret = SSHConnectionDialog.showConnectionDialog(httpProxyHost, username, password == null ? ""
                    : password, httpProxyPort, title, parentWindow);
            if (ret.length == 0)
                return new String[0];

            httpProxyHost = ret[0];
            username = ret[1];
            password = ret[2];
            try {
                httpProxyPort = Short.parseShort(ret[3]);
            }
            catch (NumberFormatException e) {
                httpProxyPort = (short) 80;
            }

            savePropertiesToDisk();
        }
        
        return new String[] { httpProxyHost, username, password, Short.toString(httpProxyPort) };
    }

    /**
     * @param client to add a route planner
     */
    public static void setRoutePlanner(final AbstractHttpClient client) {
        client.setRoutePlanner(new HttpRoutePlanner() {
            public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context)
                    throws HttpException {
                String[] ret = getConfiguratons();
                if (ret.length == 0)
                    return new HttpRoute(target, null, target, "https".equalsIgnoreCase(target.getSchemeName()));

                String proxyHost = ret[0];
                short proxyPort = Short.parseShort(ret[3]);
                String username = ret[1];
                String password = ret[2];

                client.getCredentialsProvider().setCredentials(new AuthScope(proxyHost, proxyPort),
                        new UsernamePasswordCredentials(username, password));

                return new HttpRoute(target, null, new HttpHost(proxyHost, proxyPort), "https".equalsIgnoreCase(target
                        .getSchemeName()));
            }
        });
    }

    public static Credentials getProxyCredentials() {
        String[] ret = getConfiguratons();
        if (ret.length == 0)
            return new UsernamePasswordCredentials("", "");

//        String proxyHost = ret[0];
//        short proxyPort = Short.parseShort(ret[3]);
        String username = ret[1];
        String password = ret[2];

        return new UsernamePasswordCredentials(username, password);
    }

    /**
     * @param resp
     * @param localContext
     */
    public static void authenticateConnectionIfNeeded(HttpResponse resp, HttpContext localContext, DefaultHttpClient client) {
        {
            if (isEnableProxy()) {
                AuthState proxyAuthState = (AuthState) localContext
                        .getAttribute(ClientContext.PROXY_AUTH_STATE);
                if (proxyAuthState != null) {
                    // System.out.println("Proxy auth state: " + proxyAuthState.getState());
                    if (proxyAuthState.getAuthScheme() != null)
                        System.out.println("Proxy auth scheme: " + proxyAuthState.getAuthScheme());
                    if (proxyAuthState.getCredentials() != null)
                        System.out.println("Proxy auth credentials: " + proxyAuthState.getCredentials());
                }
                AuthState targetAuthState = (AuthState) localContext
                        .getAttribute(ClientContext.TARGET_AUTH_STATE);
                if (targetAuthState != null) {
                    // System.out.println("Target auth state: " + targetAuthState.getState());
                    if (targetAuthState.getAuthScheme() != null)
                        System.out.println("Target auth scheme: " + targetAuthState.getAuthScheme());
                    if (targetAuthState.getCredentials() != null)
                        System.out.println("Target auth credentials: " + targetAuthState.getCredentials());
                }
            }
        }
        
        { // New Authentication for httpcomponents-client-4.2
            if (isEnableProxy()) {
                int sc = resp.getStatusLine().getStatusCode();
                
                AuthState authState = null;
                HttpHost authhost = null;
                if (sc == HttpStatus.SC_UNAUTHORIZED) {
                    // Target host authentication required
                    authState = (AuthState) localContext.getAttribute(ClientContext.TARGET_AUTH_STATE);
                    authhost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                }
                if (sc == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                    // Proxy authentication required
                    authState = (AuthState) localContext.getAttribute(ClientContext.PROXY_AUTH_STATE);
                    authhost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_PROXY_HOST);
                }
                if (authState != null) {
                    AuthScheme authscheme = authState.getAuthScheme();
                    System.out.println("Using proxy for " + authscheme.getRealm() + " ...");
                    Credentials creds = getProxyCredentials();
                    client.getCredentialsProvider().setCredentials(new AuthScope(authhost), creds);
                }
            }
        }
    }
    
    public static void main(String[] args) {
        savePropertiesToDisk();
        
        System.out.println(enableProxy);
        System.out.println(httpProxyHost);
        System.out.println(httpProxyPort);
        System.out.println(username);
        System.out.println(password);
    }
}
