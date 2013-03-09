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
 * 2009/03/25
 */
package pt.up.fe.dceg.neptus.console.plugins;

/**
 * @author pdias
 *
 */
public interface LockableSubPanel {
	public void lock();
	public void unLock();
	public boolean isLocked();
}
