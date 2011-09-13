/*
 * Copyright 2011 Danish Maritime Safety Administration. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY Danish Maritime Safety Administration ``AS IS'' 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of Danish Maritime Safety Administration.
 * 
 */
package dk.frv.enav.ins.layers.ais;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.bbn.openmap.MapBean;
import com.bbn.openmap.event.MapMouseListener;
import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMList;

import dk.frv.enav.ins.EeINS;
import dk.frv.enav.ins.ais.AisTarget;
import dk.frv.enav.ins.ais.AisTargets;
import dk.frv.enav.ins.ais.AtoNTarget;
import dk.frv.enav.ins.ais.IAisTargetListener;
import dk.frv.enav.ins.ais.SarTarget;
import dk.frv.enav.ins.ais.SartGraphic;
import dk.frv.enav.ins.ais.VesselTarget;
import dk.frv.enav.ins.event.NavigationMouseMode;
import dk.frv.enav.ins.gps.GpsHandler;
import dk.frv.enav.ins.gui.MainFrame;
import dk.frv.enav.ins.gui.MapMenu;

public class AisLayer extends OMGraphicHandlerLayer implements IAisTargetListener, Runnable, MapMouseListener {

	private static final Logger LOG = Logger.getLogger(AisLayer.class);
	private static final long serialVersionUID = 1L;

	private long minRedrawInterval = 5 * 1000; // 5 sec

	private AisTargets aisTargets = null;
	private MapBean mapBean = null;
	private MainFrame mainFrame = null;
	private IntendedRouteInfoPanel intendedRouteInfoPanel = new IntendedRouteInfoPanel();
	private AisTargetInfoPanel aisTargetInfoPanel = new AisTargetInfoPanel();
	private SarTargetInfoPanel sarTargetInfoPanel = new SarTargetInfoPanel();
	private MapMenu aisTargetMenu = null;

	private Map<Long, TargetGraphic> targets = new HashMap<Long, TargetGraphic>();
	private OMGraphicList graphics = new OMGraphicList();

	private Date lastRedraw = new Date();
	private Boolean redrawPending = false;

	private OMGraphic closest = null;
	private OMGraphic selectedGraphic;

	public AisLayer() {
		// graphics.setVague(false);
		(new Thread(this)).start();
	}

	@Override
	public void run() {
		while (true) {
			EeINS.sleep(1000);
			if (isRedrawPending()) {
				updateLayer();
			}
		}
	}
	
	private void updateLayer() {
		updateLayer(false);
	}

	private void updateLayer(boolean force) {
		if (!force) {
			long elapsed = (new Date()).getTime() - getLastRedraw().getTime();
			if (elapsed < minRedrawInterval) {
				return;
			}
		}
		doPrepare();
	}

	@Override
	public synchronized void targetUpdated(AisTarget aisTarget) {
		long mmsi = aisTarget.getMmsi();
		TargetGraphic targetGraphic = targets.get(mmsi);

		if (aisTarget.isGone()) {
			if (targetGraphic != null) {
				// Remove target
				// LOG.info("Target has gone: " + mmsi);
				targets.remove(mmsi);
				graphics.remove(targetGraphic);
				setRedrawPending(true);
				updateLayer();
			}
			return;
		}

		// Create and insert
		if (targetGraphic == null) {
			if (aisTarget instanceof VesselTarget) {
				targetGraphic = new VesselTargetGraphic(aisTargets.getNameCache().getName(mmsi));
			} else if (aisTarget instanceof SarTarget) {
				targetGraphic = new SarTargetGraphic();
			} else if (aisTarget instanceof AtoNTarget) {
				targetGraphic = new AtonTargetGraphic();
			} else {
				LOG.error("Unknown target type");
				return;
			}
			targets.put(mmsi, targetGraphic);
			graphics.add(targetGraphic);
		}
		
		
		boolean forceRedraw = false;		

		if (aisTarget instanceof VesselTarget) {
			// Maybe we would like to force redraw
			VesselTarget vesselTarget = (VesselTarget) aisTarget; 
			VesselTargetGraphic vesselTargetGraphic = (VesselTargetGraphic) targetGraphic;
			if (vesselTarget.getSettings().isShowRoute() && vesselTarget.hasIntendedRoute()
					&& !vesselTargetGraphic.getRouteGraphic().isVisible()) {
				forceRedraw = true;
			} else if (!vesselTarget.getSettings().isShowRoute() && vesselTargetGraphic.getRouteGraphic().isVisible()) {
				forceRedraw = true;
			}

			targetGraphic.update(vesselTarget);
		} else if (aisTarget instanceof SarTarget) {
			targetGraphic.update((SarTarget) aisTarget);
		} else if (aisTarget instanceof AtoNTarget) {
			targetGraphic.update((AtoNTarget) aisTarget);
		}

		targetGraphic.project(getProjection());
		
		// System.out.println("targets.size() : " + targets.size());
		// System.out.println("graphics.size(): " + graphics.size() + "\n---");

		setRedrawPending(true);
		updateLayer(forceRedraw);
	}

	private void setRedrawPending(boolean val) {
		synchronized (redrawPending) {
			redrawPending = val;
			if (!val) {
				lastRedraw = new Date();
			}
		}
	}

	public boolean isRedrawPending() {
		synchronized (redrawPending) {
			return redrawPending;
		}
	}

	private Date getLastRedraw() {
		synchronized (redrawPending) {
			return lastRedraw;
		}
	}

	@Override
	public synchronized OMGraphicList prepare() {
		// long start = System.nanoTime();
		Iterator<TargetGraphic> it = targets.values().iterator();

		while (it.hasNext()) {
			TargetGraphic target = it.next();
			target.setMarksVisible(getProjection());
		}

		setRedrawPending(false);
		graphics.project(getProjection());
		// System.out.println("Finished AisLayer.prepare() in " +
		// EeINS.elapsed(start) + " ms\n---");
		return graphics;
	}

	public long getMinRedrawInterval() {
		return minRedrawInterval;
	}

	public void setMinRedrawInterval(long minRedrawInterval) {
		this.minRedrawInterval = minRedrawInterval;
	}

	@Override
	public void paint(Graphics g) {
		//long start = System.nanoTime();
		super.paint(g);
		setRedrawPending(false);
		//System.out.println("Finished AisLayer.paint() in " + EeINS.elapsed(start) + " ms\n---");
	}

	@Override
	public void findAndInit(Object obj) {
		if (obj instanceof AisTargets) {
			aisTargets = (AisTargets) obj;
			aisTargets.addListener(this);
		}
		if (obj instanceof MapBean) {
			mapBean = (MapBean) obj;
		}
		if (obj instanceof MainFrame) {
			mainFrame = (MainFrame) obj;
			mainFrame.getGlassPanel().add(intendedRouteInfoPanel);
			mainFrame.getGlassPanel().add(aisTargetInfoPanel);
			mainFrame.getGlassPanel().add(sarTargetInfoPanel);
		}
		if (obj instanceof GpsHandler) {
			sarTargetInfoPanel.setGpsHandler((GpsHandler) obj);
		}
		if (obj instanceof MapMenu) {
			aisTargetMenu = (MapMenu) obj;
		}
	}

	@Override
	public void findAndUndo(Object obj) {
		if (obj == aisTargets) {
			aisTargets.removeListener(this);
		}
	}

	public MapMouseListener getMapMouseListener() {
		return this;
	}

	@Override
	public String[] getMouseModeServiceList() {
		String[] ret = new String[1];
		ret[0] = NavigationMouseMode.modeID; // "Gestures"
		return ret;
	}

	@Override
	public boolean mouseClicked(MouseEvent e) {
		if (this.isVisible()) {
			if (e.getButton() == MouseEvent.BUTTON3) {
				selectedGraphic = null;
				OMList<OMGraphic> allClosest = graphics.findAll(e.getX(), e.getY(), 5.0f);

				for (OMGraphic omGraphic : allClosest) {
					if (omGraphic instanceof IntendedRouteWpCircle || omGraphic instanceof VesselTargetTriangle
							|| omGraphic instanceof IntendedRouteLegGraphic || omGraphic instanceof SartGraphic) {
						selectedGraphic = omGraphic;
						break;
					}
				}

				if (selectedGraphic instanceof VesselTargetTriangle) {
					VesselTargetTriangle vtt = (VesselTargetTriangle) selectedGraphic;
					VesselTargetGraphic vesselTargetGraphic = vtt.getVesselTargetGraphic();
					mainFrame.getGlassPane().setVisible(false);
					aisTargetMenu.aisMenu(vesselTargetGraphic);
					aisTargetMenu.setVisible(true);
					aisTargetMenu.show(this, e.getX() - 2, e.getY() - 2);
					aisTargetInfoPanel.setVisible(false);
					return true;
				} else if (selectedGraphic instanceof IntendedRouteWpCircle) {
					IntendedRouteWpCircle wpCircle = (IntendedRouteWpCircle) selectedGraphic;
					VesselTarget vesselTarget = wpCircle.getIntendedRouteGraphic().getVesselTarget();
					mainFrame.getGlassPane().setVisible(false);
					aisTargetMenu.aisSuggestedRouteMenu(vesselTarget);
					aisTargetMenu.setVisible(true);
					aisTargetMenu.show(this, e.getX() - 2, e.getY() - 2);
					aisTargetInfoPanel.setVisible(false);
					return true;
				} else if (selectedGraphic instanceof IntendedRouteLegGraphic) {
					IntendedRouteLegGraphic wpCircle = (IntendedRouteLegGraphic) selectedGraphic;
					VesselTarget vesselTarget = wpCircle.getIntendedRouteGraphic().getVesselTarget();
					mainFrame.getGlassPane().setVisible(false);
					aisTargetMenu.aisSuggestedRouteMenu(vesselTarget);
					aisTargetMenu.setVisible(true);
					aisTargetMenu.show(this, e.getX() - 2, e.getY() - 2);
					aisTargetInfoPanel.setVisible(false);
					return true;
				} else if (selectedGraphic instanceof SartGraphic) {
					SartGraphic sartGraphic = (SartGraphic) selectedGraphic;
					SarTarget sarTarget = sartGraphic.getSarTargetGraphic().getSarTarget();
					mainFrame.getGlassPane().setVisible(false);
					aisTargetMenu.sartMenu(this, sarTarget);
					aisTargetMenu.setVisible(true);
					aisTargetMenu.show(this, e.getX() - 2, e.getY() - 2);
					sarTargetInfoPanel.setVisible(false);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// hackish?
		if (e.getComponent() instanceof MapBean)
			aisTargetMenu.setVisible(false);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved() {
	}

	@Override
	public boolean mouseMoved(MouseEvent e) {
		if (this.isVisible()) {
			OMGraphic newClosest = null;
			OMList<OMGraphic> allClosest = graphics.findAll(e.getX(), e.getY(), 3.0f);

			for (OMGraphic omGraphic : allClosest) {

				//System.out.println("omGraphic: " + omGraphic.getClass());

				if (omGraphic instanceof IntendedRouteWpCircle || omGraphic instanceof IntendedRouteLegGraphic
						|| omGraphic instanceof VesselTargetTriangle || omGraphic instanceof SartGraphic) {
					// System.out.println("omGraphic: " + omGraphic.getClass());
					newClosest = omGraphic;
					break;
				}
			}

			if (newClosest != closest) {
				Point containerPoint = SwingUtilities.convertPoint(mapBean, e.getPoint(), mainFrame);
				if (newClosest instanceof IntendedRouteWpCircle) {
					closest = newClosest;
					IntendedRouteWpCircle wpCircle = (IntendedRouteWpCircle) newClosest;
					intendedRouteInfoPanel.setPos((int) containerPoint.getX(), (int) containerPoint.getY() - 10);
					intendedRouteInfoPanel.showWpInfo(wpCircle);
					mainFrame.getGlassPane().setVisible(true);
					aisTargetInfoPanel.setVisible(false);
					sarTargetInfoPanel.setVisible(false);
					return true;
				} else if (newClosest instanceof IntendedRouteLegGraphic) {
					closest = newClosest;
					IntendedRouteLegGraphic legGraphic = (IntendedRouteLegGraphic) newClosest;
					intendedRouteInfoPanel.setPos((int) containerPoint.getX(), (int) containerPoint.getY() - 10);					
					intendedRouteInfoPanel.showLegInfo(legGraphic);
					mainFrame.getGlassPane().setVisible(true);
					aisTargetInfoPanel.setVisible(false);
					sarTargetInfoPanel.setVisible(false);
					return true;
				} else if (newClosest instanceof VesselTargetTriangle) {
					closest = newClosest;
					VesselTargetTriangle vesselTargetTriangle = (VesselTargetTriangle) newClosest;
					VesselTarget vesselTarget = vesselTargetTriangle.getVesselTargetGraphic().getVesselTarget();
					aisTargetInfoPanel.setPos((int) containerPoint.getX(), (int) containerPoint.getY() - 10);
					aisTargetInfoPanel.showAisInfo(vesselTarget);
					mainFrame.getGlassPane().setVisible(true);
					intendedRouteInfoPanel.setVisible(false);
					sarTargetInfoPanel.setVisible(false);
					return true;
				} else if (newClosest instanceof SartGraphic) {
					closest = newClosest;
					SartGraphic sartGraphic = (SartGraphic) newClosest;
					sarTargetInfoPanel.setPos((int) containerPoint.getX(), (int) containerPoint.getY() - 10);
					sarTargetInfoPanel.showSarInfo(sartGraphic.getSarTargetGraphic().getSarTarget());
					mainFrame.getGlassPane().setVisible(true);
					intendedRouteInfoPanel.setVisible(false);
					aisTargetInfoPanel.setVisible(false);
					return true;
				} else {
					intendedRouteInfoPanel.setVisible(false);
					aisTargetInfoPanel.setVisible(false);
					sarTargetInfoPanel.setVisible(false);
					mainFrame.getGlassPane().setVisible(false);
					closest = null;
				}

			}
		} else {
			intendedRouteInfoPanel.setVisible(false);
			aisTargetInfoPanel.setVisible(false);
			sarTargetInfoPanel.setVisible(false);
		}
		return false;
	}

	@Override
	public boolean mousePressed(MouseEvent e) {
		return false;
	}

	@Override
	public boolean mouseReleased(MouseEvent e) {
		return false;
	}

}