package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

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
    }

    private Type visitBinaryOp(JmmNode node, Integer temp){
        String operand = node.get("op");
        Type leftOperand = new Type("", false);
        Type rightOperand = new Type("", false);




        return new Type(leftOperand.getName(),leftOperand.isArray());
    }
}
