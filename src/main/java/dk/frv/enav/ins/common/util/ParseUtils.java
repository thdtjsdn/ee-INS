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
package dk.frv.enav.ins.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import dk.frv.enav.ins.common.FormatException;
import dk.frv.enav.ins.common.Heading;

/**
 * Utility class different parsing tasks
 * @author obo
 *
 */
public class ParseUtils {
	
	public static final int METERS_PER_NM = 1852;
	
	public static Double nmToMeters(Double nm) {
		if (nm == null) return null;
		return nm * METERS_PER_NM;
	}
	
	public static Double metersToNm(Double meters) {
		if (meters == null) return null;
		return meters / METERS_PER_NM;
	}
	
	public static Double parseDouble(String str) throws FormatException {
		if (str == null || str.length() == 0) return null;
		try {
			return Double.parseDouble(str);
		} catch (NumberFormatException e) {
			throw new FormatException("Could not parse " + str + " as a decimal number");
		}
	}
	
	public static Integer parseInt(String str) throws FormatException {
		if (str == null || str.length() == 0) return null;
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			throw new FormatException("Could not parse " + str + " as an integer");
		}
	}
	
	public static String parseString(String str) {
		str = str.trim();
		if (str == null || str.length() == 0) return null;
		return str;
	}
	
	public static double parseLatitude(String formattedString) throws FormatException {
		String[] parts = splitFormattedPos(formattedString);
		return parseLatitude(parts[0], parts[1], parts[2]);
	}
	
	public static double parseLongitude(String formattedString) throws FormatException {
		String[] parts = splitFormattedPos(formattedString);
		return parseLongitude(parts[0], parts[1], parts[2]);
	}
	
	private static String[] splitFormattedPos(String posStr) throws FormatException {
		if (posStr.length() < 4) {
			throw new FormatException();
		}
		String[] parts = new String[3];
		parts[2] = posStr.substring(posStr.length() - 1);
		posStr = posStr.substring(0, posStr.length() - 1);
		String[] posParts = posStr.split(" ");
		if (posParts.length != 2) {
			throw new FormatException();
		}
		parts[0] = posParts[0];
		parts[1] = posParts[1];
		
		return parts;
	}
	
	public static double parseLatitude(String hours, String minutes, String northSouth) throws FormatException {
		Integer h = parseInt(hours);
		Double m = parseDouble(minutes);
		String ns = parseString(northSouth);		
		if (h == null || m == null || ns == null) {
			throw new FormatException();
		}
		if (!ns.equals("N") && !ns.equals("S")) {
			throw new FormatException();
		}
		double lat = h + m / 60.0; 
		if (ns.equals("S")) {
			lat *= -1;
		}
		return lat;		
	}
	
	public static double parseLongitude(String hours, String minutes, String eastWest) throws FormatException {
		Integer h = parseInt(hours);
		Double m = parseDouble(minutes);
		String ew = parseString(eastWest);		
		if (h == null || m == null || ew == null) {
			throw new FormatException();
		}
		if (!ew.equals("E") && !ew.equals("W")) {
			throw new FormatException();
		}
		double lon = h + m / 60.0; 
		if (ew.equals("W")) {
			lon *= -1;
		}
		return lon;
	}
	
	public static Heading parseSailHeadingType(String heading) throws FormatException {
		heading = parseString(heading);
		if (heading == null) {
			throw new FormatException("Missing sail field");
		}
		if (heading.equals("RL")) {
			return Heading.RL;
		}
		if (heading.equals("GC")) {
			return Heading.GC;
		}
		throw new FormatException("Unknown sail heading " + heading);
	}
	
	public static String getShortSailHeadingType(Heading st) {
		if (st == Heading.RL) {
			return  "RL";
		}
		return "GC";
	}

	public static TimeZone parseTimeZone(String tz) throws FormatException {
		tz = parseString(tz);
		if (tz == null) return null;
		String[] parts = tz.split(":");
		if (parts.length != 2) {
			throw new FormatException("Error in timezone");
		}
		Integer hours = parseInt(parts[0]);
		Integer mins = parseInt(parts[1]);
		if (hours == null || mins == null) {
			throw new FormatException("Error in timezone");
		}
		String sign = (hours < 0) ? "-" : "+";
		hours = Math.abs(hours);		
		String customTzId = String.format("GMT%s%02d%02d", sign, hours, mins);
		return TimeZone.getTimeZone(customTzId);
	}
	
	public static Date parseIso8602(String str) throws FormatException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
		try {
			return dateFormat.parse(str);
		} catch (ParseException e) {
			throw new FormatException(e.getMessage());
		}
	}
	
	public static Date parseVariuosDateTime(String dateStr) {
		// Preparation
		int i = dateStr.indexOf('(');
		if (i >=0) {
			dateStr = dateStr.substring(0, i);
		}
		
		SimpleDateFormat dateFormat;
		List<String> patterns = new ArrayList<String>();
		patterns.add("MM/dd HH:mm:ss");
		patterns.add("MM/dd HH:mm");
		patterns.add("MM/dd/yyyy HH:mm:ss");
		patterns.add("MM/dd/yyyy HH:mm");
		patterns.add("dd-MM-yyyy HH:mm");
		patterns.add("dd-MM-yyyy HH:mm:ss");		
		Date res = null;
		for (String pattern : patterns) {
			dateFormat = new SimpleDateFormat(pattern);
			try {
				res = dateFormat.parse(dateStr);				
			} catch (ParseException e) {
				
			}
		}
		return res;
	}
	
	
}	