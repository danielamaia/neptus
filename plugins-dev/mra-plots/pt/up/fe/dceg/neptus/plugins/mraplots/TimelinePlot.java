/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: jqcorreia
 * Jun 4, 2013
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import java.awt.Component;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.gui.Timeline;
import pt.up.fe.dceg.neptus.gui.TimelineChangeListener;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.importers.lsf.LsfMraLog;
import pt.up.fe.dceg.neptus.mra.plots.MraTimeSeriesPlot;

/**
 * @author jqcorreia
 *
 */
public class TimelinePlot extends MraTimeSeriesPlot implements TimelineChangeListener {
    
    Timeline timeline;
    long firstTimestamp;
    private long lastTimestamp;
    Vector<String> fieldsToPlot;
    LinkedHashMap<String, IMraLog> parsers = new LinkedHashMap<String, IMraLog>();
    
    /**
     * @param panel
     */
    public TimelinePlot(MRAPanel panel, Vector<String> fieldsToPlot) {
        super(panel);
        this.fieldsToPlot = fieldsToPlot;
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        for (String field : fieldsToPlot) {
            String messageName = field.split("\\.")[0];
            if (index.getFirstMessageOfType(messageName) == -1)
                return false;
        }
        return true;
    }

    // In this case process serves as parser initializer
    @Override
    public void process(LsfIndex source) {
        firstTimestamp = (long) (source.timeOf(0) * 1000);
        lastTimestamp = (long) (source.timeOf(source.getNumberOfMessages()-2) * 1000);
        
        System.out.println((int)(lastTimestamp - firstTimestamp));
//        System.out.println(firstTimestamp + " " + source.timeOf(0));
//        System.out.println(lastTimestamp + " " + source.timeOf(source.getNumberOfMessages()-2) + " " + source.getNumberOfMessages());
//        System.out.println(lastTimestamp - firstTimestamp);
//        
//        System.out.println(source.getMessage(source.getNumberOfMessages()-2));
        
        timeline = new Timeline(0, (int)(lastTimestamp - firstTimestamp), 24, 1000, false);
        timeline.getSlider().setValue(0);
        timeline.addTimelineChangeListener(this);
        
        for (String field : fieldsToPlot) {
            String messageName = field.split("\\.")[0];
            parsers.put(messageName, new LsfMraLog(index, messageName));
        }
        
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        Component comp = super.getComponent(source, timestep);
        JPanel panel = new JPanel(new MigLayout());

        panel.add(comp, "w 100%, h 100%, wrap");
        panel.add(timeline, "w 100%, h 80");
        
        return panel;
    }

    @Override
    public String getName() {
        return Arrays.toString(fieldsToPlot.toArray(new String[0]));
    }    
    
    @Override
    public void timelineChanged(int value) {
        try {
            long ts = firstTimestamp + value;

            for (String field : fieldsToPlot) {
                String messageName = field.split("\\.")[0];
                String fieldName = field.split("\\.")[1];
                addValue(firstTimestamp + value, field,
                        parsers.get(messageName).getEntryAtOrAfter(ts).getDouble(fieldName));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
