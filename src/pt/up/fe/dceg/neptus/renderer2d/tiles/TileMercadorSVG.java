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
 * 8/10/2011
 */
package pt.up.fe.dceg.neptus.renderer2d.tiles;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

import pt.up.fe.dceg.neptus.plugins.MapTileProvider;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.SvgUtil;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "Mercator SVG (Local)")
public class TileMercadorSVG extends Tile {

    private static final long serialVersionUID = -6947456498157990203L;

    protected static String tileClassId = TileMercadorSVG.class.getSimpleName();
    
    private static Map<String, TileMercadorSVG> tilesMap = Collections.synchronizedMap(new HashMap<String, TileMercadorSVG>());
    
    private static final Object lock = new Object();

    protected static final String fxWM = "/images/World_Blank_Map_Mercator_projection.svg";

    protected static final double w = 634.26801, h = 476.7276;
    
    protected static final LocationType centerOfImage = new LocationType();
    static {
        centerOfImage.setLatitude(-1.5);
        centerOfImage.translatePosition(-227E3, -12E3, 0);
        centerOfImage.setLongitude(10);
        centerOfImage.convertToAbsoluteLatLonDepth();
    }
    
    private static final int offsetX = 0, offsetY = 0;

    private static final int MAX_LEVEL_OF_DETAIL = 15;
    
    private static PrintTranscoder prm;
    private static PageFormat page;

    static {
        prm = loadWorld(fxWM, w, h);
        Paper paper = new Paper();
        paper.setSize(w, h);
        paper.setImageableArea(0, 0, w, h);
        page = new PageFormat();
        page.setPaper(paper);
    }

    public TileMercadorSVG(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }
    
    /**
     * @param id
     * @throws Exception
     */
    public TileMercadorSVG(String id) throws Exception {
        super(id);
    }

    protected static PrintTranscoder loadWorld(String fx, double w, double h) {
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String data = FileUtil.getFileAsString(FileUtil.getResourceAsFile(fx));
            Document wDoc = f.createDocument(null, new StringReader((String) data));
            wDoc = SvgUtil.cleanInkscapeSVG(wDoc);
            PrintTranscoder prm = new PrintTranscoder();
            prm.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, new Float(w));
            prm.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, new Float(h));
            TranscoderInput ti = new TranscoderInput(wDoc);
            prm.transcode(ti, null);
            return prm;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        catch (NoClassDefFoundError e) {
            System.out.print("Batik missing in the classpath. Proceding without worldmap.");
            return null;
        }
        catch (Error e) {
            e.printStackTrace();
            return null;
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.tiles.Tile#retryLoadingTile()
     */
    @Override
    public void retryLoadingTile() {
        // Does not make sense to retry
    }
    
    public static int getMaxLevelOfDetail() {
        return MAX_LEVEL_OF_DETAIL;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.tiles.Tile#createTileImage()
     */
    @Override
    protected void createTileImage() {
        if (getState() == TileState.DISPOSING)
            return;
        if (prm == null) {
            setState(TileState.FATAL_ERROR);
            lasErrorMessage = "Not able to load SVG Map painter!";
        }
        setState(TileState.LOADING);
        new Thread(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "]") {
            @Override
            public void run() {
                int msize = MapTileUtil.mapSize(levelOfDetail);
                double[] xyWC = { msize / 2, msize / 2 };
                Point2D xyWG = centerOfImage.getPointInPixel(levelOfDetail);
                
                BufferedImage cache = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = cache.createGraphics();

                g2.translate((xyWG.getX() - xyWC[0]), (xyWG.getY() - xyWC[1]));
                double zt = msize / w;
                g2.translate(0, (msize - h * zt) / 2);
                Graphics2D gg = null;
                if (tileX == 0) {
                    gg = (Graphics2D) g2.create();
                    gg.translate(-msize, -tileY * 256);
                    gg.scale(zt, zt);
                    gg.translate(offsetX, offsetY);
                }
                g2.translate(-tileX * 256, -tileY * 256);
                g2.scale(zt, zt);
                g2.translate(offsetX, offsetY);
                synchronized (lock) {
                    if (TileMercadorSVG.this.getState() != TileState.DISPOSING) {
                        if (gg != null)
                            prm.print(gg, page, 0);
                    }
                    else
                        return;
                    if (TileMercadorSVG.this.getState() != TileState.DISPOSING)
                        prm.print(g2, page, 0);
                    else
                        return;
                }
                if (gg != null)
                    gg.dispose();
                g2.dispose();
//                cache = (BufferedImage) GuiUtils.applyTransparency(cache, 0.2f);
                temporaryTransparencyDetectedOnImageOnDisk = false;
                image = cache;
                
//                loadTransformedImage();
                
                TileMercadorSVG.this.setState(TileState.LOADED);
                saveTile();
                //System.out.println(image);
            }
        }.start();
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.tiles.Tile#testForAlfaOnLoaddImage(java.awt.image.BufferedImage)
     */
    @Override
    protected void testForAlfaOnLoaddImage(BufferedImage img) {
        temporaryTransparencyDetectedOnImageOnDisk = false; // this has to be overwritten because the SVG has transparent parts
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.tiles.Tile#getTransparencyToApplyToImage()
     */
    @Override
    protected float getTransparencyToApplyToImage() {
        return 0.2f;
    }
    
    public static boolean isFetchableOrGenerated() {
        return false;
    }

    /**
     * @return the tilesMap
     */
    @SuppressWarnings("unchecked")
    public static <T extends Tile> Map<String, T> getTilesMap() {
        return (Map<String, T>) tilesMap;
    }
    
    /**
     * 
     */
    public static void clearDiskCache() {
        clearDiskCache(tileClassId);
    }

    /**
     * @return 
     * 
     */
    public static Vector<TileMercadorSVG> loadCache() {
        return loadCache(tileClassId);
    }

    public static void main(String[] args) throws Exception {
//        Tile tile = new TileMercadorSVG(MapTileUtil.tileXYToQuadKey(1, 1, 1));
//        TileMercadorSVG.loadCache();
    }
}
