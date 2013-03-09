/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Jun 8, 2012
 */
package pt.up.fe.dceg.neptus.plugins.matlab;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;
import javax.vecmath.Point3d;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.mp.templates.PlanCreator;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.PathElement;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Planner - Shortest Path", icon = "pt/up/fe/dceg/neptus/plugins/matlab/shortest.png")
public class ShortestPathPlanner extends SimpleRendererInteraction implements Renderer2DPainter,
        StateRendererInteraction {

    private static final long serialVersionUID = -3743171179579658242L;

    public enum EDITION_STATES {
        NONE,
        ADDING_OBSTACLE
    };

    public LocationType destination = null;
    public LocationType initial = null;
    public LocationType bottomLeft = null;
    public LocationType topRight = null;

    protected boolean isActive;
    protected EDITION_STATES state = EDITION_STATES.NONE;
    protected Vector<PathElement> obstacles = new Vector<PathElement>();
    protected PathElement currentObstacle = null;
    protected LocationType ultimoPonto = null;

    @NeptusProperty(name = "Default Speed (m/s)")
    public double defaultSpeed = 1.0;

    @NeptusProperty(name = "Default Depth (m)")
    public double defaultDepth = 2.0;

    /**
     * @param console
     */
    public ShortestPathPlanner(ConsoleLayout console) {
        super(console);
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        isActive = mode;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {

        final Point2D mousePosition = event.getPoint();
        final StateRenderer2D renderer = source;

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menu = new JPopupMenu();
            switch (state) {
                case NONE:
                    menu.add("Define boundary's bottom left").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            bottomLeft = renderer.getRealWorldLocation(mousePosition);
                        }
                    });

                    menu.add("Define boundary's top right").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            topRight = renderer.getRealWorldLocation(mousePosition);
                        }
                    });

                    menu.add("Define destination location").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            destination = renderer.getRealWorldLocation(mousePosition);
                        }
                    });

                    menu.add("Define initial location").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            initial = renderer.getRealWorldLocation(mousePosition);
                        }
                    });
                    menu.add("Add obstacle").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            state = EDITION_STATES.ADDING_OBSTACLE;
                            LocationType loc = renderer.getRealWorldLocation(mousePosition);
                            currentObstacle = new PathElement(renderer.getMapGroup(), null, loc);
                            currentObstacle.setFill(true);
                            currentObstacle.setShape(true);
                            currentObstacle.setMyColor(Color.yellow);
                            currentObstacle.addPoint(0, 0, 0, false);

                            return;
                        }
                    });
                    menu.add("Clear obstacles").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            obstacles.clear();
                        }
                    });

                    for (PathElement el : obstacles) {
                        if (el.containsPoint(source.getRealWorldLocation(mousePosition), source)) {
                            final PathElement toremove = el;
                            menu.add("Remove obstacle").addActionListener(new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    obstacles.remove(toremove);
                                }
                            });

                        }
                    }

                    menu.addSeparator();

                    menu.add("Shortest Path settings").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PropertiesEditor.editProperties(ShortestPathPlanner.this, true);

                        }
                    });

                    menu.addSeparator();

                    menu.add("Clear map").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            obstacles.clear();
                            initial = null;
                            destination = null;
                            bottomLeft = null;
                            topRight = null;

                        }
                    });

                    menu.addSeparator();

                    final JMenuItem item = menu.add("Generate plan");
                    if (initial != null && destination != null && bottomLeft != null && topRight != null) {
                        item.setEnabled(true);

                        item.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    item.setEnabled(false);
                                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                                        protected Void doInBackground() throws Exception {
                                            generatePlan();
                                            return null;
                                        }

                                        /*
                                         * (non-Javadoc)
                                         * 
                                         * @see javax.swing.SwingWorker#done()
                                         */
                                        @Override
                                        protected void done() {
                                            item.setEnabled(true);
                                            System.out.println("..................................................");
                                        }
                                    };
                                    worker.execute();
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(getConsole(), ex);
                                }
                            }
                        });
                    }
                    break;
                case ADDING_OBSTACLE:
                    menu.add("Finish obstacle").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            currentObstacle.setFinished(true);
                            currentObstacle.setMyColor(Color.orange);
                            obstacles.add(currentObstacle);
                            currentObstacle = null;
                            state = EDITION_STATES.NONE;
                        }
                    });
                    menu.add("Delete obstacle").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            state = EDITION_STATES.NONE;
                            currentObstacle = null;
                        }
                    });
                default:
                    break;
            }

            menu.show(source, (int) mousePosition.getX(), (int) mousePosition.getY());
        }
        else {
            if (state == EDITION_STATES.ADDING_OBSTACLE) {
                System.out.println(renderer.getRealWorldLocation(mousePosition));
                LocationType loc = renderer.getRealWorldLocation(mousePosition);
                double offsets[] = loc.getOffsetFrom(currentObstacle.getCenterLocation());
                currentObstacle.addPoint(offsets[1], offsets[0], 0, false);
            }
        }

    }

    protected PlanType createPlanFromWaypoints(Collection<LocationType> waypoints,
            LinkedHashMap<String, Object> maneuverProperties) {
        PlanCreator creator = new PlanCreator(getConsole().getMission());
        for (LocationType loc : waypoints) {
            creator.setLocation(loc);
            creator.addGoto(maneuverProperties);
        }
        return creator.getPlan();
    }

    protected PlanType createPlan(Collection<LocationType> waypoints, double speedMps) {
        LinkedHashMap<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("speed", speedMps);
        props.put("units", "m/s");
        return createPlanFromWaypoints(waypoints, props);
    }

    protected void generatePlan() throws Exception {
        File output = new File(ConfigFetch.getNeptusTmpDir(), "shortest_path_in.ini");
        File input = new File(ConfigFetch.getNeptusTmpDir(), "shortest_path_out.ini");

        NeptusLog.pub().info("Creating file " + output.getAbsolutePath());
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));

        writer.append("[vehicle]\n");
        writer.append("name = " + getConsole().getMainSystem() + "\n\n");

        writer.append("[map_origin]\n");
        bottomLeft.convertToAbsoluteLatLonDepth();
        writer.append("latitude = " + bottomLeft.getLatitudeAsDoubleValue() + "\n");
        writer.append("longitude = " + bottomLeft.getLongitudeAsDoubleValue() + "\n\n");

        writer.append("[map_finish]\n");
        topRight.convertToAbsoluteLatLonDepth();
        writer.append("latitude = " + topRight.getLatitudeAsDoubleValue() + "\n");
        writer.append("longitude = " + topRight.getLongitudeAsDoubleValue() + "\n\n");

        writer.append("[start_point]\n");
        initial.convertToAbsoluteLatLonDepth();
        writer.append("latitude = " + initial.getLatitudeAsDoubleValue() + "\n");
        writer.append("longitude = " + initial.getLongitudeAsDoubleValue() + "\n\n");

        writer.append("[end_point]\n");
        destination.convertToAbsoluteLatLonDepth();
        writer.append("latitude = " + destination.getLatitudeAsDoubleValue() + "\n");
        writer.append("longitude = " + destination.getLongitudeAsDoubleValue() + "\n\n");

        writer.append("[obstacles]\n");
        int i = 1;

        for (PathElement obs : obstacles) {
            Vector<Point3d> points = obs.getPoints();
            LocationType center = new LocationType(obs.getCenterLocation().convertToAbsoluteLatLonDepth());
            writer.append("obs" + (i++) + " = ");
            for (Point3d pt : points) {
                LocationType loc = new LocationType(center);
                loc.translatePosition(pt.x, pt.y, 0);
                loc.convertToAbsoluteLatLonDepth();
                writer.append(loc.getLatitudeAsDoubleValue() + ", " + loc.getLongitudeAsDoubleValue() + "; ");
            }
            writer.append(center.getLatitudeAsDoubleValue() + ", " + center.getLongitudeAsDoubleValue() + ";\n");

        }
        writer.close();
        // System.out.println("Escrevi ini em '"+output+"'");

        String result = "<html>"
                + execCommand("matlab -nodisplay -r shortestPath_no_plots('" + output.getAbsolutePath() + "','"
                        + input.getAbsolutePath() + "')");
        // String result =
        // "<html>"+execCommand("matlab -r shortestPath_plots('"+output.getAbsolutePath()+"','"+input.getAbsolutePath()+"')");

        GuiUtils.htmlMessage(getConsole(), "Shortest Path", "", result);

        Vector<LocationType> wpts = new Vector<LocationType>();

        String in = FileUtil.getFileAsString(input);

        System.out.println(in);
        try {
            String[] lines = in.split("\n");
            for (String l : lines) {
                String[] parts = l.split("\t");
                Double lat = Double.parseDouble(parts[0]);
                Double lon = Double.parseDouble(parts[1]);
                wpts.add(new LocationType(lat, lon));
            }
            PlanType pt = createPlan(wpts, defaultSpeed);
            pt.setVehicle(getConsole().getMainSystem());
            getConsole().getMission().addPlan(pt);
            getConsole().warnMissionListeners();
            getConsole().setPlan(pt);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
        // carregar output para Vector de LocationType e criar plano usando...
        //
        //
        //
    }

    protected static String errors = "", output = "";

    public static String execCommand(String command) {

        errors = "";
        output = "";

        System.out.println("$>" + command + "\n");
        String result = "<p>Executing <b>" + command + "</b>...</p>\n";
        try {
            // String s;
            // System.out.println(new File("astar").listFiles().length);
            Process p = Runtime.getRuntime().exec(command);
            final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            final BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            Thread out = new Thread(new Runnable() {
                public void run() {
                    String s;
                    try {
                        while ((s = stdInput.readLine()) != null) {
                            output += s + "\n";
                            System.out.println(s);

                        }
                        System.out.println("closed");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            out.start();

            Thread err = new Thread(new Runnable() {
                public void run() {
                    String s;
                    try {
                        while ((s = stdError.readLine()) != null) {
                            errors += s + "\n";
                            System.err.println(s);
                            System.err.flush();
                        }
                        System.err.println("closed");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            err.start();

            while (out.isAlive() && err.isAlive())
                try {
                    Thread.sleep(500);
                    System.out.println(".");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

        }
        catch (Exception e) {
            e.printStackTrace(System.err);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            errors += new String(baos.toByteArray());
        }

        if (output.length() > 0) {
            result += "<p><b>Output:</b><blockquote><font color='green'><pre>" + output
                    + "</pre></font></blockquote></p>";
        }

        if (errors.length() > 0) {
            result += "<p><b>Errors:</b><blockquote><font color='red'><pre>" + errors
                    + "</pre></font></blockquote></p>";
        }

        return result;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (destination != null) {
            Point2D ponto = renderer.getScreenPosition(destination);
            if (isActive)
                g.setColor(Color.red);
            else
                g.setColor(Color.red.darker().darker());

            g.fill(new Ellipse2D.Double(ponto.getX() - 5, ponto.getY() - 5, 10, 10));
            g.draw(new Ellipse2D.Double(ponto.getX() - 5, ponto.getY() - 5, 10, 10));
        }

        g.setColor(Color.green.darker());
        g.setStroke(new BasicStroke(4.0f));

        if (initial != null) {
            Point2D ponto = renderer.getScreenPosition(initial);
            if (isActive)
                g.setColor(Color.green);
            else
                g.setColor(Color.green.darker().darker());

            g.fill(new Ellipse2D.Double(ponto.getX() - 5, ponto.getY() - 5, 10, 10));
            g.draw(new Ellipse2D.Double(ponto.getX() - 5, ponto.getY() - 5, 10, 10));
        }

        if (isActive) {
            if (bottomLeft != null) {
                Point2D ponto = renderer.getScreenPosition(bottomLeft);
                g.draw(new Line2D.Double(ponto.getX(), ponto.getY(), ponto.getX() + 20, ponto.getY()));
                g.draw(new Line2D.Double(ponto.getX(), ponto.getY(), ponto.getX(), ponto.getY() - 20));
            }

            if (topRight != null) {
                Point2D ponto = renderer.getScreenPosition(topRight);
                g.draw(new Line2D.Double(ponto.getX() - 20, ponto.getY(), ponto.getX(), ponto.getY()));
                g.draw(new Line2D.Double(ponto.getX(), ponto.getY(), ponto.getX(), ponto.getY() + 20));
            }
        }

        g.setColor(Color.black);
        g.setStroke(new BasicStroke(1.0f));
        if (topRight != null && bottomLeft != null) {
            Point2D topo = renderer.getScreenPosition(topRight);
            Point2D fundo = renderer.getScreenPosition(bottomLeft);

            double x = Math.min(topo.getX(), fundo.getX());
            double y = Math.min(topo.getY(), fundo.getY());

            double w = Math.abs(topo.getX() - fundo.getX());
            double h = Math.abs(topo.getY() - fundo.getY());

            g.draw(new Rectangle2D.Double(x, y, w, h));
        }

        for (PathElement elem : obstacles) {
            elem.paint(g, renderer, renderer.getRotation());
        }

        if (currentObstacle != null)
            currentObstacle.paint(g, renderer, renderer.getRotation());

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
