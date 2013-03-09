/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Feb 6, 2013
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import java.awt.Canvas;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener;
import pt.up.fe.dceg.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.JmeComponent;
import pt.up.fe.dceg.plugins.tidePrediction.Harbors;

import com.jme3.system.JmeCanvasContext;

/**
 * Object to serve as bridge between jME3 and Neptus Overrides and extends everything Neptus needs/expects
 * 
 * @author Margarida Faria
 */
@PluginDescription(author = "Margarida Faria", name = "Bathymetry 3D", icon = "pt/up/fe/dceg/neptus/plugins/r3d/icon/icon3D_v4.png")
public class Bathymetry3D extends SimpleMRAVisualization implements LogMarkerListener {
    private static final long serialVersionUID = 6749840764382852669L;
    // private static final String HEIGHT_MAP_RELATIVE_PATH = "/colorBathyForHeightMap.jpg";
    private boolean started = false;
    private final MarkerObserver markerObserver;

    private JmeComponent app;

    public Bathymetry3D(MRAPanel panel) {
        super(panel);
        // add markers listener
        markerObserver = new MarkerObserver();
    }

    /**
     * Generates a bathymetry image combining the data from the log and filling the rest with interpolation This image
     * is saved as a .jpg file and also used to show the data in 3D Starts the jME3 component for 3D visualisation
     * 
     * @param source the log source
     * @param timestep
     */
    @Override
    public JComponent getVisualization(final IMraLogGroup source, final double timestep) {
        if (started) {
            return this;
        }
        // PerformanceTest.initMem();
        // PerformanceTest.printToLog(PrintType.START, "");
        started = true;
        markerObserver.setSource(source);
        // add bad drivers listener
        final BadDrivers badDrivers = new BadDrivers();
        badDrivers.addBadDriversListener(new JMEListener() {
            @Override
            public void badDriversOccurred(final BadDriversEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        Bathymetry3D.this.removeAll();
                        showError(I18n.text("No support. ") + evt.getSource().toString());
                    }
                });
            }
        });
        setupInterface(badDrivers);
        return this;
    }


    private void setupInterface(final BadDrivers badDrivers) {
        setLayout(new MigLayout("wrap 1", "[100%]"));
        // Set tide calibration settings
        final JComboBox<Harbors> harborList = new JComboBox<Harbors>(Harbors.values());

        // Create a regular text field.
        final JLabel errorMsg = new JLabel();
        // errorMsg.setBackground(this.getBackground());
        // errorMsg.set

        JButton startButton = new JButton("Start 3D");
        startButton.setVerticalTextPosition(AbstractButton.CENTER);
        startButton.setHorizontalTextPosition(AbstractButton.LEADING); // aka LEFT, for left-to-right locales
        startButton.setMnemonic(KeyEvent.VK_ENTER);
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Harbors selectedHarbor = (Harbors) harborList.getSelectedItem();
                if (selectedHarbor != null) {

                    startJme(badDrivers, selectedHarbor);
                }
                else {
                    errorMsg.setText(I18n.text("Please select a harbor first"));
                }
            }

        });

        JButton startNoHarborButton = new JButton(I18n.text("Start 3D without tide adjustment"));
        startNoHarborButton.setVerticalTextPosition(AbstractButton.CENTER);
        startNoHarborButton.setHorizontalTextPosition(AbstractButton.LEADING); // aka LEFT, for left-to-right locales
        startNoHarborButton.setMnemonic(KeyEvent.VK_ENTER);
        startNoHarborButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startJme(badDrivers, null);
            }

        });

        this.add(harborList, "span, split 2, center");
        this.add(startButton, "wrap");
        this.add(startNoHarborButton, "span, center, wrap");
        this.add(errorMsg, "span, center");

    }

    private void startJme(final BadDrivers badDrivers, final Harbors selectedHarbor) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                app = new JmeComponent(source, badDrivers, markerObserver, selectedHarbor);
                app.createApplicationAndCanvas(getBounds());
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                JmeCanvasContext context = (JmeCanvasContext) app.getContext();
                Canvas canvas = context.getCanvas();
                removeAll();
                add(canvas);
                revalidate();

                app.startCanvas();
                Bathymetry3D.this.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        super.componentResized(e);
                        app.resize(getBounds());
                    }
                });
            }
        });
    }
    private void showError(String msg) {
        JLabel errorLabel = new JLabel(msg, SwingConstants.CENTER);
        Bathymetry3D.this.add(errorLabel);
        Bathymetry3D.this.revalidate();
    }

    /**
     * Doesn't support variableTimeSteps yet
     */
    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    /**
     * Can be applied if log has GPSFix, EstimatedState and BottomDistance data
     */
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        if (source.getLsfIndex().getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
            return source.getLog("EstimatedState") != null;
        }
        else {
            return (source.getLog("EstimatedState") != null && source.getLog("BottomDistance") != null && source
                    .getLog("GPSFix") != null);
        }

    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        markerObserver.addLogMarker(marker);
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        markerObserver.removeLogMarker(marker);
    }

    @Override
    public void GotoMarker(LogMarker marker) {
        // TODO
    }
}
