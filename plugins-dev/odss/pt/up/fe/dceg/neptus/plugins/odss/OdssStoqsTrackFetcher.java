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
 * 6 de Jul de 2012
 */
package pt.up.fe.dceg.neptus.plugins.odss;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JCheckBoxMenuItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.comm.proxy.ProxyInfoProvider;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.CheckMenuChangeListener;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.odss.track.PlatformReportType;
import pt.up.fe.dceg.neptus.plugins.odss.track.PlatformReportType.PlatformType;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.systems.external.ExternalSystem;
import pt.up.fe.dceg.neptus.systems.external.ExternalSystem.ExternalTypeEnum;
import pt.up.fe.dceg.neptus.systems.external.ExternalSystemsHolder;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.conf.IntegerMinMaxValidator;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "ODSS STOQS Track Fetcher", author = "Paulo Dias", version = "0.1",
        icon="pt/up/fe/dceg/neptus/plugins/odss/odss.png")
public class OdssStoqsTrackFetcher extends SimpleSubPanel implements IPeriodicUpdates, ConfigurationListener {

    /*
     * http://beach.mbari.org/trackingdb/position/seacon-5/last/24h/data.html
     * http://beach.mbari.org/trackingdb/positionOfType/auv/last/24h/data.html
     * http://beach.mbari.org/trackingdb/positionOfType/glider/last/24h/data.html
     */

//    private static final String POS_URL_FRAGMENT = "position/";
    private static final String POS_OF_URL_FRAGMENT = "positionOfType/";
    private static final String LAST_URL_FRAGMENT = "last/";
    private static final String AUV_URL_FRAGMENT = "auv/";
    private static final String AIS_URL_FRAGMENT = "uav/";
    private static final String GLIDER_URL_FRAGMENT = "glider/";
    private static final String DRIFTER_URL_FRAGMENT = "drifter/";
    private static final String SHIP_URL_FRAGMENT = "ship/";
    
    @NeptusProperty(name = "Web address")
    public String fetchURL = "http://beach.mbari.org/trackingdb/";

    @NeptusProperty(name = "AUV", category = "System Filter")
    public boolean fetchAUVType = true;

    @NeptusProperty(name = "Glider", category = "System Filter")
    public boolean fetchGliderType = true;

    @NeptusProperty(name = "Drifter", category = "System Filter")
    public boolean fetchDrifterType = true;

    @NeptusProperty(name = "Ship", category = "System Filter")
    public boolean fetchShipType = true;

    @NeptusProperty(name = "AIS", category = "System Filter")
    public boolean fetchAISType = true;
    
    @NeptusProperty(name = "Period to fetch (hours)")
    public short periodHoursToFetch = 3;

    @NeptusProperty(name = "Update period (ms)", description = "The period to fetch the systems' positions. "
            + "Zero means disconnected.")
    public int updatePeriodMillis = 1000;

    @NeptusProperty(name = "Publishing")
    public boolean fetchOn = false;

    public boolean publishRemoteSystemsLocally = true;

    public boolean debugOn = false;

//    private long lastFetchPosTimeMillis = System.currentTimeMillis();

    private JCheckBoxMenuItem startStopCheckItem = null;
//    private ToolbarButton sendEnableDisableButton;

    private DefaultHttpClient client;
    private PoolingClientConnectionManager httpConnectionManager; //old ThreadSafeClientConnManager
    private HttpGet getHttpRequestState;
//    private HashSet<HttpRequestBase> listActiveGetMethods = new HashSet<HttpRequestBase>();

    private Timer timer = null;
    private TimerTask ttask = null;

//    private LinkedHashMap<String, Long> timeSysList = new LinkedHashMap<String, Long>();
//    private LinkedHashMap<String, CoordinateSystem> locSysList = new LinkedHashMap<String, CoordinateSystem>();
    private final HashMap<String, PlatformReportType> sysStokesLocations = new HashMap<String, PlatformReportType>();

    private DocumentBuilderFactory docBuilderFactory;
    {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setNamespaceAware(false);
    }

    /**
     * 
     */
    public OdssStoqsTrackFetcher(ConsoleLayout console) {
        super(console);
        initializeComm();
        initialize();
    }

    /**
     * 
     */
    private void initializeComm() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
        httpConnectionManager = new PoolingClientConnectionManager(schemeRegistry);
        httpConnectionManager.setMaxTotal(4);
        httpConnectionManager.setDefaultMaxPerRoute(50);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 5000);
        client = new DefaultHttpClient(httpConnectionManager, params);
        
        ProxyInfoProvider.setRoutePlanner(client);
    }

    /**
     * 
     */
    private void initialize() {
        setVisibility(false);

//        removeAll();
//        setBackground(new Color(255, 255, 110));

        timer = new Timer(OdssStoqsTrackFetcher.class.getSimpleName() + " [" + OdssStoqsTrackFetcher.this.hashCode() + "]");
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, updatePeriodMillis);
    }

    private TimerTask getTimerTask() {
        if (ttask == null) {
            ttask = new TimerTask() {
                @Override
                public void run() {
                    if (!fetchOn)
                        return;
                    getStateRemoteData();
                }
            };
        }
        return ttask;
    }

    @Override
    public void initSubPanel() {
        setVisibility(false);
        
        startStopCheckItem = addCheckMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass())
                + ">Start/Stop", null,
                new CheckMenuChangeListener() {
                    @Override
                    public void menuUnchecked(ActionEvent e) {
                        fetchOn = false;
                    }
                    
                    @Override
                    public void menuChecked(ActionEvent e) {
                        fetchOn = true;
                    }
                });

        addMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass()) + ">Settings", null,
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PropertiesEditor.editProperties(OdssStoqsTrackFetcher.this,
                                getConsole(), true);
                    }
                });
        
        startStopCheckItem.setState(fetchOn);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        removeCheckMenuItem("Settings>" + PluginUtils.getPluginName(this.getClass()) + ">Start/Stop");
        removeMenuItem("Settings>" + PluginUtils.getPluginName(this.getClass()) + ">Settings");
        
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * 
     */
    protected void getStateRemoteData() {
        if (getHttpRequestState != null)
            getHttpRequestState.abort();
        getHttpRequestState = null;
        
        // http://beach.mbari.org/trackingdb/positionOfType/auv/last/24h/data.html
        HashMap<String, PlatformReportType.PlatformType> typeRqstLst = new LinkedHashMap<String, PlatformReportType.PlatformType>();
        if (fetchAUVType)
            typeRqstLst.put(AUV_URL_FRAGMENT, PlatformType.AUV);
        if (fetchGliderType)
            typeRqstLst.put(GLIDER_URL_FRAGMENT, PlatformType.GLIDER);
        if (fetchDrifterType)
            typeRqstLst.put(DRIFTER_URL_FRAGMENT, PlatformType.DRIFTER);
        if (fetchShipType)
            typeRqstLst.put(SHIP_URL_FRAGMENT, PlatformType.SHIP);
        if (fetchAISType)
            typeRqstLst.put(AIS_URL_FRAGMENT, PlatformType.AIS);

        for (String typeRqst : typeRqstLst.keySet()) {
            try {
                String endpoint = fetchURL;
                String uri = endpoint + POS_OF_URL_FRAGMENT + typeRqst + LAST_URL_FRAGMENT + periodHoursToFetch + "h/data.html";
                getHttpRequestState = new HttpGet(uri);

                HttpContext localContext = new BasicHttpContext();
                HttpResponse iGetResultCode = client.execute(getHttpRequestState, localContext);
                ProxyInfoProvider.authenticateConnectionIfNeeded(iGetResultCode, localContext, client);
                
                if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    System.out.println(OdssStoqsTrackFetcher.this.getClass().getSimpleName() 
                            + "[" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                            + iGetResultCode.getStatusLine().getReasonPhrase()
                            + " code was return from the server");
                    if (getHttpRequestState != null) {
                        getHttpRequestState.abort();
                    }
                    continue;
                }
                InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
                @SuppressWarnings("unused")
                long fullSize = iGetResultCode.getEntity().getContentLength();
                if (iGetResultCode.getEntity().isChunked())
                    fullSize = iGetResultCode.getEntity().getContentLength();
                DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
                Document docProfiles = builder.parse(streamGetResponseBody);
                
                HashMap<String, PlatformReportType> sysBag = processOdssStokesResponse(docProfiles, typeRqstLst.get(typeRqst));
                filterAndAddToList(sysBag);
                if (debugOn) {
                    for (String key : sysBag.keySet()) {
                        System.out.println(sysBag.get(key));
                    }
                }

                sysStokesLocations.putAll(sysBag);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            finally {
                if (getHttpRequestState != null) {
                    getHttpRequestState.abort();
                    getHttpRequestState = null;
                }
            }
        }
        
        processRemoteStates();
        
        return;
    }

    /**
     * @param sysBag
     */
    static void filterAndAddToList(HashMap<String, PlatformReportType> sysBag) {
        if (sysBag.size() == 0)
            return;
        
        String[] vehicles = VehiclesHolder.getVehiclesArray();
        ImcSystem[] imcSyss = ImcSystemsHolder.lookupAllSystems();
        String[] imcReduced = new String[vehicles.length + imcSyss.length];
        for (int i = 0; i < vehicles.length; i++) {
            imcReduced[i] = IMCUtils.reduceSystemName(vehicles[i]);
        }
        for (int i = 0; i < imcSyss.length; i++) {
            imcReduced[i + vehicles.length] = IMCUtils.reduceSystemName(imcSyss[i].getName());
        }
        List<String> lstReducedLst = Arrays.asList(imcReduced);
        for (String key : sysBag.keySet().toArray(new String[0])) {
            if (ImcSystemsHolder.lookupSystemByName(key) != null)
                continue;
            else if (lstReducedLst.contains(key)) {
                int idx = lstReducedLst.indexOf(key);
                PlatformReportType pr = sysBag.get(key);
                sysBag.remove(key);
                String newName = idx < vehicles.length ? vehicles[idx] : imcSyss[idx].getName();
                pr = pr.cloneWithName(newName);
                sysBag.put(newName, pr);
            }
        }
    }

    /**
     * @param timeSysList2
     * @param locSysList2
     * @param docProfiles
     * @return
     */
    private HashMap<String, PlatformReportType> processOdssStokesResponse(Document docProfiles, PlatformReportType.PlatformType type) {
        HashMap<String, PlatformReportType> dataBag = new HashMap<String, PlatformReportType>();
        
        Element root = docProfiles.getDocumentElement();
        NodeList elem = root.getElementsByTagName("table");
        for (int i = 0; i < elem.getLength(); i++) {
            Node tableNd = elem.item(i);
            Node tbodyNd = null;
            Node bn = tableNd.getFirstChild();
            while (bn != null) {
                if ("tbody".equalsIgnoreCase(bn.getNodeName())) {
                    tbodyNd = bn;
                    break;
                }
                else if ("tr".equalsIgnoreCase(bn.getNodeName())) {
                    tbodyNd = tableNd;
                    break;
                }
                bn = bn.getNextSibling();
            }
            if (tbodyNd != null) {
                NodeList trNdLst = ((Element) tbodyNd).getElementsByTagName("tr");
                if (trNdLst.getLength() > 0) {
                    //find header
                    boolean hasHeader = false;
                    Node trNode = trNdLst.item(0);
                    NodeList trThNdLst = ((Element) trNode).getElementsByTagName("th");
                    if (trThNdLst.getLength() > 0) {
                        hasHeader = true;
                        // Platform Name, Epoch Seconds, Longitude, Latitude, ISO-8601
                        for (int j = 0; j < trThNdLst.getLength(); j++) {
                            Element thElm = (Element) trThNdLst.item(j);
                            String txt = thElm.getTextContent().trim();
                            if (debugOn)
                                System.out.print(txt + (j == trThNdLst.getLength() - 1 ? "\n" : "\t"));
                        }
                    }
                    for (int j = hasHeader ? 1 : 0; j < trNdLst.getLength(); j++) {
                        NodeList trTdNdLst = ((Element) trNdLst.item(j)).getElementsByTagName("td");
                        if (trTdNdLst.getLength() > 0) {
                            // Platform Name, Epoch Seconds, Longitude, Latitude, ISO-8601
                            String name = null;
                            long unixTimeSeconds = -1;
                            double lat = Double.NaN, lon = Double.NaN;
                            boolean allDataIn = false;
                            try {
                                for (int k = 0; k < trTdNdLst.getLength(); k++) {
                                    Element tdElm = (Element) trTdNdLst.item(k);
                                    String txt = tdElm.getTextContent().trim();
                                    if (debugOn)
                                        System.out.print(txt + (k == trTdNdLst.getLength() - 1 ? "\n" : "\t"));
                                    switch (k) {
                                        case 0:
                                            name = txt;
                                            break;
                                        case 1:
                                            unixTimeSeconds = (long) Double.parseDouble(txt);
                                            break;
                                        case 2:
                                            lon = Double.parseDouble(txt);
                                            break;
                                        case 3:
                                            lat = Double.parseDouble(txt);
                                            allDataIn = true;
                                            break;
                                    }
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (allDataIn) {
                                PlatformReportType dfe = dataBag.get(name);
                                if (dfe == null || dfe.getEpochSeconds() <= unixTimeSeconds) {
                                    PlatformReportType pr = new PlatformReportType(name, type);
                                    pr.setLocation(lat, lon, unixTimeSeconds);
                                    dataBag.put(name, pr);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (debugOn) {
            for (String key : dataBag.keySet()) {
                System.out.println(dataBag.get(key));
            }
        }
        
        return dataBag;
    }

    private void processRemoteStates() {
        if (publishRemoteSystemsLocally) {
            processRemoteStates(sysStokesLocations, DateTimeUtil.HOUR * periodHoursToFetch);
        }
    }

    static void processRemoteStates(HashMap<String, PlatformReportType> sysStokesLocations, long timeMillisToDiscart) {
        for (String key : sysStokesLocations.keySet().toArray(new String[0])) {
            String id = key;
            PlatformReportType pr = sysStokesLocations.get(key);
            if (pr == null)
                continue;

            long time = Math.round(pr.getEpochSeconds() * 1000d);
            CoordinateSystem coordinateSystem = new CoordinateSystem();
            coordinateSystem.setLocation(pr.getHasLocation());
//            System.out.println(key + " :: " +coordinateSystem);

            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(id);
            if (sys != null) {
                if ((System.currentTimeMillis() - time < timeMillisToDiscart) && sys.getLocationTimeMillis() < time) {
                    if (coordinateSystem.getLatitudeAsDoubleValue() != 0d && coordinateSystem.getLongitudeAsDoubleValue() != 0d) {
                        sys.setLocation(coordinateSystem, time);

                        if (coordinateSystem.getRoll() != 0d && coordinateSystem.getPitch() != 0d
                                && coordinateSystem.getYaw() != 0d) {
                            sys.setAttitudeDegrees(coordinateSystem.getRoll(), coordinateSystem.getPitch(),
                                    coordinateSystem.getYaw(), time);
                        }
                        sys.storeData(ImcSystem.WEB_UPDATED_KEY, true, time, true);
                    }
                }
            }
            else {
                ExternalSystem ext = ExternalSystemsHolder.lookupSystem(id);
                boolean registerNewExternal = false;
                if (ext == null) {
                    registerNewExternal = true;
                    ext = new ExternalSystem(id);
                    SystemTypeEnum type = SystemTypeEnum.UNKNOWN;
                    if (pr.getType() == PlatformType.AUV) {
                        ext.setType(SystemTypeEnum.VEHICLE);
                        ext.setTypeVehicle(VehicleTypeEnum.UUV);
                    }
                    else if (pr.getType() == PlatformType.DRIFTER) {
                        ext.setType(SystemTypeEnum.MOBILESENSOR);
                    } 
                    else if (pr.getType() == PlatformType.GLIDER) {
                        ext.setType(SystemTypeEnum.VEHICLE);
                        ext.setTypeVehicle(VehicleTypeEnum.UUV);
                    } 
                    else if (pr.getType() == PlatformType.MOORING) {
                        ext.setType(SystemTypeEnum.STATICSENSOR);
                    } 
                    else if (pr.getType() == PlatformType.AIS) {
                        ext.setType(SystemTypeEnum.UNKNOWN);
                        ext.setTypeExternal(ExternalTypeEnum.MANNED_SHIP);
                    } 
                    else if (pr.getType() == PlatformType.SHIP) {
                        ext.setType(SystemTypeEnum.UNKNOWN);
                        ext.setTypeExternal(ExternalTypeEnum.MANNED_SHIP);
                    } 
                    ext.setType(type);
                    
                    // See better because this should not be here
                    ext.setLocation(coordinateSystem, time);
                    ExternalSystemsHolder.registerSystem(ext);
                }
                if ((System.currentTimeMillis() - time < timeMillisToDiscart) && ext.getLocationTimeMillis() < time) {
                    if (coordinateSystem.getLatitudeAsDoubleValue() != 0d && coordinateSystem.getLongitudeAsDoubleValue() != 0d) {
                        ext.setLocation(coordinateSystem, time);

                        if (coordinateSystem.getRoll() != 0d && coordinateSystem.getPitch() != 0d
                                && coordinateSystem.getYaw() != 0d) {
                            ext.setAttitudeDegrees(coordinateSystem.getRoll(), coordinateSystem.getPitch(),
                                    coordinateSystem.getYaw(), time);
                        }
                        
                        if (registerNewExternal)
                            ExternalSystemsHolder.registerSystem(ext);
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (!fetchURL.endsWith("/"))
            fetchURL += "/";

        abortAllActiveConnections();
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, updatePeriodMillis);
    }

    public String validateFetchURL(String value) {
        if (!fetchURL.endsWith("/"))
            fetchURL += "/";
        try {
            new URL(fetchURL);
            return null;
        }
        catch (MalformedURLException e) {
            return e.getMessage();
        }
    }
    
    public String validatePeriodHoursToFetch(short value) {
        String ret = new IntegerMinMaxValidator(1, 300, true, true).validate(value);
        if (value < 1)
            value = periodHoursToFetch = 1;
        if (value > 300)
            value = periodHoursToFetch = 300;
        
        return ret;
    }
    
    public String validateUpdatePeriodMillis(int value) {
        String ret = new IntegerMinMaxValidator(500, (int) (DateTimeUtil.MINUTE * 10), true, true).validate(value);
        if (value < 500)
            value = updatePeriodMillis = 500;
        if (value > DateTimeUtil.MINUTE * 10)
            value = updatePeriodMillis = (int) (DateTimeUtil.MINUTE * 10);
        
        return ret;
    }

    private void abortAllActiveConnections() {
        try {
            if (getHttpRequestState != null)
                getHttpRequestState.abort();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates
     * ()
     */
    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
//        refreshUI();

        if (startStopCheckItem != null)
            startStopCheckItem.setState(fetchOn);

        if (!fetchOn)
            abortAllActiveConnections();
        
        return true;
    }
    
    // Use for debug
    private Document loadXml(String xml) throws Exception {
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Document docProfiles = 
                //builder.parse("http://beach.mbari.org/trackingdb/positionOfType/auv/last/300h/data.html");
                builder.parse(bais);
        return docProfiles;
    }

    public static void main(String[] args) {
        try {
            VehiclesHolder.loadVehicles();
            ImcMsgManager.getManager().start();
            ImcMsgManager.getManager().initVehicleCommInfo("lauv-seacon-1", "127.0.0.1");

            System.out.println("ImcSystemsHolder:   " + ImcSystemsHolder.lookupAllSystems().length);
            System.out.println("ExternalSystemsHolder: " + ExternalSystemsHolder.lookupAllSystems().length);

            OdssStoqsTrackFetcher osf = new OdssStoqsTrackFetcher(null);
            osf.debugOn = true;
            osf.publishRemoteSystemsLocally = true;
            osf.periodHoursToFetch = 300;
            
            
            System.out.println("\n\n-------------- Use remote requests  -------------- \n");
            osf.getStateRemoteData();
            System.out.println("ImcSystemsHolder:   " + ImcSystemsHolder.lookupAllSystems().length);
            System.out.println("ExternalSystemsHolder: " + ExternalSystemsHolder.lookupAllSystems().length);

            System.out.println("\n\n-------------- Use local test files -------------- \n");
            
            String xml = FileUtil.getFileAsString("srcTests/mbari/ODSS-Position-2.html");
            Document docProfiles = osf.loadXml(xml);
            
            HashMap<String, PlatformReportType> sysBag = osf.processOdssStokesResponse(docProfiles, PlatformType.AUV);
            filterAndAddToList(sysBag);
            for (String key : sysBag.keySet()) {
                System.out.println(sysBag.get(key));
            }
            osf.sysStokesLocations.putAll(sysBag);
            osf.processRemoteStates();
            
            System.out.println("ImcSystemsHolder:   " + ImcSystemsHolder.lookupAllSystems().length);
            System.out.println("ExternalSystemsHolder: " + ExternalSystemsHolder.lookupAllSystems().length);
            
            xml = FileUtil.getFileAsString("srcTests/mbari/ODSS-Position-glider.html");
            docProfiles = osf.loadXml(xml);
            
            sysBag = osf.processOdssStokesResponse(docProfiles, PlatformType.GLIDER);
            filterAndAddToList(sysBag);
            for (String key : sysBag.keySet()) {
                System.out.println(sysBag.get(key));
            }
            osf.sysStokesLocations.putAll(sysBag);
            osf.processRemoteStates();
            
            System.out.println("ImcSystemsHolder:     " + ImcSystemsHolder.lookupAllSystems().length);
            System.out.println("ExternalSystemsHolder: " + ExternalSystemsHolder.lookupAllSystems().length);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
