package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class IndexLengthVisitor extends PreorderJmmVisitor<Integer, Type> {
    List<Report> reports = new ArrayList<>();
    SymbolTable symbolTable;

    public IndexLengthVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;

    }

    @Override
    protected void buildVisitor() {
        addVisit("IndexOp", this::visitIndex);
        addVisit("LengthOp", this::visitLength);
        setDefaultVisit(this::defaultVisitor);
    }

    private Type visitLength(JmmNode lengthNode, Integer dummy) {
        Type type = new Type("",false);
        String var = "";
        VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
        if (lengthNode.getJmmChild(0).getKind().equals("BinaryOp")){
            type = variableSemanticVisitor.visit(lengthNode.getJmmChild(0).getJmmChild(1));
            var = lengthNode.getJmmChild(0).getJmmChild(1).get("value");
        }
        else{

            type = variableSemanticVisitor.visit(lengthNode.getJmmChild(0));
            var = lengthNode.getJmmChild(0).get("value");
        }

        int line = 1;//Integer.valueOf(lengthNode.getJmmChild(0).get("line"));
        int col = 1;//Integer.valueOf(lengthNode.getJmmChild(0).get("col"));



        if(!type.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in LengthMethod: " + var + " is not an array"));
            return new Type("none", false);
        }
        return new Type("int", false);
    }

    private Type visitIndex(JmmNode index, Integer dummy) {
        Type l = new Type("", false);
        Type r = new Type("", false);

        switch(index.getJmmChild(0).getKind()) {
            case "BinaryOP":{
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                l = binOpSemanticVisitor.visit(index.getJmmChild(0), 0);
                break;
            }
            case "Integer":
            case "Bool":
            case "NewArr":
            case "NewFunc":
            case "Identifier":
            case "This":
            case "NegationOp":{
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                l = variableSemanticVisitor.visit(index.getJmmChild(0), 0);

                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                l = methodSemanticVisitor.visit(index.getJmmChild(0), 0);
                break;
            }

            default:{
                l = visit(index.getJmmChild(0));
                break;
            }
        }

        switch(index.getJmmChild(1).getKind()) {
            case "BinaryOP":{
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                r = binOpSemanticVisitor.visit(index.getJmmChild(1), 0);
                break;
            }
            case "Integer":
            case "Bool":
            case "NewArr":
            case "NewFunc":
            case "Identifier":
            case "This":
            case "NegationOp":{
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                r = variableSemanticVisitor.visit(index.getJmmChild(1), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                r = methodSemanticVisitor.visit(index.getJmmChild(1), 0);
                break;
            }
            default:{
                r = visit(index.getJmmChild(1));
                break;
            }
        }


        int lineLeft = 1;//Integer.valueOf(index.getJmmChild(0).get("line"));
        int colLeft = 1;//Integer.valueOf(index.getJmmChild(0).get("col"));
        int lineRight = 1;//Integer.valueOf(index.getJmmChild(1).get("line"));
        int colRight= 1;//Integer.valueOf(index.getJmmChild(1).get("col"));
        if(!l.isArray() ){
            System.out.println(l);
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft, "Error in Indexing: variable " + index.getJmmChild(0).get("value") + " is not an array"));
        }
        else if(!r.getName().equals("int") || r.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineRight, colRight, "Error in Indexing: index value must be of type int"));
        }

        return new Type("int", false);
    }

    public List<Report> getReports() {
        return this.reports;
    }

    private Type defaultVisitor(JmmNode node, Integer temp){
        for (JmmNode child : node.getChildren()) {
            visit(child, 0);
        }
        return new Type("",false);
    }


}
