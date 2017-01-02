package org.necsave;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.eclipsesource.json.JsonObject;

import info.necsave.plot.LogParser;
import info.necsave.plot.LogProcessor;
import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.types.coord.LocationType;

public class Json2Lsf implements LogProcessor {

    // store last positions at surface from all vehicles
    protected LinkedHashMap<String, EstimatedState> lastSurfaceLocations = new LinkedHashMap<>();
    
    // store timestamps of plan generation
    protected LinkedHashMap<Integer, Long> planGenerationTime = new LinkedHashMap<>();
    
    // cache platform names received via PlatformInfo messages
    protected LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    
    // methods with names matching NMP messages will be called when respective messages are found on the log
    protected LinkedHashMap<String, Method> methods = new LinkedHashMap<>();
    {
        for (Method m : getClass().getMethods()) {
            if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == JsonObject.class) {
                methods.put(m.getName(), m);
            }
        }
    }

    // CSV file handles
    private BufferedWriter latencies, navError;

    public Json2Lsf() {
        System.out.println("Exporting to "+LsfMessageLogger.getLogDirSingleton());
        try {
            latencies = new BufferedWriter(new FileWriter(LsfMessageLogger.getLogDirSingleton() + "/latency.csv"));
            latencies.write("Timestamp,Plan,Master,Receiver,Latency\n");
            
            navError = new BufferedWriter(new FileWriter(LsfMessageLogger.getLogDirSingleton() + "/accuracy.csv"));
            navError.write("Timestamp,Platform,Time Submerged,Position Error\n");        
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public String getName() {
        return "JSON 2 LSF";
    }

    public void PlatformInfo(JsonObject obj) {
        if (!isSourceKnown(obj)) {
            Announce announce = new Announce();
            announce.setSrc(getPlatformSrc(obj));
            announce.setSysName(obj.get("platform_name").asString());
            announce.setTimestamp(getTimestamp(obj) / 1000.0);
            LsfMessageLogger.log(announce);
        }
        platformNames.put(getPlatformSrc(obj), obj.get("platform_name").asString());
    }

   

    public void on(EstimatedState state) {
        String src = platformNames.get(state.getSrc());
        
        if (state.getDepth() == 0) {
            if (!lastSurfaceLocations.containsKey(src))
                lastSurfaceLocations.put(src, state);
            else if (state.getTimestamp() - lastSurfaceLocations.get(src).getTimestamp() > 5) {
                LocationType prev = IMCUtils.getLocation(lastSurfaceLocations.get(src));
                LocationType cur = IMCUtils.getLocation(state);
                try {
                    navError.write(state.getTimestampMillis() + "," + src + ","
                            + (state.getTimestamp() - lastSurfaceLocations.get(src).getTimestamp()) + ","
                            + cur.getHorizontalDistanceInMeters(prev) + "\n");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            lastSurfaceLocations.put(src, state);
        }
    }
    
    public void Plan(JsonObject obj) {
        int planId = Integer.parseInt(obj.getString("plan_id", "0"));
        int receiver = Integer.parseInt(obj.getString("platform_rec", "65535"));
        String medium = obj.getString("medium", "internal");

        boolean generated = receiver == 65535;
        boolean received = !medium.equals("internal");

        PlanControl pc = new PlanControl();
        pc.setPlanId("" + planId);
        pc.setSrc(receiver);
        pc.setTimestamp(getTimestamp(obj) / 1000.0);
        
        if (generated) {
            pc.setOp(OP.LOAD);
            pc.setSrc(getPlatformSrc(obj));
            pc.setInfo(pc.getDate() + ": Plan " + planId + " has been generated by " + pc.getSrc());
            LsfMessageLogger.log(pc);
            planGenerationTime.put(planId, pc.getTimestampMillis());
            try {
                latencies
                        .write(pc.getTimestampMillis() + "," + planId + "," + pc.getSrc() + "," + pc.getSrc() + ",0\n");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (received) {
            pc.setInfo(pc.getDate() + ": Plan " + planId + " has been received by " + pc.getSrc());
            long generationTime = planGenerationTime.get(planId);
            try {
                latencies.write(pc.getTimestampMillis() + "," + planId + ","
                        + Integer.parseInt(obj.getString("platform_src", "65535")) + "," + pc.getSrc() + ","
                        + (pc.getTimestampMillis() - generationTime) + "\n");
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            LsfMessageLogger.log(pc);
        }
    }

    public void process(JsonObject obj) {
        String msgName = obj.get("abbrev").asString();

        if (methods.containsKey(msgName)) {
            try {
                methods.get(msgName).invoke(this, obj);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            // System.out.println(msgName + " was not processed.");
        }
    }

    protected long getTimestamp(JsonObject msg) {
        return Long.parseLong(msg.get("timestamp").asString()) / 1000;

    }

    protected String getName(int id) {
        if (!platformNames.containsKey(id))
            return "Platform " + id;

        return platformNames.get(id);
    }

    protected int getPlatformSrc(JsonObject msg) {
        return Integer.parseInt(msg.get("platform_src").asString());
    }

    protected boolean isSourceKnown(JsonObject msg) {
        return platformNames.containsKey(getPlatformSrc(msg));
    }

    protected String getPlatformName(JsonObject msg) {
        return getName(getPlatformSrc(msg));
    }

    protected int getModuleSrc(JsonObject msg) {
        return Integer.parseInt(msg.get("module_src").asString());
    }

    @Override
    public void logFinished() {
        LsfMessageLogger.close();
        try {
            latencies.close();
            navError.close();
        }
        catch (Exception e) {
            // TODO: handle exception
        }
    }

    public void Kinematics(JsonObject m) {
        if (!isSourceKnown(m))
            return;

        if (getPlatformSrc(m) == 14)
            return;

        EstimatedState state = new EstimatedState();
        JsonObject coords = m.get("waypoint").asObject();
        double lat = Double.parseDouble(coords.get("latitude").asString());
        double lon = Double.parseDouble(coords.get("longitude").asString());
        double depth = Double.parseDouble(coords.get("depth").asString());
        double alt = Double.parseDouble(coords.get("altitude").asString());
        double yaw = Double.parseDouble(m.get("heading").asString());
        double u = Double.parseDouble(m.get("speed").asString());
        state.setSrc(getPlatformSrc(m));
        state.setLat(lat);
        state.setLon(lon);
        state.setDepth(depth);
        state.setAlt(alt);
        state.setPsi(yaw);
        state.setU(u);
        state.setTimestamp(getTimestamp(m) / 1000.0);
        
        on(state);
        
        LsfMessageLogger.log(state);

    }

    private static File[] collectLogs(File topDir) {
        HashSet<File> found = new HashSet<>();
        collectLogs(topDir, found);
        return found.toArray(new File[0]);
    }

    private static void collectLogs(File topDir, HashSet<File> found) {
        for (File f : topDir.listFiles()) {
            if (f.isDirectory())
                collectLogs(f, found);
            else if (f.getName().endsWith(".json"))
                found.add(f);
        }
    }

    public static void main(String[] args) throws Exception {

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select logs folder");
        chooser.setAcceptAllFileFilterUsed(false);
        if (!(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION))
            return;

        File topDir = chooser.getSelectedFile();
        LsfMessageLogger.changeLogBaseDir(topDir.getAbsolutePath() + "/");
        File[] files = collectLogs(topDir);
        LogParser parser = new LogParser(files);
        parser.process(false, new Json2Lsf());
        JOptionPane.showMessageDialog(null,
                files.length + " files were merged to " + LsfMessageLogger.getLogDirSingleton());
    }

}
