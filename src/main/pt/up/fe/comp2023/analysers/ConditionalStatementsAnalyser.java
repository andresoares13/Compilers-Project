package pt.up.fe.comp2023.analysers;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SemanticAnalyser;
import pt.up.fe.comp2023.visitors.ConditionalStatementVisitor;

import java.util.List;

public class ConditionalStatementsAnalyser implements SemanticAnalyser {

    private final SymbolTable symbolTable;
    private final JmmParserResult parserResult;

    public ConditionalStatementsAnalyser(SymbolTable symbolTable, JmmParserResult parserResult) {
        this.symbolTable = symbolTable;
        this.parserResult = parserResult;
    }

    @Override
    public List<Report> getReports() {
        ConditionalStatementVisitor conditionalSemanticVisitor = new ConditionalStatementVisitor(symbolTable);
        conditionalSemanticVisitor.visit(this.parserResult.getRootNode(), 0);
        return conditionalSemanticVisitor.getReports();
    }
}
