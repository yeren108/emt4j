/********************************************************************************
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.emt4j.analysis;

import org.eclipse.emt4j.analysis.common.util.Progress;
import org.eclipse.emt4j.analysis.out.BinaryFileOutputConsumer;
import org.eclipse.emt4j.analysis.report.ReportMain;
import org.eclipse.emt4j.analysis.common.util.Option;
import org.eclipse.emt4j.analysis.common.util.OptionProcessor;
import org.eclipse.emt4j.analysis.source.*;
import org.eclipse.emt4j.common.CheckConfig;
import org.eclipse.emt4j.common.Feature;
import org.eclipse.emt4j.common.ReportConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emt4j.common.SourceInformation;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * The main entry of class analysis and report
 */
public class AnalysisMain {
    private static final String DEFAULT_FILE = "analysis_output";
    private static Set<String> analysisTargetClassPaths = new HashSet<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, URISyntaxException {
        Progress root = new Progress(0, 0, "ROOT");
        doReport(doAnalysis(args, new Progress(root, "Analysis")),
                new Progress(root, "Report"));
    }

    private static void doReport(ReportConfig reportConfig, Progress progress) throws InterruptedException, IOException, ClassNotFoundException, URISyntaxException {
        ReportMain.run(reportConfig, progress);
    }

    static ReportConfig doAnalysis(String[] args, Progress progress) throws IOException {
        if (null == args || args.length == 0) {
            printUsage(null);
        }

        progress.printTitle();
        CheckConfig checkConfig = new CheckConfig();
        ReportConfig reportConfig = new ReportConfig();
        final List<Feature> featureList = new ArrayList<>();
        AnalysisExecutor analysisExecutor = new AnalysisExecutor(checkConfig);
        OptionProcessor optionProcessor = new OptionProcessor(args);
        optionProcessor.addOption(Option.buildParamWithValueOption("-o", null, (v) -> reportConfig.setOutputFile(v)));
        optionProcessor.addOption(Option.buildParamWithValueOption("-f", (v) -> StringUtils.isNumeric(v), (v) -> checkConfig.setFromVersion(Integer.parseInt(v))));
        optionProcessor.addOption(Option.buildParamWithValueOption("-t", (v) -> StringUtils.isNumeric(v), (v) -> checkConfig.setToVersion(Integer.parseInt(v))));
        optionProcessor.addOption(Option.buildParamWithValueOption("-priority", null, p -> checkConfig.setPriority(p)));
        optionProcessor.addOption(Option.buildParamWithValueOption("-p",
                (v) -> "txt".equalsIgnoreCase(v) || "json".equalsIgnoreCase(v) || "html".equalsIgnoreCase(v), (v) -> reportConfig.setOutputFormat(v.toLowerCase())));
        optionProcessor.addOption(Option.buildParamWithValueOption("-j", (v) -> new File(v).exists()
                && new File(v).isDirectory(), (v) -> reportConfig.setTargetJdkHome(v)));
        optionProcessor.addOption(Option.buildParamNoValueOption("-v", null, (v) -> {
            checkConfig.setVerbose(true);
            reportConfig.setVerbose(true);
        }));
        optionProcessor.addOption(Option.buildParamWithValueOption("-e", (v) -> new File(v).exists()
                && new File(v).isDirectory(), (v) -> reportConfig.setExternalToolRoot(v)));
        optionProcessor.addOption(Option.buildDefaultOption(
                AnalysisMain::isSource,
                (v) -> {
                    processSource(reportConfig, analysisExecutor, v);//v=Users/StephenSTF/Documents/github/yeren-cms/.emt4j
                }));
        optionProcessor.setShowUsage((s) -> printUsage(s));
        if (checkConfig.getFromVersion() >= checkConfig.getToVersion()) {
            printUsage("from version should less than to version");
        }

        optionProcessor.process();
        if (featureList.isEmpty()) {
            featureList.add(Feature.DEFAULT);
        }

        if (analysisExecutor.hasSource()) {
            File tempFile = File.createTempFile(DEFAULT_FILE, ".dat");
            tempFile.deleteOnExit();
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tempFile))) {
                analysisExecutor.setAnalysisOutputConsumer(new BinaryFileOutputConsumer(out));
                analysisExecutor.execute(featureList, progress);
            }
            reportConfig.getInputFiles().add(tempFile);
            System.out.println("Write internal file to " + tempFile + " done.");
        }
        if (reportConfig.getInputFiles().isEmpty()) {
            printUsage(null);
        }

        return reportConfig;
    }

    private static SourceInformation buildSourceInformation(String str, boolean isDependency) {
        SourceInformation info = new SourceInformation();
        String[] arr = str.split(":");
        info.setIdentifier(arr[1]);
        info.setExtras(new String[]{str});
        info.setDependency(isDependency);
        return info;
    }

    private static void processSource(ReportConfig reportConfig, AnalysisExecutor analysisExecutor, String v) {
        File file = new File(v);
        if (file.isDirectory() && file.getName().equals(".emt4j")) {
            try {
                BufferedReader br = Files.newBufferedReader(new File(file, "modules").toPath());
                String str;//eg: com.yeren:yeren-cms:1.0=/Users/StephenSTF/Documents/github/yeren-cms/target/classes:/Users/StephenSTF/Documents/github/yeren-cms/target/test-classes
                while ((str = br.readLine()) != null) {
                    //pair[0]  com.yeren:yeren-cms:1.0
                    //pair[1]  /Users/StephenSTF/Documents/github/yeren-cms/target/classes:/Users/StephenSTF/Documents/github/yeren-cms/target/test-classes
                    String[] pair = str.split("=");
                    String[] paths = pair[1].split(File.pathSeparator);
                    //paths[0] /Users/StephenSTF/Documents/github/yeren-cms/target/classes
                    //paths[1] /Users/StephenSTF/Documents/github/yeren-cms/target/test-classes
                    SourceInformation info = buildSourceInformation(pair[0], false);
                    //info {name='yeren-cms', isDependency=false, extras=[com.yeren:yeren-cms:1.0]}
                    for (String path : paths) {
                        //1
                        DependencySource dependencySource = doProcessSource(reportConfig, analysisExecutor, path);
                        if (dependencySource != null) {
                            dependencySource.setInformation(info);
                        }
                    }
                }
                br.close();

                br = Files.newBufferedReader(new File(file, "dependencies").toPath());
                //eg: str = junit:junit:4.12=/Users/StephenSTF/.m2/repository/junit/junit/4.12/junit-4.12.jar
                while ((str = br.readLine()) != null) {
                    String[] pair = str.split("=");
                    if (pair[1].endsWith(".pom")) {
                        continue;
                    }
                    //2分析依赖
                    DependencySource dependencySource = doProcessSource(reportConfig, analysisExecutor, pair[1]);
                    if (dependencySource != null) {
                        dependencySource.setInformation(buildSourceInformation(pair[0], true));
                    }
                }
                br.close();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            //3
            doProcessSource(reportConfig, analysisExecutor, v);
        }
    }

    private static DependencySource doProcessSource(ReportConfig reportConfig, AnalysisExecutor analysisExecutor, String v) {
        //v是一个path路径
        //v= /Users/StephenSTF/Documents/github/yeren-cms/target/classes
        //v= /Users/StephenSTF/Documents/github/yeren-cms/target/test-classes
        Optional<DependencySource> opt= getSource(v);
        if (!opt.isPresent()) {
            System.err.println(v + " doesn't exist");
            return null;
        }
        DependencySource ds = opt.get();
        if (ds.needAnalysis()) {
            analysisExecutor.add(ds);
            analysisTargetClassPaths.add(ds.getFile().getAbsolutePath());
        } else {
            reportConfig.getInputFiles().add(ds.getFile());
        }
        return ds;
    }

    private static boolean isSource(String file) {
        File f = new File(file);
        if (!f.exists()) {
            System.err.println(file + " not exist!");
            return false;
        }
        if (f.isFile()) {
            return file.endsWith(".class") || file.endsWith(".cfg") || file.endsWith(".jar") || file.endsWith(".dat");
        }
        return f.isDirectory();
    }

    private static Optional<DependencySource> getSource(String file) {
        File f = new File(file);
        if (!f.exists()) {
            System.err.println(file + " not exist!");
            return Optional.empty();
        }

        if (f.isFile()) {
            if (file.endsWith(".class")) {
                return Optional.of(new SingleClassSource(f));
            } else if (file.endsWith(".cfg")) {
                return Optional.of(new JavaOptionSource(f));
            } else if (file.endsWith(".jar")) {
                return Optional.of(new SingleJarSource(f));
            } else if (file.endsWith(".dat")) {
                return Optional.of(new AgentOutputAsSource(f));
            }
        } else if (f.isDirectory()) {
            return Optional.of(new DirectorySource(f));
        }

        return Optional.empty();
    }

    private static void printUsage(String option) {
        if (option != null) {
            System.err.println(option + " is invalid!");
        }

        String osName = System.getProperty("os.name");
        boolean windows = osName != null && osName.toLowerCase().indexOf("windows") != -1;
        String launcher = windows ? "analysis.bat" : "analysis.sh";
        System.err.println("Usage:" + launcher + " [-f version] [-t version] [-p txt] [-o outputfile] [-j target jdk home] [-e external tool home] [-v] <files>");
        System.err.println("-f From which JDK version,default is 8");
        System.err.println("-t To which JDK version,default is 11");
        System.err.println("-p The report format.Can be TXT or JSON or HTML.Default is HTML");
        System.err.println("-o Write analysis to output file. Default is " + DEFAULT_FILE);
        System.err.println("-j Target JDK home. Provide target jdk home can help to find more compatible problems.");
        System.err.println("-e The root directory of external tools.");
        System.err.println("-v Show verbose information.");
        System.err.println("files can be combination of following types :");
        final String[] allSupportFiles = new String[]{
                "Agent output file(*.dat)",
                "Single class file(*.class)",
                "Directory contains jars,classes",
                "File end with .cfg that contain java option(*.cfg)"
        };
        for (int i = 1; i <= allSupportFiles.length; i++) {
            System.err.println(i + ". " + allSupportFiles[i - 1]);
        }

        System.err.println("files can be class, jar,directory or java source file.");
        System.exit(1);
    }

    public static Set<String> getAnalysisTargetClassPaths() {
        return analysisTargetClassPaths;
    }
}
