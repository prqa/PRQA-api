/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa;

import java.io.Serializable;

/**
 * 
 * @author Praqma
 */
public class PRQAToolUploadSettings implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final String vcsConfigXml;
	public final boolean singleSnapshotMode;
	public final CodeUploadSetting codeUploadSetting;
	public final String sourceOrigin;
	public final String qaVerifyProjectName;

	public PRQAToolUploadSettings(final String vcsConfigXml, final boolean singleSnapshotMode, final CodeUploadSetting codeUploadSetting,
			final String sourceOrigin, final String qaVerifyProjectName) {
		this.vcsConfigXml = vcsConfigXml;
		this.singleSnapshotMode = singleSnapshotMode;
		this.codeUploadSetting = codeUploadSetting;
		this.sourceOrigin = sourceOrigin;
		this.qaVerifyProjectName = qaVerifyProjectName;
	}
}
