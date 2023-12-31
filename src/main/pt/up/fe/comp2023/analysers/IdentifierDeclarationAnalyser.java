package pt.up.fe.comp2023.analysers;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SemanticAnalyser;
import pt.up.fe.comp2023.SymbolTableStore;
import pt.up.fe.comp2023.visitors.IdentifierListVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IdentifierDeclarationAnalyser implements SemanticAnalyser {

    private final SymbolTableStore symbolTable;
    private List<String> idList = new ArrayList<>();

    public IdentifierDeclarationAnalyser(SymbolTableStore symbolTable, JmmParserResult parserResult) {
        this.symbolTable = symbolTable;
        IdentifierListVisitor idListVisitor = new IdentifierListVisitor();
        idListVisitor.visit(parserResult.getRootNode(),idList);
    }

    @Override
    public List<Report> getReports() {
        List<String> methods = symbolTable.getMethods();
        List<Report> reports= new ArrayList<>();

        for (int i=0;i<idList.size();i++){

            boolean found = false;
            boolean imported = false;
            boolean superR = false;
            boolean field = false;
            for (int j=0;j<methods.size();j++){

                List<Symbol> tempSymbolListPar = symbolTable.getParameters(methods.get(j));
                List<Symbol> tempSymbolListVar = symbolTable.getLocalVariables(methods.get(j));
                for (int k=0;k<tempSymbolListPar.size();k++){

                    if (tempSymbolListPar.get(k).getName().equals(idList.get(i))){

                        found = true;
                    }
                }
                for (int k=0;k<tempSymbolListVar.size();k++){

                    if (tempSymbolListVar.get(k).getName().equals(idList.get(i))){

                        found = true;
                    }
                }
            }
            for (int j=0;j<symbolTable.getImports().size();j++){
                if (symbolTable.getImports().get(j).equals(idList.get(i))){
                    imported = true;
                }
            }

            if (idList.get(i).equals(symbolTable.getSuper())){
                superR = true;
            }

            for (int j=0;j<symbolTable.getFields().size();j++){

                if (symbolTable.getFields().get(j).getName().equals(idList.get(i))){
                    field = true;
                }
            }


            if (!found && !imported && !superR && !field){

                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "The variable " + idList.get(i)+ " was not declared"));
            }
        }





        return reports;
    }
}
