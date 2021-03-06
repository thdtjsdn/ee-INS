/*
 * Copyright 2011 Danish Maritime Authority. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY Danish Maritime Authority ``AS IS'' 
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
 * either expressed or implied, of Danish Maritime Authority.
 * 
 */
package dk.frv.enav.ins.gui.route;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;

import dk.frv.enav.ins.EeINS;
import dk.frv.enav.ins.route.Route;
import dk.frv.enav.ins.route.RouteLoadException;
import dk.frv.enav.ins.route.RouteLoader;
import dk.frv.enav.ins.route.RouteManager;
import dk.frv.enav.ins.route.RoutesUpdateEvent;

/**
 * Route manager dialog
 */
public class RouteManagerDialog extends JDialog implements ActionListener, ListSelectionListener, TableModelListener,
		MouseListener {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = Logger.getLogger(RouteManagerDialog.class);

	protected RouteManager routeManager;

	private JButton propertiesBtn;
	private JButton zoomToBtn;
	private JButton reverseCopyBtn;
	private JButton deleteBtn;
	private JButton exportBtn;
	private JButton importBtn;
	private JButton closeBtn;
	private JButton activateBtn;

	private JScrollPane routeScrollPane;
	private JTable routeTable;
	private RoutesTableModel routesTableModel;
	private ListSelectionModel routeSelectionModel;

	private JButton exportAllBtn;

	private JButton metocBtn;

	private JButton copyBtn;

	public RouteManagerDialog(JFrame parent) {
		super(parent, "Route Manager", true);
		routeManager = EeINS.getRouteManager();

		setSize(600, 400);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(parent);

		propertiesBtn = new JButton("Properties");
		propertiesBtn.addActionListener(this);
		activateBtn = new JButton("Activate");
		activateBtn.addActionListener(this);
		zoomToBtn = new JButton("Zoom to");
		zoomToBtn.addActionListener(this);
		reverseCopyBtn = new JButton("Reverse copy");
		reverseCopyBtn.addActionListener(this);
		deleteBtn = new JButton("Delete");
		deleteBtn.addActionListener(this);
		exportBtn = new JButton("Export");
		exportBtn.addActionListener(this);
		exportAllBtn = new JButton("Export All");
		exportAllBtn.addActionListener(this);
		importBtn = new JButton("Import");
		importBtn.addActionListener(this);
		closeBtn = new JButton("Close");
		closeBtn.addActionListener(this);
		metocBtn = new JButton("METOC");
		metocBtn.addActionListener(this);
		copyBtn = new JButton("Copy");
		copyBtn.addActionListener(this);

		routeTable = new JTable();
		routesTableModel = new RoutesTableModel(routeManager);
		routesTableModel.addTableModelListener(this);
		routeTable.setShowHorizontalLines(false);
		routeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		routeScrollPane = new JScrollPane(routeTable);
		routeScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		routeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		routeTable.setFillsViewportHeight(true);
		//TODO: Comment this line when using WindowBuilder
		routeTable.setModel(routesTableModel);
		for (int i = 0; i < 3; i++) {
			if (i == 2) {
				routeTable.getColumnModel().getColumn(i).setPreferredWidth(50);
			} else {
				routeTable.getColumnModel().getColumn(i).setPreferredWidth(175);
			}
		}
		routeSelectionModel = routeTable.getSelectionModel();
		routeSelectionModel.addListSelectionListener(this);
		routeTable.setSelectionModel(routeSelectionModel);
		routeTable.addMouseListener(this);
		

		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(routeScrollPane, GroupLayout.DEFAULT_SIZE, 427, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
							.addComponent(closeBtn, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(zoomToBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(activateBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(propertiesBtn, GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
							.addComponent(copyBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
							.addComponent(exportBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(deleteBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(reverseCopyBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(metocBtn, GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
							.addComponent(exportAllBtn, GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
							.addComponent(importBtn, GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(propertiesBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(activateBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(zoomToBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(copyBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(reverseCopyBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(deleteBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(exportBtn)
							.addGap(7)
							.addComponent(metocBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(exportAllBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(importBtn))
						.addComponent(routeScrollPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE))
					.addGap(28)
					.addComponent(closeBtn)
					.addContainerGap())
		);

		getContentPane().setLayout(groupLayout);

		int selectRow = routeManager.getActiveRouteIndex();
		if (selectRow < 0 && routeManager.getRouteCount() > 0) {
			selectRow = 0;
		}
		if (selectRow >= 0) {
			routeSelectionModel.setSelectionInterval(selectRow, selectRow);
		}

		updateTable();
		updateButtons();
	}

	private void updateButtons() {
		boolean routeSelected = (routeTable.getSelectedRow() >= 0);
		boolean activeSelected = routeManager.isActiveRoute(routeTable.getSelectedRow());

		// LOG.info("---------------------------------------");
		// LOG.info("routeSelected: " + routeSelected);
		// LOG.info("routeTable.getSelectedRow(): " +
		// routeTable.getSelectedRow());
		// LOG.info("activeSelected: " + activeSelected);
		// LOG.info("routeManager.isRouteActive(): " +
		// routeManager.isRouteActive());
		// LOG.info("activeRoute: " + routeManager.getActiveRouteIndex());
		// LOG.info("\n\n");

		activateBtn.setEnabled(routeSelected);
		activateBtn.setText(activeSelected ? "Deactivate" : "Activate");

		if (routeSelected) {
			if (routeManager.isRouteActive()) {
				activateBtn.setEnabled(activeSelected);
			} else {
				activateBtn.setEnabled(true);
			}
		}

		propertiesBtn.setEnabled(routeSelected);
		zoomToBtn.setEnabled(routeSelected);
		reverseCopyBtn.setEnabled(routeSelected);
		copyBtn.setEnabled(routeSelected);
		deleteBtn.setEnabled(routeSelected && !activeSelected);
		metocBtn.setEnabled(routeSelected);
		exportBtn.setEnabled(routeSelected);		
	}

	private void updateTable() {
		int selectedRow = routeTable.getSelectedRow();
		// Update routeTable
		routesTableModel.fireTableDataChanged();
		// routeTable.doLayout();
		updateButtons();
		if (selectedRow >= 0 && selectedRow < routeTable.getRowCount()) {
			routeSelectionModel.setSelectionInterval(selectedRow, selectedRow);
		}
	}

	private void close() {
		dispose();
	}

	private void activateRoute() {
		LOG.debug("Activate route");
		if (routeTable.getSelectedRow() >= 0) {
			if (routeManager.isRouteActive()) {
				routeManager.deactivateRoute();
			} else {
				routeManager.activateRoute(routeTable.getSelectedRow());
			}

			updateTable();
		}
	}

	private void zoomTo() {
		
		// TODO ChartPanel should implement a method that given a route does the
		// following
		// TODO disable auto follow
		// TODO find minx, miny and maxx, maxy
		// TODO center and scale map to include whole route
		// 
	}
	
	private void copy() {
		if(routeTable.getSelectedRow() >= 0) {
			routeManager.routeCopy(routeTable.getSelectedRow());
			updateTable();
		}
	}

	private void reverseCopy() {
		if(routeTable.getSelectedRow() >= 0) {
			routeManager.routeReverse(routeTable.getSelectedRow());
			updateTable();
		}		
	}

	private void properties() {
		int i = routeTable.getSelectedRow();
		if (i >= 0) {
			RoutePropertiesDialog routePropertiesDialog = new RoutePropertiesDialog((Window) this, routeManager, i);
			routePropertiesDialog.setVisible(true);
		}
	}

	private void metocProperties() {
		int i = routeTable.getSelectedRow();
		if (i >= 0) {
			RouteMetocDialog routeMetocDialog = new RouteMetocDialog((Window) this, routeManager, i);
			routeMetocDialog.setVisible(true);
			routeManager.notifyListeners(RoutesUpdateEvent.METOC_SETTINGS_CHANGED);
		}
	}

	private void delete() {
		if (routeTable.getSelectedRow() >= 0) {
			routeManager.removeRoute(routeTable.getSelectedRow());
			updateTable();
		}
	}
	
	private void exportToFile() {
		exportToFile(routeTable.getSelectedRow());
	}

	private void exportToFile(int routeId) {
		if (routeId < 0) {
			return;
		}
		
		Route route = routeManager.getRoute(routeId);
		
		JFileChooser fc = new JFileChooser(System.getProperty("user.dir") + "/routes/");
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		
		fc.addChoosableFileFilter(new FileNameExtensionFilter("Simple route text format", "txt", "TXT"));
		fc.setAcceptAllFileFilterUsed(true);
		File f = new File(route.getName() + ".txt");
		fc.setSelectedFile(f);
		
		if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fc.getSelectedFile();
				
		if (!fc.getSelectedFile().toString().contains(".txt")) {
			file = new File(fc.getSelectedFile().getPath() + ".txt");
		}
		
		if (file.exists()) {
			if (JOptionPane.showConfirmDialog(this, "File exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION) != 0) {
				exportToFile(routeId);
				return;
			}
		}

		if(!RouteLoader.saveSimple(route, file)) {
			JOptionPane.showMessageDialog(EeINS.getMainFrame(), "Route save error", "Route not saved", JOptionPane.ERROR_MESSAGE);
		}
		
	}

	private void importFromFile() {
		// Get filename from dialog
		JFileChooser fc = new JFileChooser(System.getProperty("user.dir") + "/routes");
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(true);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("Simple route text format", "txt", "TXT"));
		fc.addChoosableFileFilter(new FileNameExtensionFilter("ECDIS900 V3 route", "rou", "ROU"));
		fc.addChoosableFileFilter(new FileNameExtensionFilter("Navisailor 3000 route", "rt3", "RT3"));
		fc.setAcceptAllFileFilterUsed(true);
		

		if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		for (File file : fc.getSelectedFiles()) {
			try {
				routeManager.loadFromFile(file);
			} catch (RouteLoadException e) {
				JOptionPane.showMessageDialog(this, e.getMessage() + ": " + file.getName(), "Route load error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		updateTable();
		routeSelectionModel.setSelectionInterval(routeTable.getRowCount() - 1, routeTable.getRowCount() - 1);
	}
	
	private void exportAllToFile() {
		for (int i=0; i < routeTable.getRowCount(); i++) {
			exportToFile(i);
		}		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == closeBtn) {
			close();
		} else if (e.getSource() == propertiesBtn) {
			properties();
		} else if (e.getSource() == activateBtn) {
			activateRoute();
		} else if (e.getSource() == zoomToBtn) {
			zoomTo();
		} else if (e.getSource() == copyBtn) {
			copy();
		} else if (e.getSource() == reverseCopyBtn) {
			reverseCopy();
		} else if (e.getSource() == deleteBtn) {
			delete();
		} else if (e.getSource() == metocBtn) {
			metocProperties();
		} else if (e.getSource() == exportBtn) {
			exportToFile();
		} else if (e.getSource() == exportAllBtn) {
			exportAllToFile();
		} else if (e.getSource() == importBtn) {
			importFromFile();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			properties();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// ListSelectionModel lsm = (ListSelectionModel) e.getSource();

		// int firstIndex = e.getFirstIndex();
		// int lastIndex = e.getLastIndex();
		// boolean isAdjusting = e.getValueIsAdjusting();
		// LOG.info("Event for indexes " + firstIndex + " - " + lastIndex +
		// "; isAdjusting is " + isAdjusting + "; selected indexes:");

		updateButtons();

	}

	@Override
	public void tableChanged(TableModelEvent e) {
		if (e.getColumn() == 2) {
			// Visibility has changed
			routeManager.notifyListeners(RoutesUpdateEvent.ROUTE_VISIBILITY_CHANGED);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}
}
