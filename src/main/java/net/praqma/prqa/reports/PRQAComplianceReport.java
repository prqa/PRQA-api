/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa.reports;

import java.util.logging.Level;
import net.praqma.jenkins.plugin.prqa.PrqaException;
import net.praqma.prqa.parsers.ComplianceReportHtmlParser;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.status.PRQAComplianceStatus;

/**
 *
 * @author Praqma
 */
public class PRQAComplianceReport extends PRQAReport<PRQAComplianceStatus> {    
    /**
     * A compliance report. Takes a command line wrapper for Programming Research QAR tool. Each report must implement their own parser.
     * 
     * @param qar 
     */
    public PRQAComplianceReport(QAR qar) {
    	super(qar);
        logger.log(Level.FINEST, "Constructor and super constructor called for class PRQAComplianceReport");
        this.parser = new ComplianceReportHtmlParser();
    }
   
    /**
     * Completes the job of creating usable data given a path to a report generated by QAR. This is the Compliance report
     * @param reportpath
     * @return 
     */
    
    @Override
    public PRQAComplianceStatus completeTask() throws PrqaException {
    	logger.log(Level.FINEST, "Starting execution of method - completeTask");
    	
        executeQAR();
        
        //Parse it.
        PRQAComplianceStatus status = new PRQAComplianceStatus();
        status.setMessages(Integer.parseInt(parser.getResult(ComplianceReportHtmlParser.totalMessagesPattern)));
        status.setProjectCompliance(Double.parseDouble(parser.getResult(ComplianceReportHtmlParser.projectCompliancePattern)));
        status.setFileCompliance(Double.parseDouble(parser.getResult(ComplianceReportHtmlParser.fileCompliancePattern)));
        
        logger.log(Level.FINEST, "Returning status {0}", status);
        
        return status;
    }
}
