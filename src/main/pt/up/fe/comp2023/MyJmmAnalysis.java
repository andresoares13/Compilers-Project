package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.analysers.AttributionAnalyser;
import pt.up.fe.comp2023.analysers.IdentifierDeclarationAnalyser;
import pt.up.fe.comp2023.analysers.IndexLengthAnalyser;
import pt.up.fe.comp2023.analysers.OperandsAnalyser;
import pt.up.fe.comp2023.visitors.IndexLengthVisitor;

import java.util.*;

public class MyJmmAnalysis implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        SymbolTableStore table = new SymbolTableStore(jmmParserResult);
        //table.print();
        List<Report> reports = new ArrayList<>();
        List<SemanticAnalyser> analysers = new ArrayList<>();

        analysers.add(new IdentifierDeclarationAnalyser(table,jmmParserResult));
        analysers.add(new IndexLengthAnalyser(table, jmmParserResult));
        analysers.add(new AttributionAnalyser(table,jmmParserResult));
        analysers.add(new OperandsAnalyser(table, jmmParserResult));



        Map<String, String> config = new HashMap<>();

        for(SemanticAnalyser analyser : analysers) {
            reports.addAll(analyser.getReports());
        }
        reports.removeAll(Collections.singleton(null));

        JmmSemanticsResult result = new JmmSemanticsResult(jmmParserResult.getRootNode(),table,reports,config);
        return result;
    }
}
