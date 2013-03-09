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
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.renderer3d;

import pt.up.fe.dceg.neptus.console.plugins.JVideoPanelConsole;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.gui.editor.ComboEditor;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

public class ProjectionPropertiesProvider implements PropertiesProvider {

    ProjectionObj pObj;

    public ProjectionPropertiesProvider(ProjectionObj po) {
        pObj = po;
    }

    public DefaultProperty[] getProperties() {
        DefaultProperty xrel = PropertiesEditor.getPropertyInstance("x offset Camera to vehicle", "Camera",
                Double.class, pObj.getXOffSet(), true);
        DefaultProperty yrel = PropertiesEditor.getPropertyInstance("y offset Camera to vehicle", "Camera",
                Double.class, pObj.getYOffSet(), true);
        DefaultProperty zrel = PropertiesEditor.getPropertyInstance("z offset Camera to vehicle", "Camera",
                Double.class, pObj.getZOffSet(), true);
        DefaultProperty vang = PropertiesEditor.getPropertyInstance("Vertical Capture angle", "Camera", Double.class,
                pObj.getViewAngleVert(), true);
        DefaultProperty hang = PropertiesEditor.getPropertyInstance("Horizontal Capture angle", "Camera", Double.class,
                pObj.getViewAngleHor(), true);
        DefaultProperty pan = PropertiesEditor.getPropertyInstance("Pan angle", "Camera", Double.class, pObj.getPan(),
                true);
        DefaultProperty tilt = PropertiesEditor.getPropertyInstance("Tilt angle", "Camera", Double.class,
                pObj.getTilt(), true);
        DefaultProperty mtop = PropertiesEditor.getPropertyInstance("Margin Top", "Camera Image Margin", Integer.class,
                pObj.getPtop(), true);
        DefaultProperty mdown = PropertiesEditor.getPropertyInstance("Margin Botton", "Camera Image Margin",
                Integer.class, pObj.getPdown(), true);
        DefaultProperty mleft = PropertiesEditor.getPropertyInstance("Margin Left", "Camera Image Margin",
                Integer.class, pObj.getPleft(), true);
        DefaultProperty mright = PropertiesEditor.getPropertyInstance("Margin Right", "Camera Image Margin",
                Integer.class, pObj.getPright(), true);
        DefaultProperty act = PropertiesEditor.getPropertyInstance("Activate Projection", "Projection Obj",
                Boolean.class, pObj.isActivated(), true);
        DefaultProperty hgr = PropertiesEditor.getPropertyInstance("Horizontal Grid resolution", "Projection Obj",
                Integer.class, pObj.getGridResolutionH(), true);
        DefaultProperty vgr = PropertiesEditor.getPropertyInstance("Vertical Grid resolution", "Projection Obj",
                Integer.class, pObj.getGridResolutionV(), true);
        DefaultProperty intdist = PropertiesEditor.getPropertyInstance("Distance from intercection", "Projection Obj",
                Double.class, pObj.getDistanceWall(), true);
        DefaultProperty video = PropertiesEditor.getPropertyInstance("Video Panel Mapping", "Projection Obj",
                JVideoPanelConsole.class, pObj.getVideoSource(), true);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(
                video,
                new ComboEditor<JVideoPanelConsole>(pObj.getRender().getConsole().getSubPanelsOfClass(JVideoPanelConsole.class)
                        .toArray(new JVideoPanelConsole[0])));
        return new DefaultProperty[] { xrel, yrel, zrel, vang, hang, pan, tilt, mtop, mdown, mleft, mright, act, hgr,
                vgr, intdist, video };
    }

    public String getPropertiesDialogTitle() {

        return "Projection Properties";
    }

    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    public void setProperties(Property[] properties) {
        for (Property p : properties) {
            if (p.getName().equals("x offset Camera to vehicle")) {
                pObj.setXOffSet((Double) p.getValue());
            }
            if (p.getName().equals("y offset Camera to vehicle")) {
                pObj.setYOffSet((Double) p.getValue());
            }
            if (p.getName().equals("z offset Camera to vehicle")) {
                pObj.setZOffSet((Double) p.getValue());
            }

            if (p.getName().equals("Vertical Capture angle")) {
                pObj.setViewAngleVert((Double) p.getValue());
            }
            if (p.getName().equals("Horizontal Capture angle")) {
                pObj.setViewAngleHor((Double) p.getValue());
            }

            if (p.getName().equals("Pan angle")) {
                pObj.setPan((Double) p.getValue());
            }
            if (p.getName().equals("Tilt angle")) {
                pObj.setTilt((Double) p.getValue());
            }

            if (p.getName().equals("Margin Top")) {
                pObj.setPtop((Integer) p.getValue());
            }
            if (p.getName().equals("Margin Botton")) {
                pObj.setPdown((Integer) p.getValue());
            }
            if (p.getName().equals("Margin Left")) {
                pObj.setPleft((Integer) p.getValue());
            }
            if (p.getName().equals("Margin Right")) {
                pObj.setPright((Integer) p.getValue());
            }

            if (p.getName().equals("Activate Projection")) {
                pObj.setActivated((Boolean) p.getValue());
            }

            if (p.getName().equals("Horizontal Grid resolution")) {
                pObj.setGridResolutionH((Integer) p.getValue());
            }
            if (p.getName().equals("Vertical Grid resolution")) {
                pObj.setGridResolutionV((Integer) p.getValue());
            }

            if (p.getName().equals("Distance from intercection")) {
                pObj.setDistanceWall((Double) p.getValue());
            }

            if (p.getName().equals("Video Panel Mapping")) {
                pObj.setVideoSource((JVideoPanelConsole) p.getValue());
            }
        }
        pObj.rebuildPointArray();
    }

}
