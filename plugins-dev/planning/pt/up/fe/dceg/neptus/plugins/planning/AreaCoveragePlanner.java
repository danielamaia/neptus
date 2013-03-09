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
 * Apr 26, 2010
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Collection;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.SystemsList;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mp.templates.PlanCreator;
import pt.up.fe.dceg.neptus.planeditor.IEditorMenuExtension;
import pt.up.fe.dceg.neptus.planeditor.IMapPopup;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.PathElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
@LayerPriority(priority = 50)
@SuppressWarnings("serial")
@PluginDescription(name = "Polygon Coverage Planner", icon = "pt/up/fe/dceg/neptus/plugins/planning/polyline.png")
public class AreaCoveragePlanner extends SimpleSubPanel implements StateRendererInteraction, IEditorMenuExtension,
        Renderer2DPainter {

    @NeptusProperty(name = "Depth")
    public double depth = 1;

    @NeptusProperty(name = "Grid width")
    public double grid = 20;

    protected InteractionAdapter adapter;

    private boolean initCalled = false;

    public AreaCoveragePlanner(ConsoleLayout console) {
        super(console);
        adapter = new InteractionAdapter(console);
        setVisibility(false);
    }

    @Override
    public void initSubPanel() {

        if (initCalled)
            return;
        initCalled = true;

        addMenuItem(I18n.text("Settings")+">" + I18n.text("Coverage Planner Settings"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(AreaCoveragePlanner.this, getConsole(), true);
            }
        });

    };

    PathElement pe = null;
    int vertexCount = 0;

    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        if (pe != null) {
            Graphics2D g = (Graphics2D) g2.create();
            pe.paint(g, renderer, -renderer.getRotation());
            g.dispose();
        }
    }

    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {

        JMenu menu = new JMenu("Polygon coverage");
        final LocationType l = loc;
        menu.add(new AbstractAction("Add polygon vertex") {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                if (pe == null) {
                    pe = new PathElement(MapGroup.getMapGroupInstance(getConsole().getMission()), new MapType(), l);
                    pe.setMyColor(Color.green.brighter());
                    pe.setShape(true);
                    pe.setFinished(true);
                    pe.setStroke(new BasicStroke(2.0f));
                    pe.addPoint(0, 0, 0, false);
                }
                else {
                    double[] offsets = l.getOffsetFrom(pe.getCenterLocation());
                    pe.addPoint(offsets[1], -offsets[0], 0, false);
                }
                vertexCount++;
            }
        });

        if (vertexCount > 0) {
            menu.add(new AbstractAction("Clear") {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    pe = null;
                    vertexCount = 0;
                }
            });
        }

        if (vertexCount > 2) {
            menu.add(new AbstractAction("Generate plan(s)") {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    solve();
                }
            });
        }

        Vector<JMenuItem> items = new Vector<JMenuItem>();
        items.add(menu);
        return items;
    }

    private void solve() {

        if (pe == null) {
            GuiUtils.errorMessage(getConsole(), "Coverage Plan Solver", "The polygon is not valid");
            return;
        }

        double north, east, south, west;
        double[] bounds = pe.getBounds3d();

        south = bounds[PathElement.SOUTH_COORD];
        west = bounds[PathElement.WEST_COORD];
        north = bounds[PathElement.NORTH_COORD];
        east = bounds[PathElement.EAST_COORD];

        CoverageCell[][] cells = new CoverageCell[(int) ((north - south) / grid) + 1][(int) ((east - west) / grid) + 1];

        for (int i = 0; i < cells.length; i++)
            for (int j = 0; j < cells[i].length; j++) {
                cells[i][j] = new CoverageCell();
                cells[i][j].i = i;
                cells[i][j].j = j;
            }

        int i = 0, j = 0;
        int desiredCells = 0;
        for (double n = south + grid / 2; n < north; n += grid) {
            j = 0;
            for (double e = west + grid / 2; e < east; e += grid) {
                LocationType lt = new LocationType(pe.getCenterLocation());
                lt.translatePosition(n, e, 0);
                CoverageCell cell = cells[i][j];
                cell.realWorldLoc = lt.getNewAbsoluteLatLonDepth();
                if (pe.containsPoint(lt, null)) {
                    cell.desired = true;
                    desiredCells++;
                }
                cells[i][j] = cell;
                j++;
            }
            i++;
        }

        CoverageCell initialCell = null;
        i = 0;
        for (j = 0; j < cells[0].length - 1 && initialCell == null; j++)
            for (i = 0; i < cells.length && initialCell == null; i++)
                if (cells[i][j].desired)
                    initialCell = cells[i][j];

        if (initialCell == null) {
            GuiUtils.errorMessage("Polygon coverage", "Polygon area is invalid");
            return;
        }

        CoverageCell current = initialCell;
        desiredCells--;

        int dir = -1;

        while (desiredCells > 0) {
            current.visited = true;
            current.active = false;
            if (dir == 1) {
                if (current.i < cells.length - 1 && cells[current.i + 1][current.j].desired == true
                        && cells[current.i + 1][current.j].visited == false) {
                    current.next = cells[current.i + 1][current.j];
                    cells[current.i + 1][current.j].previous = current;
                    current = current.next;
                    current.active = true;
                }
                else {
                    dir = -1;
                    if (current.j == cells[0].length - 1)
                        break;

                    while (!cells[current.i][current.j + 1].desired && i > 0 && current.previous != null) {
                        current.active = false;
                        current = current.previous;
                    }

                    if (i == 0)
                        break;

                    current.next = cells[current.i][current.j + 1];
                    cells[current.i][current.j + 1].previous = current;
                    current = current.next;
                    current.active = true;
                }
            }
            else {
                if (current.i > 0 && cells[current.i - 1][current.j].desired == true
                        && cells[current.i - 1][current.j].visited == false) {
                    current.next = cells[current.i - 1][current.j];
                    cells[current.i - 1][current.j].previous = current;
                    current = current.next;
                    current.active = true;
                }
                else {
                    dir = 1;
                    if (current.j == cells[0].length - 1)
                        break;

                    while (current.previous != null && !cells[current.i][current.j + 1].desired && i < cells.length) {
                        current.active = false;
                        current = current.previous;
                    }

                    if (i == cells.length)
                        break;

                    current.next = cells[current.i][current.j + 1];
                    cells[current.i][current.j + 1].previous = current;
                    current = current.next;
                    current.active = true;
                }
            }
            desiredCells--;
        }
        generatePlans(cells, initialCell);
    }

    void generatePlans(CoverageCell[][] mat, CoverageCell first) {

        Vector<String> selectedVehicles = new Vector<String>();
        Vector<SystemsList> tmp = getConsole().getSubPanelsOfClass(SystemsList.class);

        selectedVehicles.addAll(tmp.get(0).getSelectedSystems(true));
        Object planid;

        if (selectedVehicles.size() > 1)
            planid = JOptionPane.showInputDialog(getConsole(), "Enter desired plan prefix");
        else
            planid = JOptionPane.showInputDialog(getConsole(), "Enter desired plan name");

        MissionType mission = getConsole().getMission();

        if (mission == null) {
            GuiUtils.errorMessage(getConsole(), "Coverage Plan Solver", "No mission has been set");
            return;
        }

        if (selectedVehicles.size() <= 1) {
            CoverageCell current = first, next = current.next;
            PlanCreator creator = new PlanCreator(mission);
            creator.setLocation(first.realWorldLoc);
            // creator.addManeuver("Goto");
            while (next != null) {
                if (next.j != current.j) {
                    CoverageCell pivot = current;
                    while (pivot.previous != null && pivot.previous.i == current.i)
                        pivot = pivot.previous;
                    creator.setLocation(pivot.realWorldLoc);
                    creator.addManeuver("Goto");
                    creator.setLocation(next.realWorldLoc);
                    creator.addManeuver("Goto");
                }
                current = next;
                next = current.next;
            }

            PlanType plan = creator.getPlan();
            plan.setId(planid.toString());
            plan.setVehicle(getConsole().getMainSystem());
            mission.addPlan(plan);
            mission.save(false);
            getConsole().updateMissionListeners();
        }
        else {
            double distance = 0;
            CoverageCell current = first, next = current.next;
            distance += current.realWorldLoc.getDistanceInMeters(next.realWorldLoc);
            while (next != null) {
                if (next.j != current.j) {
                    CoverageCell pivot = current;
                    while (pivot.previous != null && pivot.previous.i == current.i)
                        pivot = pivot.previous;
                }
                distance += current.realWorldLoc.getDistanceInMeters(next.realWorldLoc);
                current = next;
                next = current.next;
            }

            double distEach = distance / selectedVehicles.size();

            current = first;
            next = current.next;
            PlanCreator creator = new PlanCreator(mission);
            creator.setLocation(current.realWorldLoc);
            distance = 0;
            int curIndex = 0;
            while (next != null) {

                if (next.j != current.j) {
                    CoverageCell pivot = current;
                    while (pivot.previous != null && pivot.previous.i == current.i)
                        pivot = pivot.previous;
                    creator.setLocation(pivot.realWorldLoc);
                    creator.addManeuver("Goto");

                    distance += current.realWorldLoc.getDistanceInMeters(next.realWorldLoc);

                    if (distance < distEach) {
                        creator.setLocation(next.realWorldLoc);
                        creator.addManeuver("Goto");
                    }
                }
                else
                    distance += current.realWorldLoc.getDistanceInMeters(next.realWorldLoc);

                if (distance > distEach) {
                    creator.setLocation(current.realWorldLoc);
                    creator.addManeuver("Goto");
                    PlanType plan = creator.getPlan();
                    plan.setVehicle(selectedVehicles.get(curIndex));
                    plan.setId(planid + "_" + selectedVehicles.get(curIndex++));

                    mission.addPlan(plan);
                    creator = new PlanCreator(mission);
                    creator.setLocation(current.realWorldLoc);
                    creator.addManeuver("Goto");
                    distance = 0;
                }
                current = next;
                next = current.next;
            }
            PlanType plan = creator.getPlan();
            plan.setVehicle(selectedVehicles.get(curIndex));
            plan.setId(planid + "_" + selectedVehicles.get(curIndex++));

            mission.addPlan(plan);

            mission.save(false);
            getConsole().updateMissionListeners();

        }
    }

    void printMatrix(CoverageCell[][] mat) {
        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < mat[0].length; j++) {
                if (mat[i][j] != null)
                    System.out.print(mat[i][j].rep());
            }
            System.out.println();
        }
    }

    @Override
    public Image getIconImage() {
        return ImageUtils.getImage(PluginUtils.getPluginIcon(this.getClass()));
    }

    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    @Override
    public String getName() {
        return "Coverage Solver";
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            popup.add("Generate plans locally").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    solve();
                }
            });

            popup.add("Clear polygon").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    pe = null;
                    vertexCount = 0;
                }
            });

            popup.add("Settings").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    PropertiesEditor.editProperties(AreaCoveragePlanner.this, getConsole(), true);
                }
            });

            popup.show(source, event.getX(), event.getY());
        }
        else if (pe == null) {
            LocationType l = source.getRealWorldLocation(event.getPoint());
            pe = new PathElement(MapGroup.getMapGroupInstance(getConsole().getMission()), new MapType(), l);
            pe.setMyColor(Color.green.brighter());
            pe.setShape(true);
            pe.setFinished(true);
            pe.setStroke(new BasicStroke(2.0f));
            pe.addPoint(0, 0, 0, false);
            vertexCount = 1;
        }
        else {
            LocationType l = source.getRealWorldLocation(event.getPoint());
            double[] offsets = l.getOffsetFrom(pe.getCenterLocation());
            pe.addPoint(offsets[1], offsets[0], 0, false);
            vertexCount++;
        }

    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        adapter.mouseDragged(event, source);
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
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
