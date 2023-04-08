package pt.up.fe.comp2023.analysers;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SemanticAnalyser;
import pt.up.fe.comp2023.visitors.VariableSemanticVisitor;

import java.util.List;

public class VariableAnalyser implements SemanticAnalyser {
    private final SymbolTable symbolTable;
    private final JmmParserResult parserResult;

    public VariableAnalyser(SymbolTable symbolTable, JmmParserResult parserResult) {
        this.symbolTable = symbolTable;
        this.parserResult = parserResult;
    }

    @Override
    public List<Report> getReports() {
        VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
        variableSemanticVisitor.visit(this.parserResult.getRootNode(), 0);
        return variableSemanticVisitor.getReports();
    }
}
