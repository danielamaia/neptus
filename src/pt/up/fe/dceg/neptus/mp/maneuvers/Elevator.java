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
 * 18 de Nov de 2011
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.editor.SpeedUnitsEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginProperty;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.PlanElement;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author pdias
 *
 */
public class Elevator extends Maneuver implements LocatedManeuver, IMCSerialization, StatisticsProvider {

    protected static final String DEFAULT_ROOT_ELEMENT = "Elevator";

    //@NeptusProperty(name="Location")
    public ManeuverLocation location = new ManeuverLocation();

    @NeptusProperty(name="Speed", description="The speed to be used")
    public double speed = 1000; 

    @NeptusProperty(name="Speed units", description="The speed units", editorClass=SpeedUnitsEditor.class)
    public String speedUnits = "RPM";

    @NeptusProperty(name="Start from current position", description="Start from current position or use the location field")
    public boolean startFromCurrentPosition = false;
    
    @NeptusProperty(name="Start Z (m)")
    public float startZ = 0;
    
    @NeptusProperty(name="Start Z Units")
    public ManeuverLocation.Z_UNITS startZUnits = ManeuverLocation.Z_UNITS.NONE;

//    protected ManeuverLocation.Z_UNITS startZUnits = pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS.NONE;
    
    @NeptusProperty(name="Radius (m)")
    public float radius = 5;

    double speedTolerance = 5, radiusTolerance = 2;

    /**
     * 
     */
    public Elevator() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        //        root.addAttribute("kind", "automatic");
        Element finalPoint = root.addElement("finalPoint");
        finalPoint.addAttribute("type", "pointType");
        Element point = getManeuverLocation().asElement("point");
        finalPoint.add(point);

        Element radTolerance = finalPoint.addElement("radiusTolerance");
        radTolerance.setText("0");

        Element startZ = root.addElement("startZ");
        startZ.setText(String.valueOf(getStartZ()));
        Element startZUnits = root.addElement("startZUnits");
        startZUnits.setText(String.valueOf(getStartZUnits().toString()));

        Element radius = root.addElement("radius");
        radius.setText(String.valueOf(getRadius()));

        Element velocity = root.addElement("speed");
        velocity.addAttribute("tolerance", String.valueOf(speedTolerance));
        velocity.addAttribute("type", "float");
        velocity.addAttribute("unit", getSpeedUnits());
        velocity.setText(String.valueOf(getSpeed()));

        Element flags = root.addElement("flags");
        flags.addAttribute("useCurrentLocation", String.valueOf(isStartFromCurrentPosition()));

        return document;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.Maneuver#loadFromXML(java.lang.String)
     */
    @Override
    public void loadFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            Node node = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/finalPoint/point");
            if (node == null)
                node = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/initialPoint/point"); // to read old elevator specs
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);
            Node speedNode = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/speed");
            setSpeed(Double.parseDouble(speedNode.getText()));
            setSpeedUnits(speedNode.valueOf("@unit"));

            Node sz = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/startZ");
            if (sz == null)
                doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/endZ"); // to read old elevator specs
            setStartZ(sz == null ? 0 : Float.parseFloat(sz.getText()));
            Node szu = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/startZUnits");
            setStartZUnits(szu == null ? ManeuverLocation.Z_UNITS.NONE : ManeuverLocation.Z_UNITS.valueOf(szu.getText()));
            setRadius(Float.parseFloat(doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/radius").getText()));

            Element flags = (Element) doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/flags");
            if (flags == null) {
                setStartFromCurrentPosition(false);
            }
            else {
                Node ucl = flags.selectSingleNode("@useCurrentLocation");
                if (ucl == null)
                    setStartFromCurrentPosition(false);
                else
                    setStartFromCurrentPosition(Boolean.parseBoolean(ucl.getText()));
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.Maneuver#ManeuverFunction(pt.up.fe.dceg.neptus.mp.VehicleState)
     */
    @Override
    public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return the startFromCurrentPosition
     */
    public boolean isStartFromCurrentPosition() {
        return startFromCurrentPosition;
    }

    /**
     * @param startFromCurrentPosition the startFromCurrentPosition to set
     */
    public void setStartFromCurrentPosition(boolean startFromCurrentPosition) {
        this.startFromCurrentPosition = startFromCurrentPosition;
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        return location.clone();
    }

    @Override
    public ManeuverLocation getEndLocation() {
        return getManeuverLocation();
    }

    @Override
    public ManeuverLocation getStartLocation() {
        return getManeuverLocation();
    }

    @Override
    public void setManeuverLocation(ManeuverLocation loc) {
        location = loc.clone();
        setStartZ((float)loc.getZ());
    }

    /**
     * @return the endZ
     */
    public float getEndZ() {
        return (float)getManeuverLocation().getZ();
    }

    /**
     * @param endZ the endZ to set
     */
    public void setEndZ(float endZ) {
        location.setZ(endZ);
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        getManeuverLocation().translatePosition(offsetNorth, offsetEast, offsetDown);
    }

    @Override
    public String getTooltipText() {
        return super.getTooltipText() + "<hr>" + "speed: <b>" + speed + " " + speedUnits + "</b>" + 
                (!startFromCurrentPosition ? "<br>cruise depth: <b>" + (int) getStartLocation().getDepth() + " m</b>":"") + 
                "<br>start: <b>" + startZ + " m (" + I18n.text(startZUnits.toString()) + ")</b>" +
                "<br>end z: <b>" + getManeuverLocation().getZ() + " m (" + I18n.text(getManeuverLocation().getZUnits().toString()) + ")</b>" +
                "<br>radius: <b>" + radius + " m</b>";                
    }

    public String validatePitchAngleDegrees(float value) {
        System.out.println("validate...");
        if (value < 0 || value > (float)45)
            return "Pitch angle shoud be bounded between [0\u00B0, 45\u00B0]";
        return null;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> properties = new Vector<DefaultProperty>();
        PluginProperty[] prop = PluginUtils.getPluginProperties(this);
        properties.addAll(Arrays.asList(prop));
        return properties;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public IMCMessage serializeToIMC() {
        getManeuverLocation().convertToAbsoluteLatLonDepth();

        pt.up.fe.dceg.neptus.imc.Elevator elevator = new pt.up.fe.dceg.neptus.imc.Elevator();

        elevator.setTimeout(getMaxTime());
        elevator.setLat(getManeuverLocation().getLatitudeAsDoubleValueRads());
        elevator.setLon(getManeuverLocation().getLongitudeAsDoubleValueRads());
        elevator.setStartZ(startZ);
        elevator.setStartZUnits(startZUnits.toString());
        elevator.setEndZ(getManeuverLocation().getZ());
        elevator.setEndZUnits(getManeuverLocation().getZUnits().toString());
        elevator.setRadius(getRadius());
        elevator.setSpeed(getSpeed());
        elevator.setCustom(getCustomSettings());
        
        switch (getSpeedUnits()) {
            case "m/s":
                elevator.setSpeedUnits(pt.up.fe.dceg.neptus.imc.Elevator.SPEED_UNITS.METERS_PS);
                break;
            case "RPM":
                elevator.setSpeedUnits(pt.up.fe.dceg.neptus.imc.Elevator.SPEED_UNITS.RPM);
                break;
            default:
                elevator.setSpeedUnits(pt.up.fe.dceg.neptus.imc.Elevator.SPEED_UNITS.PERCENTAGE);
                break;
        }

        if (isStartFromCurrentPosition())
            elevator.setFlags(pt.up.fe.dceg.neptus.imc.Elevator.FLG_CURR_POS);
        else
            elevator.setFlags((short)0);

        return elevator;
    }
    @Override
    public void parseIMCMessage(IMCMessage message) {
        if (!DEFAULT_ROOT_ELEMENT.equalsIgnoreCase(message.getAbbrev()))
            return;
        pt.up.fe.dceg.neptus.imc.Elevator elev = null;
        try {
             elev = new pt.up.fe.dceg.neptus.imc.Elevator(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        setMaxTime(elev.getTimeout());
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitude(Math.toDegrees(elev.getLat()));
        loc.setLongitude(Math.toDegrees(elev.getLon()));
        loc.setZ(elev.getEndZ());
        System.out.println(elev.getEndZUnits());
//        loc.setZUnits(pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS.valueOf(elev.getEndZUnits().toString()));
        loc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(message.getString("end_z_units").toString()));
        setManeuverLocation(loc);
        startZ = (float)elev.getStartZ();
        startZUnits = ManeuverLocation.Z_UNITS.valueOf(message.getString("start_z_units").toString());
        setRadius((float)elev.getRadius());
        setSpeed(elev.getSpeed());
        setStartFromCurrentPosition((elev.getFlags() & pt.up.fe.dceg.neptus.imc.Elevator.FLG_CURR_POS) != 0);
        setCustomSettings(elev.getCustom());
        
        switch (elev.getSpeedUnits()) {
            case RPM:
                speedUnits = "RPM";
                break;
            case METERS_PS:
                speedUnits = "m/s";
                break;
            case PERCENTAGE:
                speedUnits = "%";
                break;
            default:
                break;
        }
    }

    @Override
    public Object clone() {
        Elevator clone = new Elevator();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation());
        clone.startZ = startZ;
        clone.startZUnits = startZUnits;
        clone.setStartFromCurrentPosition(isStartFromCurrentPosition());
        clone.setRadius(getRadius());
        clone.setSpeed(getSpeed());
        clone.setSpeedUnits(getSpeedUnits());
        return clone;
    }

    /**
     * @return the radius
     */
    public float getRadius() {
        return radius;
    }

    /**
     * @param radius the radius to set
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * @return the speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @param speed the speed to set
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }

    /**
     * @return the units
     */
    public String getSpeedUnits() {
        return speedUnits;
    }

    /**
     * @param units the units to set
     */
    public void setSpeedUnits(String units) {
        this.speedUnits = units;
    }

    /**
     * @return the startZ
     */
    public final float getStartZ() {
        return startZ;
    }

    /**
     * @param startZ the startZ to set
     */
    public final void setStartZ(float startZ) {
        this.startZ = startZ;
    }

    /**
     * @return the startZUnits
     */
    public ManeuverLocation.Z_UNITS getStartZUnits() {
        return startZUnits;
    }
    
    /**
     * @param startZUnits the startZUnits to set
     */
    public void setStartZUnits(ManeuverLocation.Z_UNITS startZUnits) {
        this.startZUnits = startZUnits;
    }
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        g2d = (Graphics2D) g2d.create();
        if (!isStartFromCurrentPosition()) {
            // x marks the spot...
            g2d.drawLine(-4, -4, 4, 4);
            g2d.drawLine(-4, 4, 4, -4);
        }
        double radius = this.getRadius() * renderer.getZoom();
        if (isStartFromCurrentPosition())
            g2d.setColor(new Color(255, 0, 0, 100));
        else
            g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fill(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));
        if (isStartFromCurrentPosition())
            g2d.setColor(Color.RED);
        else
            g2d.setColor(Color.GREEN);
        g2d.draw(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));
        g2d.setColor(new Color(255, 0, 0, 200));
        for (double i = this.getRadius() - 2; i > 0; i = i - 2) {
            double r = i * renderer.getZoom();
            g2d.draw(new Ellipse2D.Double(-r, -r, r * 2, r * 2));
        }

        //        g2d.rotate(Math.PI/2);
        g2d.translate(0, -14);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (isStartFromCurrentPosition()) {
            g2d.drawLine(-5, 0, 5, 0);
        }
        else {
            int m = 1;
            if (getManeuverLocation().getAllZ() < getEndZ())
                m = -1;
            g2d.drawLine(-5, m * -5, 5, m * -5);
            if (getManeuverLocation().getAllZ() < getEndZ() || getManeuverLocation().getAllZ() > getEndZ()) {
                g2d.drawLine(-5, m * 5, 0, 0);
                g2d.drawLine(5, m * 5, 0, 0);
            }
            else
                g2d.drawLine(-5, m * 5, 5, m * 5);
        }

        g2d.dispose();
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        double speed = this.speed;
        if (this.speedUnits.equalsIgnoreCase("RPM")) {
            speed = speed/769.230769231; //1.3 m/s for 1000 RPMs
        }
        else if (this.speedUnits.equalsIgnoreCase("%")) {
            speed = speed/76.923076923; //1.3 m/s for 100% speed
        }
      
        return getDistanceTravelled(initialPosition) / speed;
    }

    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = startFromCurrentPosition ? 0 : getStartLocation().getDistanceInMeters(initialPosition);
        double depthDiff = startFromCurrentPosition ? initialPosition.getAllZ() : getStartLocation().getAllZ();
        meters += depthDiff;
        return meters;
    }

    @Override
    public double getMaxDepth() {
        return getManeuverLocation().getAllZ();
    }

    @Override
    public double getMinDepth() {
        return getManeuverLocation().getAllZ();
    }   
}
