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
 * 2011/04/16
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class DisplayColorSymbol extends SymbolLabel {

    private Color displayColor = Color.MAGENTA;
    
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#initialize()
	 */
	@Override
	protected void initialize() {
		setSize(10, 10);
		setPreferredSize(new Dimension(10, 10));
		super.initialize();
	}
	
//	/* (non-Javadoc)
//	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#isRightClickable()
//	 */
//	@Override
//	public boolean isRightClickable() {
//	    return super.isActive();
//	}
	
	/**
     * @return the displayColor
     */
    public Color getDisplayColor() {
        return displayColor;
    }
    
    /**
     * @param displayColor the displayColor to set
     */
    public void setDisplayColor(Color displayColor) {
        this.displayColor = displayColor;
    }
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
	 */
	@Override
	public void paint(Graphics2D g, JXPanel c, int width, int height) {
		Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.scale(width/10.0, height/10.0);
		
		RoundRectangle2D rect = new RoundRectangle2D.Double(1,1,10,10, 0,0);
		g2.setColor(new Color(0,0,0,0));
		g2.fill(rect);
		
		if (isActive()) {
			g2.setColor(getActiveColor());
			Shape shape = new RoundRectangle2D.Double(0, 0, 10, 10, 2, 2);
			g2.fill(shape);
            g2.setColor(getDisplayColor());
            shape = new RoundRectangle2D.Double(1, 1, 8, 8, 2, 2);
            g2.fill(shape);
		}
	}
	
	public static void main(String[] args) {
		DisplayColorSymbol symb1 = new DisplayColorSymbol();
		symb1.setActive(true);
		symb1.setSize(50, 50);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.BLACK);
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel,"",400,400);
		
		try {Thread.sleep(5000);} catch (Exception e) {}
		symb1.blink(true);
		try {Thread.sleep(5000);} catch (Exception e) {}
		symb1.blink(false);
	}
}
