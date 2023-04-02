package pt.up.fe.comp2023.analysers;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SemanticAnalyser;
import pt.up.fe.comp2023.visitors.BinaryExpressionVisitor;

import java.util.List;

public class OperandsAnalyser implements SemanticAnalyser {
    private final SymbolTable symbolTable;
    private final JmmParserResult parserResult;

    public OperandsAnalyser(SymbolTable symbolTable, JmmParserResult parserResult) {
        this.symbolTable = symbolTable;
        this.parserResult = parserResult;
    }

    @Override
    public List<Report> getReports() {
        BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
        binOpSemanticVisitor.visit(this.parserResult.getRootNode(), 0);
        return binOpSemanticVisitor.getReports();
    }
}
