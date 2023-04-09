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
        addVisit("MethodDeclare", this::visitMethodDeclaration);
        setDefaultVisit(this::defaultVisitor);
    }


    private Type visitMethodCall(JmmNode methodCall, Integer dummy) {

        if (methodCall.getJmmChild(0).getKind().equals("This")){
            boolean exists = false;
            for (String method: symbolTable.getMethods()){
                if (method.equals(methodCall.get("name"))){
                    exists = true;
                }
            }
            if (exists){
                return symbolTable.getReturnType(methodCall.get("name"));
            }
            else{
                return new Type("",false);
            }
        }
        else if (methodCall.getJmmChild(0).getKind().equals("BinaryOp")){
            boolean exists = false;
            for (String method: symbolTable.getMethods()){
                if (method.equals(methodCall.get("name"))){
                    exists = true;
                }
            }
            if (exists){

                return symbolTable.getReturnType(methodCall.get("name"));
            }
            else{
                return new Type("",false);
            }
        }

        int line = Integer.valueOf(methodCall.get("lineStart"));
        int col = Integer.valueOf(methodCall.get("colStart"));


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

            for (int i=0;i<symbolTable.getFields().size();i++){
                if (symbolTable.getFields().get(i).getName().equals(methodCall.getJmmChild(0).get("value"))){
                    name = symbolTable.getFields().get(i).getName();
                    type = symbolTable.getFields().get(i).getType();
                }
            }


            boolean imported = false;

            for (String import_: symbolTable.getImports()){
                if (import_.equals(name)){
                    imported = true;
                }
                else if (import_.equals(methodCall.getJmmChild(0).get("value"))){
                    imported = true;
                    type = new Type(methodCall.getJmmChild(0).get("value"),false);
                }
            }






            if (!imported && (!symbolTable.getClassName().equals(name) || symbolTable.getSuper().equals(""))){
                VariableSemanticVisitor variableVisitor = new VariableSemanticVisitor(symbolTable);
                Type v = variableVisitor.visit(methodCall.getJmmChild(0),0);
                if (!v.getName().equals(symbolTable.getSuper())){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error on method " + methodCall.get("name") + ": Method Undeclared"));
                }


                return type;
            }

            else{

                return type;
            }


        }





        Type type = this.symbolTable.getReturnType(methodCall.get("name"));



        if (methodCall.getChildren().size() > 1){
            for(int i = 1; i < methodCall.getChildren().size(); i++){
                Type argType = new Type("", false);

                switch(methodCall.getJmmChild(i).getKind()) {
                    case "Integer":
                    case "Bool":
                    case "NewArr":
                    case "NewFunc":
                    case "Identifier":
                    case "This":
                    case "NegationOp":{
                        VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                        argType = variableSemanticVisitor.visit(methodCall.getJmmChild(i), 0);
                        break;
                    }
                    case "IndexOp":
                    case "LengthOp":{
                        IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                        argType = indexingSemanticVisitor.visit(methodCall.getJmmChild(i), 0);
                        break;
                    }

                    case "BinaryOP": {
                        BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                        argType = binOpSemanticVisitor.visit(methodCall.getJmmChild(i),0);
                        break;
                    }
                    default:{
                        argType = visit(methodCall.getJmmChild(i));
                        break;
                    }
                }
                int argLine = 1;//Integer.valueOf(methodCall.getJmmChild(1).getJmmChild(i).get("line"));
                int argCol = 1;//Integer.valueOf(methodCall.getJmmChild(1).getJmmChild(i).get("col"));
                List<Symbol> params = symbolTable.getParameters(methodCall.get("name"));

                if(!params.get(i-1).getType().equals(argType)) {

                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, argLine, argCol, "Error on method " + methodCall.get("name") + ": invalid method call, types of parameters are invalid. Parameter " + params.get(i-1).getName() + " expected " + params.get(i-1).getType().getName() + " but got " + argType.getName()));
                }
            }
        }


        return type;
    }


    private Type visitMethodDeclaration(JmmNode methodDeclaration, Integer dummy) {
        int numOfChildren = methodDeclaration.getChildren().size() - 1;

        if (numOfChildren > -1) {
            Type type = new Type("", false);

            switch (methodDeclaration.getJmmChild(numOfChildren).getKind()) {
                case "Integer":
                case "Bool":
                case "NewArr":
                case "NewFunc":
                case "Identifier":
                case "This":
                case "NegationOp": {
                    VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                    type = variableSemanticVisitor.visit(methodDeclaration.getJmmChild(numOfChildren), 0);
                    break;
                }
                case "ParOp":
                case "BinaryOp": {

                    BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                    type = binOpSemanticVisitor.visit(methodDeclaration.getJmmChild(numOfChildren), 0);
                    break;
                }

                default: {
                    type = visit(methodDeclaration.getJmmChild(numOfChildren));
                    break;
                }
            }

            int line = 1;//Integer.valueOf(methodDeclaration.getJmmChild(numOfChildren).get("line"));
            int col = 1;//Integer.valueOf(methodDeclaration.getJmmChild(numOfChildren).get("col"));
            if (type != null && !type.isArray()) {

                if (!type.getName().equals(methodDeclaration.getJmmChild(0).get("name"))) {
                    if (methodDeclaration.getJmmChild(numOfChildren).getKind().equals("FuncOp")) {
                        Type callerType = new Type("", false);
                        switch (methodDeclaration.getJmmChild(numOfChildren).getJmmChild(0).getKind()) {
                            case "Integer":
                            case "Bool":
                            case "NewArr":
                            case "NewFunc":
                            case "Identifier":
                            case "This":
                            case "NegationOp": {
                                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                                callerType = variableSemanticVisitor.visit(methodDeclaration.getJmmChild(numOfChildren).getJmmChild(0), 0);
                                break;
                            }
                            case "BinaryOP": {
                                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                                callerType = binOpSemanticVisitor.visit(methodDeclaration.getJmmChild(numOfChildren).getJmmChild(0), 0);
                                break;
                            }
                            default: {
                                callerType = visit(methodDeclaration.getJmmChild(numOfChildren).getJmmChild(0));
                                break;
                            }
                        }
                        if (!symbolTable.getImports().contains(callerType.getName())) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error: invalid return type on method " + methodDeclaration.get("name") + " method not declared or imported"));
                        }
                    } else {


                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error: invalid return type on method " + methodDeclaration.get("name") + ". Expected " + methodDeclaration.getJmmChild(0).get("name") + " but got " + type.getName()));
                    }
                }
            }
        }
        return new Type("none", false);
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
