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
 * 14/Set/2004
 */
package pt.up.fe.dceg.neptus.types.map;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.mp.Dimension;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.mp.MapChangeListener;
import pt.up.fe.dceg.neptus.types.XmlInputMethods;
import pt.up.fe.dceg.neptus.types.XmlInputMethodsFromFile;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.coord.LocationsHolder;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.Dom4JUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.XMLValidator;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * Refactored in 17/11/2006.
 * 
 * @author Zé Carlos (DCEG-FEUP)
 * @author Paulo Dias
 */
public class MapType implements XmlOutputMethods, XmlInputMethods, XmlInputMethodsFromFile {

    // <FROM Map>
    protected static final String DEFAULT_ROOT_ELEMENT = "map";

    private String href = "";
    private LinkedList<String> notesList = new LinkedList<String>();

    private Document doc;
    protected boolean isLoadOk = true;

    private String originalFilePath = "";

    private boolean changed = false;

    private LinkedHashMap<String, AbstractElement> elements = new LinkedHashMap<String, AbstractElement>();
    private Dimension defaultDimension;
    LinkedList<MapChangeListener> changeListeners = new LinkedList<MapChangeListener>();
    private LocationType centerLocation = new LocationType();
    private String name = "Unnamed Map";
    private String id = "id_" + System.currentTimeMillis();
    private String type = "Other";

    private MissionType mission = null;
    private MapGroup mapGroup = null;

    private String description = "All aditional information should go here. "
            + "Enter things like surrounding locations, previous missions in the local, etc...";

    /**
     * @param url
     * 
     */
    public MapType(String url) {
        super();
        loadFile(url);
    }

    public MapType(Dimension defaultDimension) {
        this.defaultDimension = defaultDimension;
        this.centerLocation = new LocationType();
    }

    public MapType(LocationType center) {
        this.defaultDimension = new Dimension(30.0f, 30.0f, 30.0f);
        this.centerLocation = center;
    }

    public MapType() {
        this.defaultDimension = new Dimension(30.0f, 30.0f, 30.0f);
        this.centerLocation = new LocationType();
    }

    public void addChangeListener(MapChangeListener listener) {
        if (!changeListeners.contains(listener))
            changeListeners.add(listener);
    }

    public void removeChangeListener(MapChangeListener listener) {
        changeListeners.remove(listener);
    }

    public void clearChangeListeners() {
        NeptusLog.pub().warn("Removing " + changeListeners.size() + " dead listeners");
        changeListeners.clear();
    }

    public void warnChangeListeners(MapChangeEvent changeEvent) {
        for (int i = 0; i < changeListeners.size(); i++) {
            MapChangeListener tmp = (MapChangeListener) changeListeners.get(i);
            tmp.mapChanged(changeEvent);
        }
    }

    public Dimension getDimension() {
        return this.defaultDimension;
    }

    /**
     * This function adds a new object to this map. Use this function to build the map of the world
     * 
     * @param obstacleCenter The center of the new obstacle
     * @param obstacleSize The size of the obstacle to add
     */
    public void addObject(AbstractElement newObject) {
        // newObject.setParentMap(this);
        elements.put(newObject.getName(), newObject);

        MapChangeEvent event = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
        event.setChangedObject(newObject);
        event.setSourceMap(this);
        warnChangeListeners(event);

    }

    /**
     * Returns a map object by referring its identifier (name)
     * 
     * @param name the identifier of the object in this map
     * @return A map object
     */
    public AbstractElement getObject(String name) {
        return (AbstractElement) elements.get(name);
    }

    public String[] getObjectNames() {
        return elements.keySet().toArray(new String[0]);
        /*
         * Object[] names = new Object[MapObjects.size()]; for (int i = 0; namesEnum.hasMoreElements(); i++) { names[i]
         * = namesEnum.nextElement(); } return names;
         */
    }

    public void remove(String objectName) {

        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED);
        changeEvent.setChangedObject((AbstractElement) elements.get(objectName));
        changeEvent.setSourceMap(this);
        elements.remove(objectName);
        warnChangeListeners(changeEvent);
    }

    /**
     * Returns all the objects in this map as a collection
     * 
     * @return The collection of all objects in this map
     */
    public Collection<AbstractElement> getObjects() {
        return elements.values();
    }

    /**
     * Returns this map's current number of objects
     * 
     * @return The number of objects in this map
     */
    public int numObjects() {
        return elements.size();
    }

    /**
     * @return Returns the centerLocation.
     */
    public LocationType getCenterLocation() {
        return centerLocation;
    }

    /**
     * @param centerLocation The centerLocation to set.
     */
    public void setCenterLocation(LocationType centerLocation) {
        this.centerLocation.setLocation(centerLocation);
    }

    /**
     * @return Returns the defaultDimension.
     */
    public Dimension getDefaultDimension() {
        return defaultDimension;
    }

    /**
     * @param defaultDimension The defaultDimension to set.
     */
    public void setDefaultDimension(Dimension defaultDimension) {
        this.defaultDimension = defaultDimension;
    }

    /**
     * @return Returns the mapDescription.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param mapDescription The mapDescription to set.
     */
    public void setDescription(String mapDescription) {
        this.description = mapDescription;
    }

    /**
     * @return Returns the mapID.
     */
    public String getId() {
        return id;
    }

    /**
     * @param mapID The mapID to set.
     */
    public void setId(String mapID) {
        this.id = mapID;
    }

    /**
     * @return Returns the mapName.
     */
    public String getName() {
        return name;
    }

    /**
     * @param mapName The mapName to set.
     */
    public void setName(String mapName) {
        this.name = mapName;
    }

    public void getReferentialPoints() {
        Object[] objs = getObjects().toArray();
        for (int i = 0; i < objs.length; i++) {
            AbstractElement mo = (AbstractElement) objs[i];
            if (objs[i] instanceof MarkElement) {
                LocationsHolder.putAbstractLocationPoint(mo.getCenterLocation());
            }
            if (objs[i] instanceof HomeReference) {
                LocationsHolder.putAbstractLocationPoint(mo.getCenterLocation());
            }
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlInputMethods#load(org.dom4j.Element)
     */
    public boolean load(Element elem) {
        doc = Dom4JUtil.elementToDocument(elem);
        if (doc == null) {
            isLoadOk = false;
            return false;
        }
        return load();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlInputMethods#load(java.lang.String)
     */
    public boolean load(String xml) {
        try {
            doc = DocumentHelper.parseText(xml);
        }
        catch (DocumentException e) {
            e.printStackTrace();
            isLoadOk = false;
            return false;
        }
        return load();
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean load() {

        elements = new LinkedHashMap<String, AbstractElement>();

        long initTime = System.currentTimeMillis();
        try {
            isLoadOk = true;//validate(doc);

            this.setId(doc.selectSingleNode("/map/header/id").getText());
            this.setName(doc.selectSingleNode("/map/header/name").getText());
            Node nd = doc.selectSingleNode("/map/header/type");
            if (nd != null)
                this.setType(nd.getText());
            nd = doc.selectSingleNode("/map/header/description");
            if (nd != null)
                this.setDescription(nd.getText());

            nd = doc.selectSingleNode("/map/header/type");
            if (nd != null)
                this.setType(nd.getText());

            List<?> lst = doc.selectNodes("/map/header/notes/note");
            ListIterator<?> lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                notesList.add(elem.getText());
            }

            lst = doc.selectNodes("/map/features/mark");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();

                // MarkElement mm = new MarkElement(elem.asXML());
                MarkElement mm = new MarkElement();
                mm.load(elem);
                elements.put(mm.getId(), mm);

            }

            lst = doc.selectNodes("/map/features/transponder");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                // TransponderElement mm = new TransponderElement(elem.asXML());
                TransponderElement mm = new TransponderElement();
                mm.load(elem);
                elements.put(mm.getId(), mm);
            }

            lst = doc.selectNodes("/map/features/geometry[type='" + GeometryElement.PARALLELEPIPED + "']");
            lst.addAll(doc.selectNodes("/map/features/geometry[type='Parallel Piped']"));
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                ParallelepipedElement mm = new ParallelepipedElement();
                // mm.load(elem.asXML());
                mm.load(elem);
                elements.put(mm.getId(), mm);
            }

            lst = doc.selectNodes("/map/features/geometry[type='" + GeometryElement.CYLINDER + "']");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                CylinderElement mm = new CylinderElement();
                // mm.load(elem.asXML());
                mm.load(elem);
                elements.put(mm.getId(), mm);
            }

            lst = doc.selectNodes("/map/features/geometry[type='" + GeometryElement.ELLIPSOID + "']");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                EllipsoidElement mm = new EllipsoidElement();
                // mm.load(elem.asXML());
                mm.load(elem);
                elements.put(mm.getId(), mm);
            }

            lst = doc.selectNodes("/map/features/path");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                // PathElement mm = new PathElement(elem.asXML());
                PathElement mm = new PathElement();

                mm.load(elem);
                elements.put(mm.getId(), mm);
            }

            lst = doc.selectNodes("/map/features/image");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                // ImageElement im = new ImageElement(elem.asXML(), getOriginalFilePath());
                ImageElement im = new ImageElement();
                im.setOriginalFilePath(originalFilePath);
                im.load(elem);
                elements.put(im.getId(), im);
            }

            lst = doc.selectNodes("/map/features/model3d");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                Model3DElement im = new Model3DElement();
                im.setOriginalFilePath(originalFilePath);
                im.load(elem);
                elements.put(im.getId(), im);
            }

            LinkedHashMap<String, Class<? extends AbstractElement>> elementClasses = new LinkedHashMap<String, Class<? extends AbstractElement>>();

            elementClasses.put("MineDangerAreaElement", MineDangerAreaElement.class);
            elementClasses.put("QRouteElement", QRouteElement.class);

            for (String name : elementClasses.keySet()) {
                lst = doc.selectNodes("/map/features/" + name);
                lstIt = lst.listIterator();
                while (lstIt.hasNext()) {
                    Element elem = (Element) lstIt.next();
                    AbstractElement el = elementClasses.get(name).getConstructor(MapGroup.class, MapType.class)
                            .newInstance(null, null);
                    el.load(elem);
                    elements.put(el.getId(), el);
                }
            }

            // Set parentwood
            for (AbstractElement elem : elements.values()) {
                elem.setMissionType(getMission());
                elem.setMapGroup(getMapGroup());
                elem.setParentMap(this);
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            isLoadOk = false;
            long totalTime = System.currentTimeMillis() - initTime;
            NeptusLog.pub().info(this + ": Total map load time: " + totalTime + " ms.");
            return false;
        }
        isLoadOk = true;
        setChanged(false);
        long totalTime = System.currentTimeMillis() - initTime;
        NeptusLog.pub().info(this + ": Total map load time: " + totalTime + " ms.");
        return true;
    }

    /**
     * @param url
     */
    public boolean loadFile(String url) {
        // Sets the href of this map to be the url from where it was loaded
        setHref(url);

        originalFilePath = new File(url).getAbsolutePath();
        SAXReader reader = new SAXReader();
        try {
            this.doc = reader.read(url);
        }
        catch (DocumentException e) {
            NeptusLog.pub().error("loading map "+ url , e);
        }
        return load();
//        String fileAsString = FileUtil.getFileAsString(url);
//        if (fileAsString == null) {
//            JOptionPane.showMessageDialog(ConfigFetch.getSuperParentFrame(), "<html>File " + url
//                    + " was not loaded.<br>The file was not found.</html>");
//            return false;
//        }
//
//        return load(fileAsString);
    }

    public boolean loadFile(File file) {
        return loadFile(file.getAbsolutePath());
    }

    public boolean isLoadOk() {
        return isLoadOk;
    }

    public static boolean validate(Document doc) {
        System.out.println("validating map: " + doc.toString());
        System.out.println("schema: " + ConfigFetch.getMapSchemaLocation());
        try {
            String sLoc = new File(ConfigFetch.getMapSchemaLocation()).getAbsoluteFile().toURI().toASCIIString();

            XMLValidator xmlVal = new XMLValidator(doc, sLoc);
            boolean ret = xmlVal.validate();
            return ret;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Map:validate", e);
            return false;
        }
    }

    public static boolean validate(String xml) {
        try {
            String sLoc = new File(ConfigFetch.getMapSchemaLocation()).getAbsoluteFile().toURI().toASCIIString();
            XMLValidator xmlVal = new XMLValidator(xml, sLoc);
            boolean ret = xmlVal.validate();
            return ret;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Map:validate", e);
            return false;
        }
    }

    public static boolean validate(File file) {
        try {
            // System.err.println(file.getAbsoluteFile().toURI());
            String xml = FileUtil.getFileAsString(file);
            return validate(xml);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Map:validate", e);
            return false;
        }
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addComment(ConfigFetch.getSaveAsCommentForXML());

        Element header = root.addElement("header");
        header.addElement("id").addText(getId());
        header.addElement("name").addText(getName());
        header.addElement("type").addText(getType());
        if (!description.equalsIgnoreCase(""))
            header.addElement("description").addText(getDescription());
        if (notesList.size() != 0) {
            Element notes = header.addElement("notes");
            for (Iterator<String> iter = notesList.iterator(); iter.hasNext();) {
                notes.addElement("note").addText((String) iter.next());
            }
        }

        Element features = root.addElement("features");

        for (AbstractElement elem : elements.values()) {

            if (elem instanceof ImageElement)
                ((ImageElement) elem).setOriginalFilePath(getHref());
            else if (elem instanceof Model3DElement)
                ((Model3DElement) elem).setOriginalFilePath(getHref());

            features.add(elem.asElement());
        }

        return document;
    }

    public boolean showSaveDialog() {
        JFrame tmp = new JFrame();
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
        fc.setFileView(new NeptusFileView());
        // File file = fc.getSelectedFile();
        fc.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                try {
                    if (!f.exists() || !f.canRead())
                        return false;
                    if (f.getCanonicalPath().toLowerCase().endsWith("xml")
                            || f.getCanonicalPath().toLowerCase().endsWith("nmap"))
                        return true;
                    if (f.isDirectory())
                        return true;
                }
                catch (Exception e) {
                }
                return false;
            }

            public String getDescription() {
                return "Neptus Mission Map files";
            }
        });
        fc.setDialogTitle("Choose file to save the map '" + getId() + "'...");
        int response = fc.showSaveDialog(tmp);
        if (response == JFileChooser.CANCEL_OPTION)
            return false;
        else {
            String filename = fc.getSelectedFile().getAbsolutePath();
            String filenameNoCase = filename.toLowerCase();
            if (filenameNoCase.endsWith(".xml") || filenameNoCase.endsWith(".nmap"))
                return saveFile(filename);
            else
                return saveFile(filename + ".nmap");
        }
    }

    public boolean saveFile(String filename) {
        setHref(filename);
        boolean ret = FileUtil.saveToFile(filename, FileUtil.getAsPrettyPrintFormatedXMLString(asDocument()));

        if (!ret) {
            setHref(null);
            return false;
        }

        setChanged(false);

        NeptusLog.pub().info("The map '" + getId() + "' was saved to '" + getHref() + "'");

        return true;
    }

    public LinkedHashMap<String, AbstractElement> getElements() {
        return elements;
    }

    public void setElements(LinkedHashMap<String, AbstractElement> elements) {
        this.elements = elements;
    }

    public LinkedList<AbstractElement> getAllElements() {
        LinkedList<AbstractElement> elems = new LinkedList<AbstractElement>();
        elems.addAll(elements.values());
        return elems;
    }

    public LinkedHashMap<String, TransponderElement> getTranspondersList() {
        LinkedHashMap<String, TransponderElement> transList = new LinkedHashMap<String, TransponderElement>();
        for (AbstractElement elem : elements.values()) {
            if (elem.getType().equals("Transponder"))
                transList.put(elem.getId(), (TransponderElement) elem);
        }
        return transList;
    }

    public LinkedHashMap<String, MarkElement> getMarksList() {
        LinkedHashMap<String, MarkElement> markList = new LinkedHashMap<String, MarkElement>();
        for (AbstractElement elem : elements.values()) {
            if (elem.getType().equals("Mark"))
                markList.put(elem.getId(), (MarkElement) elem);
        }
        return markList;
    }

    @Override
    public String toString() {
        return getId();
    }

    public MapGroup getMapGroup() {
        return mapGroup;
    }

    public void setMapGroup(MapGroup mapGroup) {
        this.mapGroup = mapGroup;
    }

    public MissionType getMission() {
        return mission;
    }

    public void setMission(MissionType mission) {
        this.mission = mission;
    }
}
