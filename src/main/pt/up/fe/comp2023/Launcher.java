package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.backend.JmmBackend;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.comp2023.visitors.ImportsVisitor;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);
        config.put("registerAllocation", "0");
        config.put("optimize", "true");
        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        System.out.println(parserResult.getRootNode().toTree());

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // ... add remaining stages

        //SymbolTableStore table = new SymbolTableStore(parserResult);
        MyJmmAnalysis analysis = new MyJmmAnalysis();
        JmmSemanticsResult analysisResult = analysis.semanticAnalysis(parserResult);


        TestUtils.noErrors(analysisResult.getReports());
        //analysisResult.getConfig().put("registerAllocation", "3");
        //analysisResult.getConfig().put("optimize", "true");
        OllirResult ollir = new MyJmmOptimization().toOllir(analysisResult);
        System.out.println(ollir.getOllirCode());
        OllirResult ollir2 = new MyJmmOptimization().optimize(ollir);
        System.out.println(ollir2.getOllirCode());

        //table.printImports();
        //table.printVars();

        JasminResult jasminResult = new JmmBackend().toJasmin(ollir2);
        //System.out.println(jasminResult.getJasminCode());
        //System.out.println(jasminResult.runWithFullOutput().getOutput());
        //var result = TestUtils.backend(ollir);
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
