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
 * 2009/09/27
 */
package pt.up.fe.dceg.neptus.plugins.acoustic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;
import pt.up.fe.dceg.neptus.util.gui.LBLUtil;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class LBLRangeLabel extends JXPanel {

    private static final Color COLOR_IDLE = new JXPanel().getBackground();
    private static final Color COLOR_ACCEPT = Color.GREEN.darker(); //new Color(140, 255, 170);
    private static final Color COLOR_REJECTED = new Color(245, 20, 40); //new Color(255, 210, 140);
    private static final Color COLOR_SURFACE = Color.blue;
    private static final Color COLOR_OLD = Color.GRAY.darker(); //new Color(255, 100, 100);

    
	private Timer timer = null;
	private TimerTask colorUpdaterTask = null;
	private TimerTask periodicTask = null;

    private String name = "?";
    private double range = -1;
    private boolean isAccepted = true;
    private String rejectionReason = "";
    
    //UI
    private JXLabel label = null;
    private long timeStampMillis = -1;
    
    /**
	 * 
	 */
	public LBLRangeLabel(String name) {
		this.name = name;
		initialize();
	}

	/**
	 * 
	 */
	private void initialize() {
		setBackgroundPainter(getCompoundBackPainter());
		label = new JXLabel("<html><b>"+name, JLabel.CENTER) {
		    @Override
            public void setText(String text) {
		        if (text.equals(getText()))
		            return;
		        super.setText(text);
		    };
		};
		label.setHorizontalTextPosition(JLabel.CENTER);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(new Font("Arial", Font.BOLD, 12));
		label.setForeground(Color.WHITE);
		label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setLayout(new BorderLayout());
		add(label, BorderLayout.CENTER);
		
		setPreferredSize(new Dimension(215, 180));
		setSize(new Dimension(215, 180));
	}

	
	/**
	 * @return the range
	 */
	public double getRange() {
		return range;
	}
	
	/**
	 * @param range the range to set
	 */
	public void setRange(double range) {
		this.range = range;
		//setAccepted(true, null);
		updateLabel();
	}
	
	/**
	 * @return the isAccepted
	 */
	public boolean isAccepted() {
		return isAccepted;
	}
	
	/**
	 * @param isAccepted the isAccepted to set
	 */
	public void setAccepted(boolean isAccepted, String rejectionReason) {
		this.isAccepted = isAccepted;
		this.rejectionReason = rejectionReason;
		if(isAccepted)
		    updateBackColor(COLOR_ACCEPT);
		else
		    updateBackColor(rejectionReason.contains("SURFACE")? COLOR_SURFACE : COLOR_REJECTED);
		setToolTipText(isAccepted ? I18n.text("Accepted")
				: (I18n.text("Rejected") + (rejectionReason != null ? " "
						+ rejectionReason : "")));
		updateLabel();
		revokeScheduleUpdateTask();
		scheduleUpdateTask();
	}
	
    /**
     * @param timeStampMillis
     */
    public void setTimeStampMillis(long timeStampMillis) {
        this.timeStampMillis = timeStampMillis;
        
        if (periodicTask == null) {
            periodicTask = new TimerTask() {
                @Override
                public void run() {
                    updateLabel(false);
                }
            };
            timer.scheduleAtFixedRate(periodicTask, 50, 1000);
        }
    }

    private void updateLabel() {
        updateLabel(true);
    }

	private void updateLabel(boolean triggerChangeColorIndicator) {
        String ellapsedTime = LBLUtil.writeTimeLabel(timeStampMillis);
    label.setText(I18n.textf("<html><b>Beacon %beaconid (%range m) %datetime <br>%reason", 
               "ch" + name, MathMiscUtils.round(range, 1), ellapsedTime,
                (isAccepted ? I18n.text("Accepted") : (I18n.text("Rejected") + (rejectionReason != null ? ": "
                        + rejectionReason : "")))));

	    if (triggerChangeColorIndicator) {
	        revokeScheduleUpdateTask();
	        scheduleUpdateTask();
	    }
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.Component#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		try {
			LBLRangeLabel cmp = (LBLRangeLabel) obj;
			return cmp.getName().equals(name);
		} catch (Exception e) {
			return false;
		}
	}

	private void scheduleUpdateTask() {
		if (colorUpdaterTask == null) {
			if (timer == null)
                timer = new Timer(LBLRangeLabel.class.getSimpleName() + " color updater: " + LBLRangeLabel.this.name, true);
			colorUpdaterTask = createTimerTask();
			timer.schedule(colorUpdaterTask, 1*1000);
		}
	}
	
	private void revokeScheduleUpdateTask() {
		if (colorUpdaterTask != null) {
			colorUpdaterTask.cancel();
			colorUpdaterTask = null;
		}
	}

	/**
	 * @return
	 */
	private TimerTask createTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
				updateBackColor(((Color) rectPainter.getFillPaint()).darker());
				colorUpdaterTask = createTimerTask2();
				timer.schedule(colorUpdaterTask, 4*1000);
			}
		};
	}

	private TimerTask createTimerTask2() {
		return new TimerTask() {
			@Override
			public void run() {
				updateBackColor(COLOR_OLD);
			}
		};
	}


	//Background Painter Stuff
	private RectanglePainter rectPainter;
	private CompoundPainter<JXPanel> compoundBackPainter;
	/**
	 * @return the rectPainter
	 */
	private RectanglePainter getRectPainter() {
		if (rectPainter == null) {
	        rectPainter = new RectanglePainter(5,5,5,5, 20,20);
	        rectPainter.setFillPaint(COLOR_IDLE);
	        rectPainter.setBorderPaint(COLOR_IDLE.darker().darker().darker());
	        rectPainter.setStyle(RectanglePainter.Style.BOTH);
	        rectPainter.setBorderWidth(2);
	        rectPainter.setAntialiasing(true);
		}
		return rectPainter;
	}
	/**
	 * @return the compoundBackPainter
	 */
	private CompoundPainter<JXPanel> getCompoundBackPainter() {
		compoundBackPainter = new CompoundPainter<JXPanel>(
					//new MattePainter(Color.BLACK),
					getRectPainter(), new GlossPainter());
		return compoundBackPainter;
	}
	/**
	 * @param color
	 */
	private void updateBackColor(Color color) {
		getRectPainter().setFillPaint(color);
		getRectPainter().setBorderPaint(color.darker());

		//this.setBackgroundPainter(getCompoundBackPainter());
		repaint();
	}

	/**
     * Call this to completely stop the timer that this class uses 
     */
    public void dispose() {
        revokeScheduleUpdateTask();
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }    
    }
    
	@Override
	protected void finalize() throws Throwable {
	    dispose();
	}
	
	public static void main(String[] args) {
		final LBLRangeLabel lb = new LBLRangeLabel("1");
		GuiUtils.testFrame(lb);
		lb.setRange(233.5);
		lb.setAccepted(false, "ABOVE_THRESHOLD");
		Timer timer = new Timer("LblRangeLabel Main");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				lb.setAccepted(!lb.isAccepted(), "ABOVE_THRESHOLD");
			}
		}, 6000, 6000);
	}
}
