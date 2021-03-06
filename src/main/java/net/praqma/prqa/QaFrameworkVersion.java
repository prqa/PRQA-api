package net.praqma.prqa;

import java.io.PrintStream;
import java.io.Serializable;

public class QaFrameworkVersion implements Serializable {

	private String qaFrameworkVersion;

	public QaFrameworkVersion(String qaFrameworkVersion) {
		this.qaFrameworkVersion = qaFrameworkVersion;
	}

	public String getQaFrameworkVersion() {
		return qaFrameworkVersion;
	}

	public boolean isQAFVersionSupported() {
		if (qaFrameworkVersion == null || qaFrameworkVersion.length() == 0) {
			return false;
		}
		return !isAnOlderVersionThanSupported();
	}

	private boolean isAnOlderVersionThanSupported() {
            return getVersionShortFormat().equals("1.0.0");
	}

	public boolean isQaFrameworkVersionPriorToVersion104() {

		String shortVersion = getVersionShortFormat();
                String qafVersion = shortVersion.substring(shortVersion.lastIndexOf(" ") + 1);
		return ( qafVersion.equals("1.0.0")
                        || qafVersion.equals("1.0.1")
                        || qafVersion.equals("1.0.2")
                        || qafVersion.equals("1.0.3"));
	}
        
	public boolean isQaFrameworkUnified() {

		String shortVersion = getVersionShortFormat();
                String qafVersion = shortVersion.substring(shortVersion.lastIndexOf(" ") + 1);
		return ( !qafVersion.equals("1.0.0") 
                        && !qafVersion.equals("1.0.1")
                        && !qafVersion.equals("1.0.2")
                        && !qafVersion.equals("1.0.3")
                        && !qafVersion.equals("1.0.4")
                        && !qafVersion.equals("1.0.5"));
	}

	public boolean isQaFrameworkVersion210() {
		String shortVersion = getVersionShortFormat();
		String qafVersion = shortVersion.substring(shortVersion.lastIndexOf(" ") + 1);
		return qafVersion.equals("2.1.0");
	}

	public String getVersionShortFormat() {
		return qaFrameworkVersion.substring(0, qaFrameworkVersion.lastIndexOf("."));
	}
}
