/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa.reports;

import net.praqma.jenkins.plugin.prqa.PrqaException;
import net.praqma.prqa.parsers.ComplianceReportHtmlParser;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.praqma.util.execute.CommandLineException;

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
        this.qar = qar;
        this.parser = new ComplianceReportHtmlParser();
    }
   
    /**
     * Completes the job of creating usable data given a path to a report generated by QAR. This is the Compliance report
     * @param reportpath
     * @return 
     */
    
    @Override
    public PRQAComplianceStatus completeTask() throws PrqaException {
        parser.setFullReportPath(this.getFullReportPath());
        cmdResult = null;
        try {
            cmdResult = qar.execute();
        } catch (AbnormalProcessTerminationException ex) {
            throw new PrqaException.PrqaCommandLineException(qar,ex);            
        } catch (CommandLineException cle) {      
            throw new PrqaException.PrqaCommandLineException(qar,cle);            
        }
       
        //Parse it.
        PRQAComplianceStatus stat = new PRQAComplianceStatus();        
        stat.setMessages(Integer.parseInt(parser.getResult(ComplianceReportHtmlParser.totalMessagesPattern)));
        stat.setProjectCompliance(Double.parseDouble(parser.getResult(ComplianceReportHtmlParser.projectCompliancePattern)));
        stat.setFileCompliance(Double.parseDouble(parser.getResult(ComplianceReportHtmlParser.fileCompliancePattern)));    
        return stat;
    }
}
