package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class MethodListVisitor extends PreorderJmmVisitor<Integer, Type> {

    List<Report> reports = new ArrayList<>();
    SymbolTable symbolTable;

    public MethodListVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;

    }

    @Override
    protected void buildVisitor() {
        addVisit("FuncOp", this::visitMethodCall);
        //addVisit("MethodDeclare", this::visitMethodDeclaration);
        setDefaultVisit(this::defaultVisitor);
    }


    private Type visitMethodCall(JmmNode methodCall, Integer dummy) {



        int line = 1;//Integer.valueOf(methodCall.get("line"));
        int col = 1;//Integer.valueOf(methodCall.get("col"));


        List<String> methods = symbolTable.getMethods();
        boolean declared = false;

        for (int i=0;i<methods.size();i++){
            if (methods.get(i).equals(methodCall.get("name"))){
                declared = true;

            }


        }

        if (!declared){
            String name = "";
            Type type = new Type("", false);
            for (int i=0;i<methods.size();i++){
                List<Symbol> tempSymbolListPar = symbolTable.getParameters(methods.get(i));
                List<Symbol> tempSymbolListVar = symbolTable.getLocalVariables(methods.get(i));
                for (int k=0;k<tempSymbolListPar.size();k++){

                    if (tempSymbolListPar.get(k).getName().equals(methodCall.getJmmChild(0).get("value"))){
                        name=  tempSymbolListPar.get(k).getType().getName();
                        type = tempSymbolListPar.get(k).getType();
                    }
                }
                for (int k=0;k<tempSymbolListVar.size();k++){

                    if (tempSymbolListVar.get(k).getName().equals(methodCall.getJmmChild(0).get("value"))){
                        name=  tempSymbolListVar.get(k).getType().getName();
                        type = tempSymbolListVar.get(k).getType();
                    }
                }
            }


            boolean imported = false;

            for (String import_: symbolTable.getImports()){
                if (import_.equals(name)){
                    imported = true;
                }
            }





            if (!imported && (!symbolTable.getClassName().equals(name) || symbolTable.getSuper().equals(""))){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error on method " + methodCall.get("name") + ": Method Undeclared"));
                return type;
            }

            else{
                return type;
            }


        }





        Type type = this.symbolTable.getReturnType(methodCall.get("name"));





        for(int i = 0; i < methodCall.getJmmChild(1).getChildren().size(); i++){
            Type argType = new Type("", false);

            switch(methodCall.getJmmChild(1).getJmmChild(i).getKind()) {
                case "Integer":
                case "Bool":
                case "NewArr":
                case "NewFunc":
                case "Identifier":
                case "This":
                case "NegationOp":{
                    VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                    argType = variableSemanticVisitor.visit(methodCall.getJmmChild(1).getJmmChild(i), 0);
                    break;
                }
                case "IndexOp":
                case "LengthOp":{
                    IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                    argType = indexingSemanticVisitor.visit(methodCall.getJmmChild(1).getJmmChild(i), 0);
                    break;
                }
                case "BinaryOP": {
                    BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                    argType = binOpSemanticVisitor.visit(methodCall.getJmmChild(1).getJmmChild(i),0);
                    break;
                }
                default:{
                    argType = visit(methodCall.getJmmChild(1).getJmmChild(i));
                    break;
                }
            }
            int argLine = 1;//Integer.valueOf(methodCall.getJmmChild(1).getJmmChild(i).get("line"));
            int argCol = 1;//Integer.valueOf(methodCall.getJmmChild(1).getJmmChild(i).get("col"));
            List<Symbol> params = symbolTable.getParameters(methodCall.get("name"));
            if(!params.get(i).getType().equals(argType)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, argLine, argCol, "Error on method " + methodCall.get("name") + ": invalid method call, types of parameters are invalid. Parameter " + params.get(i).getName() + " expected " + params.get(i).getType() + " but got " + argType));
            }
        }
        return type;
    }

    private Type defaultVisitor(JmmNode node, Integer temp){
        for (JmmNode child : node.getChildren()) {
            visit(child, 0);
        }
        return new Type("",false);
    }

    public List<Report> getReports() {
        return this.reports;
    }
}
