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

public class ConditionalStatementVisitor extends PreorderJmmVisitor<Integer, Type> {

    List<Report> reports = new ArrayList<>();
    SymbolTable symbolTable;

    public ConditionalStatementVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;

    }


    @Override
    protected void buildVisitor() {
        addVisit("IfElseStatement", this::visitIfStatement);
        addVisit("WhileStatement", this::visitWhileStatement);
        setDefaultVisit(this::defaultVisitor);
    }



    private Type visitIfStatement(JmmNode ifStatement, Integer dummy) {
        Type type = new Type("", false);

        switch(ifStatement.getJmmChild(0).getKind()) {
            case "Integer":
            case "Bool":
            case "Identifier":
            case "NegationOp":
            case "NewArr":
            case "NewFunc":
            case "This":
            case "NotExpr": {
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                type = variableSemanticVisitor.visit(ifStatement.getJmmChild(0), 0);
                break;
            }
            case "BinaryOp":{
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                type = binOpSemanticVisitor.visit(ifStatement.getJmmChild(0), 0);
                break;
            }
            case "IndexOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                type = indexingSemanticVisitor.visit(ifStatement.getJmmChild(0), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                type = methodSemanticVisitor.visit(ifStatement.getJmmChild(0), 0);
                break;
            }

            default:{
                type = visit(ifStatement.getJmmChild(0));
                break;
            }
        }
        int line = 1;//Integer.valueOf(ifStatement.getJmmChild(0).get("line"));
        int col = 1;//Integer.valueOf(ifStatement.getJmmChild(0).get("col"));
        if(!type.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in IfStatement: condition has to be of type boolean"));
        }
        else if(type.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in IfStatement: condition cannot be an array"));
        }
        return type;
    }




    private Type visitWhileStatement(JmmNode whileStatement, Integer dummy) {
        Type type = new Type("", false);

        switch(whileStatement.getJmmChild(0).getKind()) {
            case "Integer":
            case "Bool":
            case "Identifier":
            case "NegationOp":
            case "NewArr":
            case "NewFunc":
            case "This":
            case "NotExpr": {
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                type = variableSemanticVisitor.visit(whileStatement.getJmmChild(0), 0);
                break;
            }
            case "BinaryOp":{
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                type = binOpSemanticVisitor.visit(whileStatement.getJmmChild(0), 0);
                break;
            }
            case "IndexOp":{
                //IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                //type = indexingSemanticVisitor.visit(whileStatement.getJmmChild(0), 0);
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                Type type2 = variableSemanticVisitor.visit(whileStatement.getJmmChild(0).getJmmChild(0).getJmmChild(0),0);
                Type type3 = variableSemanticVisitor.visit(whileStatement.getJmmChild(0).getJmmChild(0).getJmmChild(1),0);
                Type type4 = variableSemanticVisitor.visit(whileStatement.getJmmChild(0).getJmmChild(1),0);
                if (type2.getName().equals("int") && type3.isArray() && whileStatement.getJmmChild(0).getJmmChild(0).get("op").equals("<") && type4.getName().equals("int")){
                    type = new Type("boolean",false);
                }
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                type = methodSemanticVisitor.visit(whileStatement.getJmmChild(0), 0);
                break;
            }
            case "LengthOp":{
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                Type type2 = variableSemanticVisitor.visit(whileStatement.getJmmChild(0).getJmmChild(0).getJmmChild(0),0);
                Type type3 = variableSemanticVisitor.visit(whileStatement.getJmmChild(0).getJmmChild(0).getJmmChild(1),0);
                if (type2.getName().equals("int") && type3.isArray() && whileStatement.getJmmChild(0).getJmmChild(0).get("op").equals("<")){
                    type = new Type("boolean",false);
                }
                break;
            }

            default:{
                type = visit(whileStatement.getJmmChild(0));
                break;
            }
        }
        int line = 1;//Integer.valueOf(whileStatement.getJmmChild(0).get("line"));
        int col = 1;//Integer.valueOf(whileStatement.getJmmChild(0).get("col"));
        if(!type.getName().equals("boolean")) {
            System.out.println(type);
            System.out.println(whileStatement.getChildren());
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in WhileStatement: condition has to be of type boolean"));
        }
        else if(type.isArray() || type.getName().equals("intArr")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in WhileStatement: condition cannot be an array"));
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
