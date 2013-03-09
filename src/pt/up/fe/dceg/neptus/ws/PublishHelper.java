/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 25/Abr/2006
 */
package pt.up.fe.dceg.neptus.ws;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.PathProvider;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.PathElement;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author Paulo Dias
 *
 */
public class PublishHelper {
//    protected String endpoint = "http://nagoya.apache.org:5049/axis/services/echo";
//    
//    private DefaultHttpClient client;
//    private PoolingClientConnectionManager httpConnectionManager; //old ThreadSafeClientConnManager
//
//    protected static final String UPDATE_STATE_OPERATION = "updateState";
//    protected static final String UPDATE_PLAN_OPERATION  = "updatePlan";
//
//    protected Timer runner = null;
//    protected TimerTask ttask = null;
//
//    private URL serverURL;
//    private HTTPPublisher publisher = null;
//
//    protected BlinkStatusLed blinkLed = null;

    private PublishHelper() {
        
    }
        
//    public PublishHelper() {
//        endpoint = GeneralPreferences
//                .getProperty(GeneralPreferences.PUBLISH_WS_ADDRESS);
//        try {
//        	serverURL = new URL(endpoint);
//        	publisher = new HTTPPublisher(serverURL);
//        }
//        catch (Exception e) {
//        	e.printStackTrace();
//        	publisher = new HTTPPublisher();
//        }
//        
//        initializeComm();
//    }
    
//    private void initializeComm() {
//        SchemeRegistry schemeRegistry = new SchemeRegistry();
//        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
//        schemeRegistry.register(new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
//        httpConnectionManager = new PoolingClientConnectionManager(schemeRegistry);
//        httpConnectionManager.setMaxTotal(4);
//        httpConnectionManager.setDefaultMaxPerRoute(50);
//        
//        HttpParams params = new BasicHttpParams();
//        HttpConnectionParams.setConnectionTimeout(params, 5000);
//        client = new DefaultHttpClient(httpConnectionManager, params);
//        
//        ProxyInfoProvider.setRoutePlanner((AbstractHttpClient) client);
//    }
//    
//    public URL updateServerURL() throws MalformedURLException {
//    	endpoint = GeneralPreferences
//        		.getProperty(GeneralPreferences.PUBLISH_WS_ADDRESS);
//    	serverURL = new URL(endpoint);
//    	return serverURL;
//    }
//    
//
//    public String updateState(String xml) {
//        boolean doIt = Boolean.parseBoolean(GeneralPreferences
//                .getProperty(GeneralPreferences.PUBLISH_WS_STATE));
//        if (!doIt)
//            return "";
//        
//        HttpPost post = null;
//        try {
//            endpoint = GeneralPreferences
//                    .getProperty(GeneralPreferences.PUBLISH_WS_ADDRESS);
//            String uri = endpoint;
//            post = new HttpPost(uri);
//            NameValuePair nvp_type = new BasicNameValuePair("type", "state");
//            NameValuePair nvp_xml = new BasicNameValuePair("xml", xml);
//            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
//            nvps.add(nvp_type);
//            nvps.add(nvp_xml);
//
//            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
//            HttpContext localContext = new BasicHttpContext();
//            HttpResponse iGetResultCode = client.execute(post, localContext);
//            ProxyInfoProvider.authenticateConnectionIfNeeded(iGetResultCode, localContext, client);
//
//            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
//                System.out.println("[" + iGetResultCode.getStatusLine().getStatusCode() + "] "
//                        + iGetResultCode.getStatusLine().getReasonPhrase()
//                        + " code was return from the server");
//                if (post != null) {
//                    post.abort();
//                }
//                return null;
//            }
//        }
//        catch (Exception e) {
//            // e.printStackTrace();
//            NeptusLog.pub().warn(e.getMessage());
//        }
//        finally {
//            if (post != null) {
//                post.abort();
//                post = null;
//            }
//        }
//
//        return null;
//    }
//    
//    public String updatePlan(String xml) {
//        boolean doIt = Boolean.parseBoolean(GeneralPreferences
//                .getProperty(GeneralPreferences.PUBLISH_WS_PLAN));
//        if (!doIt)
//            return "";
//
//        HttpPost post = null;
//        try {
//            endpoint = GeneralPreferences
//                    .getProperty(GeneralPreferences.PUBLISH_WS_ADDRESS);
//            String uri = endpoint;
//            post = new HttpPost(uri);
//            NameValuePair nvp_type = new BasicNameValuePair("type", "plan");
//            NameValuePair nvp_xml = new BasicNameValuePair("xml", xml);
//            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
//            nvps.add(nvp_type);
//            nvps.add(nvp_xml);
//
//            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
//            HttpResponse iGetResultCode = client.execute(post);
//
//            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
//                System.out.println("[" + iGetResultCode.getStatusLine().getStatusCode() + "] "
//                        + iGetResultCode.getStatusLine().getReasonPhrase()
//                        + " code was return from the server");
//                if (post != null) {
//                    post.abort();
//                }
//                return null;
//            }
//        }
//        catch (Exception e) {
//            // e.printStackTrace();
//            NeptusLog.pub().warn(e.getMessage());
//        }
//        finally {
//            if (post != null) {
//                post.abort();
//                post = null;
//            }
//        }
//
//        return null;
//    }
//
//    public void sendPlanPath(final MissionType missionType, final PlanType plan) {
//        Thread worker = new Thread() {
//            public void run() {
//                WaitPanel wp = new WaitPanel();
//                wp.start();
//                String pathXml = createPlanPath(missionType, plan);
//                publisher.publishPlan(pathXml);
//                //updatePlan(pathXml);
//                wp.stop();
//            }
//        };
//        //SwingUtilities.invokeLater(worker);
//        worker.start();
//    }

    public static String createPlanPath(MissionType missionType, PlanType plan) {
        LocationType locStart = new LocationType(), startPos = new LocationType();
        
        LinkedHashMap<String, MapMission> mapList = missionType.getMapsList();
        
        //HomeRef
        HomeReference hRef = new HomeReference(missionType.getHomeRef().asXML());
        //System.err.println(hRef);
        hRef.setRoll(0d);
        hRef.setPitch(0d);
        hRef.setYaw(0d);
        locStart.setLocation(hRef);
        
        //locStart;
        boolean isFound = false;
        for (MapMission mpm : mapList.values()) {
            //LinkedHashMap traList = mpm.getMap().getMarksList();
            LinkedHashMap<String, MarkElement> transList = mpm.getMap().getMarksList();
            for (MarkElement tmp : transList.values()) {
                String name = tmp.getName();
                if (name.equalsIgnoreCase("start")) {
                    locStart.setLocation(tmp.getCenterLocation());
                    isFound = true;
                    break;
                }
            }
            if (isFound)
                break;
        }
        if (isFound)
            startPos = new LocationType(locStart);
        else
            startPos = new LocationType(locStart);
        
        LinkedList<Maneuver> mans = plan.getGraph().getGraphAsManeuversList();
        NeptusLog.pub().debug(PublishHelper.class.getSimpleName() + "\nPlan: " + mans.size());
        if (mans.size() == 0)
            return new PathElement().asXML();
        
        Vector<LocationType> locLst = planPathLocs(plan);
        PathElement path = planPathElement(locLst, plan.getId());
        if (path == null) {
            path = new PathElement(null, null, startPos);
            //path.setMyColor(new Color(204, 255, 102));
            path.setId(plan.getId());
            path.setName(plan.getId());
            
            path.setFinished(true);
        }

        String pathXML = path.asXML();
        return pathXML;
    }
    
    public static final Vector<LocationType> planPathLocs(PlanType plan) {
        Vector<LocationType> locations = new Vector<>();
        if (plan == null)
            return locations;
        
        LinkedList<Maneuver> mans = plan.getGraph().getGraphAsManeuversList();

        for (Maneuver man : mans) {
            if (!(man instanceof LocatedManeuver))
                continue;

            LocationType destTo = ((LocatedManeuver) man).getManeuverLocation();                         
            if (man instanceof PathProvider)
                locations.addAll(((PathProvider) man).getPathLocations());
            else
                locations.add(destTo);
        }        
        return locations;
    }

    public static final PathElement planPathElement(List<LocationType> locLst, String pathId) {
        if (locLst.size() == 0)
            return null;
        LocationType pivotLoc = locLst.get(0).getNewAbsoluteLatLonDepth();
        LocationType destFrom = pivotLoc, destTo = null;
        double[] offsets;
        double[] offsetsum = {0, 0, 0};
        PathElement path = new PathElement(null, null, pivotLoc);
        if (pathId != null && pathId.length() > 0) {
            path.setId(pathId);
            path.setName(pathId);
        }
        for (LocationType loc : locLst) {
            destTo = loc;
            offsets = destTo.getOffsetFrom(destFrom);
            offsetsum[0] = offsetsum[0] + offsets[0];
            offsetsum[1] = offsetsum[1] + offsets[1];
            offsetsum[2] = offsetsum[2] + offsets[2];
            NeptusLog.pub().debug(PublishHelper.class.getSimpleName() + "\nPlan: " + offsets[0] + "; " + offsets[1]
                    + "; " + offsets[2] + ".");
            path.addPoint(offsetsum[1], offsetsum[0], offsetsum[2], false);
            destFrom = destTo;
        }
        path.setFinished(true);
            
        return path;
    }
    
//    public void startPublishState(final Map<String, VehicleTreeListener> vehicleTreesListeners) {
//        stopPublishState();
//        
//        ttask = new TimerTask() {
//          @Override
//          public void run() {
//              Document doc = DocumentHelper.createDocument();
//              Element ms = doc.addElement("MissionState");
//              for (VehicleTreeListener vtl : vehicleTreesListeners.values()) {
//                  Element vs = ms.addElement("VehicleState");
//                  vs.addAttribute("id", vtl.getVehicleId());
//                  
//                  try {
//                	  vs.addAttribute("time", DateTimeUtil.timeFormater
//								.format(new Date(vtl.getState().getTime())));
//                	  vs.addAttribute("date", DateTimeUtil.dateFormater
//								.format(new Date(vtl.getState().getTime())));
//                  }
//                  catch (Exception e) {
//                  }
//                  
//                  Element crd = vtl.getState().getPosition().asElement();
//                  vs.add(crd);
//                  Element att = vs.addElement("attitude");
//                  att.addElement("phi").addText("" + vtl.getState().getRoll());
//                  att.addElement("theta").addText("" + vtl.getState().getPitch());
//                  att.addElement("psi").addText("" + vtl.getState().getYaw());
//                  
//                  vs.addElement("imc");
//              }
//              //updateState(doc.asXML());
//              publisher.publishState(doc.asXML());
//              if (blinkLed != null)
//                  blinkLed.blinkLed();
//          }  
//        };
//        
//        try {
//			publisher.setServerURL(updateServerURL());
//		} catch (MalformedURLException e) {
//			NeptusLog.pub().warn("Web publish URL is malformed!!");
//		}
//        long pr = Long.parseLong(GeneralPreferences
//                .getProperty(GeneralPreferences.PUBLISH_WS_PERIOD));
//        if (runner == null)
//        	runner = new Timer("Publish Runner");
//        
//        runner.scheduleAtFixedRate(ttask, 500, pr);
//        //WebServer.start(8080);    
//    }
//
//    public void stopPublishState() {
//        if (ttask != null)
//            ttask.cancel();
//        
//        if (runner != null)
//        	runner.cancel();
//        
//        runner = null;
//        //WebServer.stop();
//    }
//    
//    public void setBlinkLed(BlinkStatusLed blinkLed) {
//        this.blinkLed = blinkLed;
//    }
//    
//    /**
//     * @param args
//     */
//    public static void main(String[] args) {
//        ConfigFetch.initialize();
//        PublishHelper ph = new PublishHelper();
//        ph.updateState("xml");
//        ph.updatePlan("xml");
//    }
}
