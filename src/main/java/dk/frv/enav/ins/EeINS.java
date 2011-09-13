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
package dk.frv.enav.ins;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.bbn.openmap.MapHandler;
import com.bbn.openmap.PropertyConsumer;

import dk.frv.enav.ins.ais.AisTargets;
import dk.frv.enav.ins.gps.GnssTime;
import dk.frv.enav.ins.gps.GpsHandler;
import dk.frv.enav.ins.gui.MainFrame;
import dk.frv.enav.ins.msi.MsiHandler;
import dk.frv.enav.ins.nmea.NmeaFileSensor;
import dk.frv.enav.ins.nmea.NmeaSensor;
import dk.frv.enav.ins.nmea.NmeaSerialSensor;
import dk.frv.enav.ins.nmea.NmeaStdinSensor;
import dk.frv.enav.ins.nmea.NmeaTcpSensor;
import dk.frv.enav.ins.nmea.SensorType;
import dk.frv.enav.ins.route.RouteManager;
import dk.frv.enav.ins.services.ais.AisServices;
import dk.frv.enav.ins.services.shore.ShoreServices;
import dk.frv.enav.ins.settings.SensorSettings;
import dk.frv.enav.ins.settings.Settings;
import dk.frv.enav.ins.util.OneInstanceGuard;
import dk.frv.enav.ins.util.UpdateCheckerThread;

public class EeINS {
	
	private static String VERSION;
	private static String MINORVERSION;
	private static Logger LOG;	
	private static MainFrame mainFrame;	
	private static MapHandler mapHandler;
	private static Settings settings;
	private static Properties properties = new Properties();
	private static NmeaSensor aisSensor;
	private static NmeaSensor gpsSensor;
	private static GpsHandler gpsHandler;
	private static AisTargets aisTargets;
	private static RouteManager routeManager;
	private static ShoreServices shoreServices;
	private static AisServices aisServices;
	private static MsiHandler msiHandler;
	private static UpdateCheckerThread updateThread;
	private static ExceptionHandler exceptionHandler = new ExceptionHandler();
	
	public static void main(String[] args) {
		// Set up log4j logging
		DOMConfigurator.configure("log4j.xml");
        LOG = Logger.getLogger(EeINS.class);
        
        // Set default exception handler        
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        
        // Determine version
        Package p = EeINS.class.getPackage();
        MINORVERSION = p.getImplementationVersion();
        LOG.info("Starting ee-INS version " + MINORVERSION);
        LOG.info("Copyright (C) 2011 Danish Maritime Safety Administration");
        LOG.info("This program comes with ABSOLUTELY NO WARRANTY.");
        LOG.info("This is free software, and you are welcome to redistribute it under certain conditions.");
        LOG.info("For details see LICENSE file.");
        VERSION = MINORVERSION.split("[-]")[0];
        
        // Load properties
        loadProperties();
                
        // Create the bean context (map handler)
        mapHandler = new MapHandler();
        
        // Load settings or get defaults and add to bean context       
        if (args.length > 0) {        	
        	settings = new Settings(args[0]);
        } else {
        	settings = new Settings();
        }       
        LOG.info("Using settings file: " + settings.getSettingsFile());
        settings.loadFromFile();
        mapHandler.add(settings);
        
        // Determine if instance already running and if that is allowed
        OneInstanceGuard guard = new OneInstanceGuard("eeins.lock");
        if (!settings.getGuiSettings().isMultipleInstancesAllowed() && guard.isAlreadyRunning()) {
        	JOptionPane.showMessageDialog(null, "One application instance already running. Stop instance or restart computer.", "Error", JOptionPane.ERROR_MESSAGE);
        	System.exit(1);
        }
        
        // Start sensors
        startSensors();
        
        // Enable GPS timer by adding it to bean context
        GnssTime.init();
        mapHandler.add(GnssTime.getInstance());
        
        // Start position handler and add to bean context
        gpsHandler = new GpsHandler();
        mapHandler.add(gpsHandler);        
        
        // Start AIS target monitoring
        aisTargets = new AisTargets();        
        mapHandler.add(aisTargets);
        
        // Load routeManager and register as GPS data listener
        routeManager = RouteManager.loadRouteManager();
        mapHandler.add(routeManager);
        
        // Create shore services
        shoreServices = new ShoreServices(getSettings().getEnavSettings());
        mapHandler.add(shoreServices);
        
        // Create AIS services
        aisServices = new AisServices();
        mapHandler.add(aisServices);
        
        // Create MSI handler
        msiHandler = new MsiHandler(getSettings().getEnavSettings());
        mapHandler.add(msiHandler);
        
        // Create plugin components
        createPluginComponents();
        
        // Create and show GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

        // Start thread to handle software updates
        updateThread = new UpdateCheckerThread();
        mapHandler.add(updateThread);
        
	}
	
	private static void startSensors() {
		SensorSettings sensorSettings = settings.getSensorSettings();
        switch (sensorSettings.getAisConnectionType()) {
		case NONE:
			aisSensor = new NmeaStdinSensor();
			break;
		case TCP:
			aisSensor = new NmeaTcpSensor(sensorSettings.getAisHostOrSerialPort(), sensorSettings.getAisTcpPort()); 
			break;
		case SERIAL:
			aisSensor = new NmeaSerialSensor(sensorSettings.getAisHostOrSerialPort());
			break;		
		case FILE:
			aisSensor = new NmeaFileSensor(sensorSettings.getAisFilename(), sensorSettings);
			break;
		default:
			LOG.error("Unknown sensor connection type: " + sensorSettings.getAisConnectionType());
		}
        
        if (aisSensor != null) {
        	aisSensor.addSensorType(SensorType.AIS);
        }

        switch (sensorSettings.getGpsConnectionType()) {
        case NONE:
        	gpsSensor = new NmeaStdinSensor();
        	break;
        case TCP:
        	gpsSensor = new NmeaTcpSensor(sensorSettings.getGpsHostOrSerialPort(), sensorSettings.getGpsTcpPort());
        	break;
        case SERIAL:
        	gpsSensor = new NmeaSerialSensor(sensorSettings.getGpsHostOrSerialPort());
        	break;
        case FILE:
        	gpsSensor = new NmeaFileSensor(sensorSettings.getGpsFilename(), sensorSettings);
        	break;
        case AIS_SHARED:
			gpsSensor = aisSensor;
			break;
        default:
			LOG.error("Unknown sensor connection type: " + sensorSettings.getAisConnectionType());
        }
        
        if (gpsSensor != null) {
        	gpsSensor.addSensorType(SensorType.GPS);
        }
        
        if (aisSensor != null) {
        	aisSensor.setSimulateGps(sensorSettings.isSimulateGps());
        	aisSensor.setSimulatedOwnShip(sensorSettings.getSimulatedOwnShip());
        	aisSensor.start();
        	// Add ais sensor to bean context
        	mapHandler.add(aisSensor);
        }
        if (gpsSensor != null && gpsSensor != aisSensor) {
        	gpsSensor.setSimulateGps(sensorSettings.isSimulateGps());
        	gpsSensor.setSimulatedOwnShip(sensorSettings.getSimulatedOwnShip());
        	gpsSensor.start();
        	// Add gps sensor to bean context
        	mapHandler.add(gpsSensor);
        }
        
	}
	
	private static void loadProperties() {
		InputStream in = EeINS.class.getResourceAsStream("/eeins.properties");		
		try {
			if (in == null) {
				throw new IOException("Properties file not found");
			}
			properties.load(in);
			in.close();
		} catch (IOException e) {
			LOG.error("Failed to load resources: " + e.getMessage());
		}		
	}
	
	private static void createAndShowGUI() {
		// Set the look and feel.
		initLookAndFeel();
		
		// Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);
        
        // Create and set up the main window        
		mainFrame = new MainFrame();
		mainFrame.setVisible(true);
	}
	
	private static void initLookAndFeel() {
		try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { 
        	LOG.error("Failed to set look and feed: " + e.getMessage());
        }

	}
	
	public static void closeApp() {
		closeApp(false);
	}
	
	public static void closeApp(boolean restart) {
		// Shutdown routine
		mainFrame.saveSettings();
		settings.saveToFile();
		routeManager.saveToFile();
		msiHandler.saveToFile();
		aisTargets.saveToFile();		
		LOG.info("Closing ee-INS");
		System.exit(restart ? 2 : 0);
	}
	
	private static void createPluginComponents() {
		Properties props = getProperties();
		String componentsValue = props.getProperty("eeins.plugin_components");
		if (componentsValue == null) {
			return;
		}
		String[] componentNames = componentsValue.split(" ");
		for (String compName : componentNames) {
			String classProperty = compName + ".class";
			String className = props.getProperty(classProperty);
			if (className == null) {
				LOG.error("Failed to locate property " + classProperty);
				continue;
			}
			// Create it if you do...
			try {
				Object obj = java.beans.Beans.instantiate(null, className);
				if (obj instanceof PropertyConsumer) {
					PropertyConsumer propCons = (PropertyConsumer) obj;
					propCons.setProperties(compName, props);
				}
				mapHandler.add(obj);
			} catch (IOException e) {
				LOG.error("IO Exception instantiating class \"" + className + "\"");
			} catch (ClassNotFoundException e) {
				LOG.error("Component class not found: \"" + className + "\"");
			}
		}
	}
	
	public static Properties getProperties() {
		return properties;
	}
	
	public static String getVersion() {
		return VERSION;
	}
	
	public static String getMinorVersion() {
		return MINORVERSION;
	}
	
	public static Settings getSettings() {
		return settings;
	}
	
	public static NmeaSensor getAisSensor() {
		return aisSensor;
	}
	
	public static NmeaSensor getGpsSensor() {
		return gpsSensor;
	}
	
	public static GpsHandler getGpsHandler() {
		return gpsHandler;
	}
	
	public static MainFrame getMainFrame() {
		return mainFrame;
	}
	
	public static AisTargets getAisTargets() {
		return aisTargets;
	}
	
	public static RouteManager getRouteManager() {
		return routeManager;
	}
	
	public static MapHandler getMapHandler() {
		return mapHandler;
	}
	
	public static ShoreServices getShoreServices() {
		return shoreServices;
	}
	
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			LOG.error(e.getMessage());
		}
	}
	
	public static void wake() {
		Thread.currentThread().interrupt();
	}
	
	public static void startThread(Runnable t, String name) {
		Thread thread = new Thread(t);
		thread.setName(name);
		thread.start();
	}
	
	public static double elapsed(long start) {
		double elapsed = System.nanoTime() - start;
		return elapsed / 1000000.0;
	}

}