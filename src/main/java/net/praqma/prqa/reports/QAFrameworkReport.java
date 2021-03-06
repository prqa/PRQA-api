package net.praqma.prqa.reports;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.QAVerifyServerSettings;
import net.praqma.prqa.QaFrameworkVersion;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.exceptions.PrqaUploadException;
import net.praqma.prqa.parsers.ComplianceReportHtmlParser;
import net.praqma.prqa.parsers.MessageGroup;
import net.praqma.prqa.parsers.ResultsDataParser;
import net.praqma.prqa.parsers.Rule;
import net.praqma.prqa.products.PRQACommandBuilder;
import net.praqma.prqa.products.QACli;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.praqma.util.execute.CmdResult;
import net.praqma.util.execute.CommandLine;
import net.prqma.prqa.qaframework.QaFrameworkReportSettings;

import org.apache.commons.lang.StringUtils;

public class QAFrameworkReport implements Serializable {

    /**
     * @author Alexandru Ion
     * @since 2.0.3
     */
    private static final long serialVersionUID = 1L;
    public static final String XHTML = "xhtml";
    public static final String XML = "xml";
    public static final String HTML = "html";

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String QUOTE = "\"";

    public static String XHTML_REPORT_EXTENSION = "Report." + PRQAReport.XHTML;
    public static String XML_REPORT_EXTENSION = "Report." + PRQAReport.XML;
    public static String HTML_REPORT_EXTENSION = "Report." + PRQAReport.HTML;

    private static final Logger log = Logger.getLogger(QAFrameworkReport.class.getName());
    private QaFrameworkReportSettings settings;
    private QAVerifyServerSettings qaVerifySettings;
    private File workspace;
    private Map<String, String> environment;
    private PRQAApplicationSettings appSettings;
    private QaFrameworkVersion qaFrameworkVersion;

    public QAFrameworkReport(QaFrameworkReportSettings settings, QAVerifyServerSettings qaVerifySettings,
            PRQAApplicationSettings appSettings) {
        this.settings = settings;
        this.appSettings = appSettings;
        this.qaVerifySettings = qaVerifySettings;
    }

    public QAFrameworkReport(QaFrameworkReportSettings settings, QAVerifyServerSettings qaVerifySettings,
            PRQAApplicationSettings appSettings, HashMap<String, String> environment) {
        this.settings = settings;
        this.environment = environment;
        this.appSettings = appSettings;
        this.qaVerifySettings = qaVerifySettings;
    }

    public CmdResult pullUnifyProjectQacli(boolean isUnix, PrintStream out) throws PrqaException {
        if (!settings.isLoginToQAV()) {
            out.println("Configuration Error: Pull Unified Project is Selected but QAV Server Connection Configuration is not Selected");
            return null;
        } else {
            String command = createPullUnifyProjectCommand(isUnix, out);
            out.println("Download Unified Project Definition command:");
            out.println(command);
            try {
                return CommandLine.getInstance().run(command, workspace, true, false);
            } catch (AbnormalProcessTerminationException abnex) {
                throw new PrqaException(String.format("Failed to Download Unified Project, message was:\n %s", abnex.getMessage()), abnex);
            }
        }
    }

    private String createPullUnifyProjectCommand(boolean isUnix, PrintStream out) throws PrqaException {

        if (StringUtils.isBlank(settings.getUniProjectName())) {
            throw new PrqaException(
                    "Configuration Error: Download Unified Project Definition was selected but no Unified project was provided. The Download unified project was aborted.");
        } else if (StringUtils.isBlank(qaVerifySettings.host) || StringUtils.isBlank(qaVerifySettings.user) || StringUtils.isBlank(qaVerifySettings.password)) {
            throw new PrqaException("QAV Server Connection Settings are not selected");
        }
        PRQACommandBuilder builder = new PRQACommandBuilder(formatQacliPath());
        builder.appendArgument("admin");
        builder.appendArgument("--pull-unify-project");
        builder.appendArgument("--qaf-project");
        builder.appendArgument(PRQACommandBuilder.wrapFile(workspace, settings.getQaProject()));
        builder.appendArgument("--username");
        builder.appendArgument(qaVerifySettings.user);
        builder.appendArgument("--password");
        builder.appendArgument(qaVerifySettings.password.isEmpty() ? "\"\"" : qaVerifySettings.password);
        builder.appendArgument("--url");
        builder.appendArgument(qaVerifySettings.host + ":" + qaVerifySettings.port);
        builder.appendArgument("--project-name");
        builder.appendArgument(settings.getUniProjectName());
        return builder.getCommand();
    }

    public CmdResult analyzeQacli(boolean isUnix, String Options, PrintStream out) throws PrqaException {
        String finalCommand = createAnalysisCommandForQacli(isUnix, Options, out);
        out.println("Analysis command:");
        out.println(finalCommand);
        HashMap<String, String> systemVars = new HashMap<>();
        systemVars.putAll(System.getenv());
        try {
            if (getEnvironment() == null) {
                PRQAReport._logEnv("Current analysis execution environment", systemVars);
                return CommandLine.getInstance().run(finalCommand, workspace, true, false);
            } else {
                systemVars.putAll(getEnvironment());
                PRQAReport._logEnv("Current modified analysis execution environment", systemVars);
                return CommandLine.getInstance().run(finalCommand, workspace, true, false, systemVars);
            }
        } catch (AbnormalProcessTerminationException abnex) {
            throw new PrqaException(String.format("ERROR: Failed to analyze, message is... \n %s ", abnex.getMessage()), abnex);
        }
    }

    private String createAnalysisCommandForQacli(boolean isUnix, String options, PrintStream out) throws PrqaException {
        PRQACommandBuilder builder = new PRQACommandBuilder(formatQacliPath());
        builder.appendArgument("analyze");
        String analyzeOptions = options;
        if (settings.isQaEnableDependencyMode() && analyzeOptions.contains("c")) {
            analyzeOptions = analyzeOptions.replace("c", "");
        }
        if (settings.isQaEnableProjectCma()) {
            if (analyzeOptions.contains("f")) {
                analyzeOptions = analyzeOptions.replace("f", "p");
            }
        }

        builder.appendArgument(analyzeOptions);
        if (settings.isStopWhenFail() && settings.isAnalysisSettings()) {
            builder.appendArgument("--stop-on-fail");
        }

        if (settings.isGeneratePreprocess() && settings.isAnalysisSettings()) {
            builder.appendArgument("--generate-preprocessed-source");
        }

        if (settings.isGeneratePreprocess() && settings.isAnalysisSettings() && settings.isAssembleSupportAnalytics()) {
            builder.appendArgument("--assemble-support-analytics");
        } else if (settings.isAnalysisSettings() && settings.isAssembleSupportAnalytics() && (!settings.isGeneratePreprocess())) {
            log.log(Level.WARNING, "Assemble Support Analytics is selected but Generate Preprocessed Source option is not selected");
        }

        builder.appendArgument("-P");
        builder.appendArgument(PRQACommandBuilder.wrapFile(workspace, settings.getQaProject()));
        return builder.getCommand();
    }

    public CmdResult cmaAnalysisQacli(boolean isUnix, PrintStream out) throws PrqaException {
        if (settings.isQaCrossModuleAnalysis()) {
            String command = createCmaAnalysisCommand(isUnix, out);
            if (command != null) {
                out.println("Perform Cross-Module analysis command:");
                out.println(command);
                try {
                    return CommandLine.getInstance().run(command, workspace, true, false);
                } catch (AbnormalProcessTerminationException abnex) {
                    throw new PrqaException(String.format("ERROR: Failed to analyze, message is:  %s", abnex.getMessage()),
                            abnex);
                }
            } else {
                throw new PrqaException("ERROR: Detected PRQA Framework version 2.1.0. CMA analysis cannot be configured with the selected option. It has to be done by adding it to the toolchain of the project.");
            }
        }
        return null;
    }

    private String createCmaAnalysisCommand(boolean isUnix, PrintStream out) throws PrqaException {
        if (!qaFrameworkVersion.isQaFrameworkVersion210()) {
            PRQACommandBuilder builder = new PRQACommandBuilder(formatQacliPath());
            builder.appendArgument("analyze");
            builder.appendArgument("-p");
            builder.appendArgument("-P");
            builder.appendArgument(PRQACommandBuilder.wrapFile(workspace, settings.getQaProject()));
            return builder.getCommand();
        }
        return null;
    }

    public CmdResult reportQacli(boolean isUnix, String repType, PrintStream out) throws PrqaException {
        /*MDR Report type isnt supported in 1.0.3, 1.0.2, 1.0.1 and 1.0.0 */
        if (repType.equals("MDR") && (qaFrameworkVersion.isQaFrameworkVersionPriorToVersion104())) {
            out.println("===================================================================================================");
            out.println("Warning: Metrics Data Report isn't supported report type in PRQA-Framework Prior to 1.0.4 version");
            out.println("===================================================================================================");
            log.severe(String.format("Warning: Metrics Data Report isn't supported report type in PRQA-Framework Prior to 1.0.4 version"));
            return null;
        }
        String reportCommand = createReportCommandForQacli(isUnix, repType, out);
        Map<String, String> systemVars = new HashMap<String, String>();
        systemVars.putAll(System.getenv());
        systemVars.putAll(getEnvironment());
        out.println("Report command :" + reportCommand);
        try {
            PRQAReport._logEnv("Current report generation execution environment", systemVars);
            return CommandLine.getInstance().run(reportCommand, workspace, true, false, systemVars);
        } catch (AbnormalProcessTerminationException abnex) {
            log.severe(String.format("Failed to execute report generation command: %s%n%s", reportCommand,
                    abnex.getMessage()));
            log.logp(Level.SEVERE, this.getClass().getName(), "report()",
                    "Failed to execute report generation command", abnex);
            out.println(String.format("Failed to execute report generation command: %s%n%s", reportCommand,
                    abnex.getMessage()));
        }
        return new CmdResult();
    }

    private String createReportCommandForQacli(boolean isUnix, String reportType, PrintStream out) throws PrqaException {
        out.println("Create " + reportType + " report command");
        String projectLocation = PRQACommandBuilder.resolveAbsOrRelativePath(workspace, settings.getQaProject());
        removeOldReports(projectLocation, reportType);
        return createReportCommand(projectLocation, reportType, out);
    }

    private String createReportCommand(String projectLocation, String reportType, PrintStream out) {
        PRQACommandBuilder builder = new PRQACommandBuilder(formatQacliPath());
        builder.appendArgument("report -P");
        builder.appendArgument(PRQACommandBuilder.wrapInQuotationMarks(projectLocation));
        if (qaFrameworkVersion.isQaFrameworkVersionPriorToVersion104()) {
            builder.appendArgument("-l C");
        }
        builder.appendArgument("-t");
        builder.appendArgument(reportType);

        return builder.getCommand();
    }

    private void removeOldReports(String projectLocation, String reportType) {
        String reportsPath = "/prqa/reports";
        File file = new File(projectLocation + reportsPath);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if ((f.getName().contains("RCR") && reportType.equals("RCR"))
                        || (f.getName().contains("CRR") && reportType.equals("CRR"))
                        || (f.getName().contains("MDR") && reportType.equals("MDR"))
                        || (f.getName().contains("SUR") && reportType.equals("SR"))
                        || f.getName().contains("results_data")) {
                    f.delete();
                }

            }
        }
    }

    private String createUploadCommandQacli(PrintStream out) throws PrqaException {
        String projectLocation;
        if (!StringUtils.isBlank(settings.getQaVerifyProjectName())) {
            projectLocation = PRQACommandBuilder.wrapFile(workspace, settings.getQaProject());
        } else {
            throw new PrqaException("Neither file list nor project file has been set, this should not be happening");
        }
        PRQACommandBuilder builder = new PRQACommandBuilder(formatQacliPath());
        builder.appendArgument("upload -P");
        builder.appendArgument(projectLocation);
        builder.appendArgument("--qav-upload");
        builder.appendArgument("--username");
        builder.appendArgument(qaVerifySettings.user);
        builder.appendArgument("--password");
        builder.appendArgument(qaVerifySettings.password.isEmpty() ? "\"\"" : qaVerifySettings.password);
        builder.appendArgument("--url");
        builder.appendArgument(qaVerifySettings.host + ":" + qaVerifySettings.port);
        builder.appendArgument("--upload-project");
        builder.appendArgument(settings.getQaVerifyProjectName());
        builder.appendArgument("--snapshot-name");
        builder.appendArgument(settings.getUploadSnapshotName() + '_' + settings.getbuildNumber());
        builder.appendArgument("--upload-source");
        builder.appendArgument(settings.getUploadSourceCode());
        return builder.getCommand();
    }

    public CmdResult uploadQacli(PrintStream out) throws PrqaUploadException, PrqaException {
        if (!settings.isLoginToQAV()) {
            out.println("Configuration Error: Upload Results to QAV is Selected but QAV Server Connection Configuration is not Selected");
            return null;
        }
        String finalCommand = createUploadCommandQacli(out);
        out.println("Upload command: " + finalCommand);
        try {
            Map<String, String> getEnv = getEnvironment();
            if (getEnv == null) {
                return CommandLine.getInstance().run(finalCommand, workspace, true, false);
            } else {
                return CommandLine.getInstance().run(finalCommand, workspace, true, false, getEnv);
            }
        } catch (AbnormalProcessTerminationException abnex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "upload()", "Logged error with upload", abnex);
            throw new PrqaUploadException(String.format("Upload failed with message: %s", abnex.getMessage()), abnex);
        }
    }

    // __________________________________________________________________
    private String formatQacliPath() {
        if (environment.containsKey(QACli.QAF_BIN_PATH)) {
            return (QUOTE + environment.get(QACli.QAF_BIN_PATH) + FILE_SEPARATOR + QACli.QACLI + QUOTE);
        } else {
            return QACli.QACLI;
        }
    }

    public static String getNamingTemplate(PRQAContext.QARReportType type, String extension) {
        return type.toString() + " " + extension;
    }

    public static void _logEnv(String location, Map<String, String> env) {
        log.fine(String.format("%s", location));
        log.fine("==========================================");
        if (env != null) {
            for (String key : env.keySet()) {
                log.fine(String.format("%s=%s", key, env.get(key)));
            }
        }
        log.fine("==========================================");
    }

    public PRQAComplianceStatus getComplianceStatus(PrintStream out) throws PrqaException, Exception {
        PRQAComplianceStatus status = new PRQAComplianceStatus();
        status.setQaFrameworkVersion(qaFrameworkVersion);
        String projectLocation;
        String report_structure;
        report_structure = new File("prqa", "reports").getPath();

        projectLocation = PRQACommandBuilder.resolveAbsOrRelativePath(workspace, settings.getQaProject());
        File reportFolder = new File(projectLocation, report_structure);
        out.println("Report Folder Path:: " + reportFolder);

        File resultsDataFile = new File(projectLocation, getResultsDataFileRelativePath());
        out.println("RESULTS DATA file path: " + resultsDataFile.getPath());

        if (!reportFolder.exists()
                || !reportFolder.isDirectory()
                || !resultsDataFile.exists()
                || !resultsDataFile.isFile()) {
            return status;
        }

        File[] listOfReports = reportFolder.listFiles();
        if (listOfReports.length < 1) {
            return status;
        }

        Double fileCompliance = 0.0;
        Double projectCompliance = 0.0;
        int messages = 0;
        for (File reportFile : listOfReports) {
            if (reportFile.getName().contains("RCR")) {
                ComplianceReportHtmlParser parser = new ComplianceReportHtmlParser(reportFile.getAbsolutePath());
                fileCompliance += Double.parseDouble(parser.getParseFirstResult(ComplianceReportHtmlParser.QAFfileCompliancePattern));
                projectCompliance += Double.parseDouble(parser.getParseFirstResult(ComplianceReportHtmlParser.QAFprojectCompliancePattern));
                messages += Integer.parseInt(parser.getParseFirstResult(ComplianceReportHtmlParser.QAFtotalMessagesPattern));
            }
        }

        /*This section is to read result data file and parse the results*/
        ResultsDataParser resultsDataParser = new ResultsDataParser(resultsDataFile.getAbsolutePath());
        resultsDataParser.setQaFrameworkVersion(qaFrameworkVersion);
        List<MessageGroup> messagesGroups = resultsDataParser.parseResultsData();
        sortViolatedRulesByRuleID(messagesGroups);
        status.setMessagesGroups(messagesGroups);
        status.setFileCompliance(fileCompliance);
        status.setProjectCompliance(projectCompliance);
        status.setMessages(messages);

        return status;
    }

    private String getResultsDataFileRelativePath() {
        return (qaFrameworkVersion.isQaFrameworkVersionPriorToVersion104() ? "/prqa/output/results_data.xml" : "/prqa/reports/results_data.xml");

    }

    private void sortViolatedRulesByRuleID(List<MessageGroup> messagesGroups) {
        for (MessageGroup messageGroup : messagesGroups) {
            Collections.sort(messageGroup.getViolatedRules(), new Comparator<Rule>() {
                @Override
                public int compare(Rule o1, Rule o2) {
                    return o1.getRuleID().toString().compareTo(o2.getRuleID().toString());
                }
            });
        }
    }

    public void setWorkspace(File workspace) {
        this.workspace = workspace;
    }

    public QaFrameworkReportSettings getSettings() {
        return settings;
    }

    public void setSettings(QaFrameworkReportSettings settings) {
        this.settings = settings;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public PRQAApplicationSettings getAppSettings() {
        return appSettings;
    }

    public void setQaFrameworkVersion(QaFrameworkVersion qaFrameworkVersion) {
        this.qaFrameworkVersion = qaFrameworkVersion;
    }
}
