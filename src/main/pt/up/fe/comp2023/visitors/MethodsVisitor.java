package pt.up.fe.comp2023.visitors;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;

public class MethodsVisitor extends AJmmVisitor<Map<String,List<Symbol>>,Boolean>{

    @Override
    protected void buildVisitor() {
        addVisit("MethodDeclaration", this::visitMethodDeclaration);
        setDefaultVisit(this::defaultVisitor); //methods_fields
    }


    private Boolean visitMethodDeclaration(JmmNode methodDeclaration, Map<String,List<Symbol>> methods_fields) {
        if (methodDeclaration.getKind().equals("MethodDeclare")){
            List<Symbol> symbols = new ArrayList<Symbol>();
            Type tempType1 = new Type(methodDeclaration.getJmmChild(0).get("name"), parseBoolean(methodDeclaration.getJmmChild(0).get("isArray")));
            Symbol tempSymbol = new Symbol(tempType1,methodDeclaration.get("name"));
            symbols.add(tempSymbol);

            for (int i=1; i<methodDeclaration.getChildren().size();i++){

                if (methodDeclaration.getChildren().get(i).getKind().equals("Param")){
                    Type tempType = new Type(methodDeclaration.getChildren().get(i).getJmmChild(0).get("name"), parseBoolean(methodDeclaration.getChildren().get(i).getJmmChild(0).get("isArray")));
                    Symbol temp = new Symbol(tempType,methodDeclaration.getChildren().get(i).get("name"));
                    symbols.add(temp);
                }
                else {
                    break;
                }
            }
            methods_fields.put(methodDeclaration.get("name"),symbols);
            System.out.println(methodDeclaration.get("name"));
        }
        else{
            
        }




        return true;
    }


    private Boolean defaultVisitor(JmmNode program, Map<String,List<Symbol>> methods_fields) {

        for (JmmNode child : program.getChildren()) {
            visit(child, methods_fields);
        }
        return true;

    }
}
