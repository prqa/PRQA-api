/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa;

import java.io.Serializable;


public class PRQAApplicationSettings implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public final String qarHome;
	public final String qavClientHome;
	public final String qawHome;
	public final String productHome;

	public PRQAApplicationSettings(final String qarHome, final String qavClientHome, final String qawHome, final String productHome) {
		this.qarHome = qarHome;
		this.qavClientHome = qavClientHome;
		this.qawHome = qawHome;
		this.productHome = productHome;
	}

	public PRQAApplicationSettings(final String productHome) {
		this.qarHome = null;
		this.qavClientHome = null;
		this.qawHome = null;
		this.productHome = productHome;
	}

	public static String resolveQarExe(boolean isUnix) {
		if (isUnix) {
			return "qar.pl";
		} else {
			return "qar";
		}
	}

	public static String resolveQawExe(boolean isUnix) {
            return "qaw";
	}

	public static String resolveQacliExe(boolean isUnix) {
            return "qacli";
	}

	public static String addSlash(String value, String pathSeperator) {
		if (value.endsWith(pathSeperator)) {
			return value;
		} else {
			return value + pathSeperator;
		}
	}
}
