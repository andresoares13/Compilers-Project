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

public class BinaryExpressionVisitor extends PreorderJmmVisitor <Integer, Type> {

    List<Report> reports = new ArrayList<>();
    SymbolTable symbolTable;


    public BinaryExpressionVisitor(SymbolTable table){
        this.symbolTable = table;
    }

    @Override
    protected void buildVisitor() {

        addVisit("BinaryOp", this::visitBinaryOp);
        setDefaultVisit(this::defaultVisitor);
    }

    private Type visitBinaryOp(JmmNode node, Integer temp){
        String operand = node.get("op");
        Type leftOperand = new Type("", false);
        Type rightOperand = new Type("", false);


        switch(node.getJmmChild(0).getKind()) {
            case "Integer":
            case "Bool":
            case "Identifier":
            case "NegationOp":
            case "NewArr":
            case "NewFunc":
            case "This":
            case "NotExpr": {
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                leftOperand = variableSemanticVisitor.visit(node.getJmmChild(0), 0);
                break;
            }

            default: {
                leftOperand = visit(node.getJmmChild(0));
                break;
            }
        }


        switch(node.getJmmChild(1).getKind()) {
            case "Integer":
            case "Bool":
            case "Identifier":
            case "NegationOp":
            case "NewArr":
            case "NewFunc":
            case "This":{
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                rightOperand = variableSemanticVisitor.visit(node.getJmmChild(1), 0);
                break;
            }

            default:{
                rightOperand = visit(node.getJmmChild(1));
                break;
            }
        }


        int lineLeft = 1;//Integer.parseInt(node.getJmmChild(0).get("line"));
        int colLeft = 1;//Integer.parseInt(node.getJmmChild(0).get("col"));
        int lineRight = 1;//Integer.parseInt(node.getJmmChild(1).get("line"));
        int colRight = 1;//Integer.parseInt(node.getJmmChild(1).get("col"));

        if(!leftOperand.getName().equals(rightOperand.getName())){

            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineRight, colRight, "Error in operation " + operand + " : operands have different types"));
        }
        else if( ( leftOperand.isArray() || rightOperand.isArray() ) && ( operand.equals("+") || operand.equals("-") || operand.equals("*") || operand.equals("/") || operand.equals("<") ) ) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft, "Error in operation " + operand + " : array cannot be used in this operation"));
        }
        else if(!leftOperand.getName().equals("int") && ( operand.equals("+") || operand.equals("-") || operand.equals("*") || operand.equals("/") || operand.equals("<") ) ) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft, "Error in operation " + operand + " : operands have invalid types for this operation. " + operand + " expects operands of type integer"));
        }
        else if(!leftOperand.getName().equals("boolean") && ( operand.equals("&&") || operand.equals("!") )) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, lineLeft, "Error in operation "+ operand + " : operands have invalid types for this operation. " + operand + " expects operands of type boolean"));
        }
        else {
            switch(operand) {
                case "<":
                case "&&":
                    return new Type("boolean", false);
                case "+":
                case "-":
                case "/":
                case "*":
                    return new Type("int", false);
                default:
                    return new Type(leftOperand.getName(), leftOperand.isArray());
            }
        }
        return new Type(leftOperand.getName(),leftOperand.isArray());
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
