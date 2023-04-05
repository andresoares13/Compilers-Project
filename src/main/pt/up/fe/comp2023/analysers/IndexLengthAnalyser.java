package pt.up.fe.comp2023.analysers;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SemanticAnalyser;
import pt.up.fe.comp2023.visitors.IndexLengthVisitor;

import java.util.List;

public class IndexLengthAnalyser implements SemanticAnalyser {
    private final SymbolTable symbolTable;
    private final JmmParserResult parserResult;

    public IndexLengthAnalyser(SymbolTable symbolTable, JmmParserResult parserResult) {
        this.symbolTable = symbolTable;
        this.parserResult = parserResult;
    }

    @Override
    public List<Report> getReports() {
        IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
        indexingSemanticVisitor.visit(this.parserResult.getRootNode(), 0);
        return indexingSemanticVisitor.getReports();
    }
}
