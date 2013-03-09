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
package pt.up.fe.dceg.neptus.mme;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.ScalableElement;

public class ScaleObjectEdit extends AbstractUndoableEdit {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AbstractElement element;
	private double[] originalDimension, newDimension;

	public ScaleObjectEdit(AbstractElement element, double[] originalDimension, double[] newDimension) {
		this.element = element;
		this.originalDimension = originalDimension;
		this.newDimension = newDimension;
	}
	
	@Override
	public void undo() throws CannotUndoException {
		if (element instanceof ScalableElement) {
			((ScalableElement)element).setDimension(originalDimension);
			MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
			mce.setSourceMap(element.getParentMap());
			mce.setChangedObject(element);
			mce.setChangeType(MapChangeEvent.OBJECT_SCALED);
			mce.setMapGroup(element.getMapGroup());
			element.getParentMap().warnChangeListeners(mce);
		}
	}
	
	@Override
	public void redo() throws CannotRedoException {
		if (element instanceof ScalableElement) {
			((ScalableElement)element).setDimension(newDimension);
			MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
			mce.setSourceMap(element.getParentMap());
			mce.setChangedObject(element);
			mce.setMapGroup(element.getMapGroup());
			element.getParentMap().warnChangeListeners(mce);
		}
	}
	
	@Override
	public boolean canRedo() {
		return true;
	}
	
	@Override
	public boolean canUndo() {
		return true;
	}
	
	@Override
	public String getPresentationName() {
		return "Scale the "+element.getType()+" '"+element.getId()+"'";
	}
}
