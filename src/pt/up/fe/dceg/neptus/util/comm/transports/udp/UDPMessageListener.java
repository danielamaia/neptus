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
 * 2010/01/16
 */
package pt.up.fe.dceg.neptus.util.comm.transports.udp;

/**
 * @author pdias
 *
 */
public interface UDPMessageListener {

	/**
	 * @param req
	 */
	public void onUDPMessageNotification(UDPNotification req);
	
}
