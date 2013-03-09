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
 * Feb 16, 2013
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.l2fprod.common.beans.editor.NumberPropertyEditor;
import com.l2fprod.common.util.converter.ConverterRegistry;

/**
 * @author pdias
 *
 */
public class NumberEditor<T extends Number> extends NumberPropertyEditor {
    
    protected final Class<T> classType;
    
    protected T minValue = null;
    protected T maxValue = null;
    
    private Pattern pattern;
    protected String elementPattern;
    private Color errorColor = new Color(255, 108, 108);
    // private Color syncColor = new Color(108, 255, 108);
    // private Color blueColor = new Color(108, 108, 255);
    private Border defaultBorder = null;
    private Border errorBorder = null;

    @SuppressWarnings("unchecked")
    public NumberEditor(Class<T> type, T minValue, T maxValue) {
        this(type);
        
        if (type == Double.class) {
            if (minValue == null)
                minValue = (T) new Double(Double.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Double(Double.MAX_VALUE);
            if (minValue.doubleValue() > maxValue.doubleValue())
                minValue = maxValue;
        }
        else if (type == Float.class) {
            if (minValue == null)
                minValue = (T) new Float(Float.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Float(Float.MAX_VALUE);
            if (minValue.floatValue() > maxValue.floatValue())
                minValue = maxValue;
        }
        else if (type == Long.class) {
            if (minValue == null)
                minValue = (T) new Long(Long.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Long(Long.MAX_VALUE);
            if (minValue.longValue() > maxValue.longValue())
                minValue = maxValue;
        }
        else if (type == Integer.class) {
            if (minValue == null)
                minValue = (T) new Integer(Integer.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Integer(Integer.MAX_VALUE);
            if (minValue.intValue() > maxValue.intValue())
                minValue = maxValue;
        }
        else if (type == Short.class) {
            if (minValue == null)
                minValue = (T) new Short(Short.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Short(Short.MAX_VALUE);
            if (minValue.shortValue() > maxValue.shortValue())
                minValue = maxValue;
        }
        else if (type == Byte.class) {
            if (minValue == null)
                minValue = (T) new Byte(Byte.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Byte(Byte.MAX_VALUE);
            if (minValue.byteValue() > maxValue.byteValue())
                minValue = maxValue;
        }
        else {
            minValue = maxValue = null;
        }

        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    
    public NumberEditor(Class<T> type) {
        super(type);
        this.classType = type;
        
        if (this.classType == Double.class || this.classType == Float.class) {
            elementPattern = ArrayListEditor.REAL_PATTERN;
        }
        else if (this.classType == Byte.class) {
            elementPattern = "[01]+";
        } 
        else {
            elementPattern = ArrayListEditor.INTEGER_PATTERN;
        }
        
        ((JTextField) editor).addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
//                char keyChar = e.getKeyChar();
//                if (Character.isAlphabetic(keyChar))
//                    e.consume();
//                if ((classType == Double.class || classType == Float.class) && !(Character.isDigit(keyChar) || keyChar == '.' || keyChar == 'e' || keyChar == 'E' || keyChar == '+' || keyChar == '-'))
//                    e.consume();
//                else if ((classType == Long.class || classType == Integer.class || classType == Short.class) && !Character.isDigit(keyChar))
//                        e.consume();
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                boolean checkOk = true;
                String txt = ((JTextField) editor).getText();
                try {
                    convertFromString(txt);
                }
                catch (Exception e1) {
                    checkOk = false;
                }
                if (!checkOk) {
                    if (errorBorder == null) {
                        errorBorder = BorderFactory.createLineBorder(errorColor, 2);
                        defaultBorder = ((JTextField) editor).getBorder();
                    }
                    
                    ((JTextField) editor).setBorder(errorBorder);
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                }
                else {
                    ((JTextField) editor).setBorder(defaultBorder);
                }
            }

        });
        
        ((JTextField) editor).addFocusListener(new FocusAdapter() {
            private T oldVal = null;
            public void focusGained(FocusEvent fe) {
                try {
                    oldVal = convertFromString(((JTextField) editor).getText());
                }
                catch (Exception e) {
                    oldVal = null;
                }
            }

            public void focusLost(FocusEvent fe) {
                try {
                    T newVal = (T) convertFromString(((JTextField) editor).getText());
                    firePropertyChange(oldVal, newVal);
                }
                catch (Exception e) {                   
                    ((JTextField) editor).setText(convertToString(oldVal));
                }
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    private T convertFromString(String txt) {
        if (pattern == null) {
            pattern = Pattern.compile(elementPattern, Pattern.CASE_INSENSITIVE);
        }
        Matcher m = pattern.matcher(((JTextField) editor).getText());
        boolean checkOk = m.matches();
        if (!checkOk)
            throw new NumberFormatException();
        
        T valueToReq = (T) ConverterRegistry.instance().convert(this.classType, txt);
        if (minValue != null && maxValue != null) {
            if (valueToReq.doubleValue() < minValue.doubleValue()) {
                ((JTextField) editor).setText(convertToString(minValue));
                valueToReq = minValue;
            }
            if (valueToReq.doubleValue() > maxValue.doubleValue()) {
                ((JTextField) editor).setText(convertToString(maxValue));
                valueToReq = maxValue;
            }
        }
        
        return valueToReq;
    }

    protected String convertToString(Object value) {
        return (String) ConverterRegistry.instance().convert(String.class, value);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
