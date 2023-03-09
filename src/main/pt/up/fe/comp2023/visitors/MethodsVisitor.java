package pt.up.fe.comp2023.visitors;
import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;

public class MethodsVisitor extends AJmmVisitor<Map<String, Triple<Type,List<Symbol>,List<Symbol>>>,Boolean>{

    @Override
    protected void buildVisitor() {
        addVisit("MethodDeclare", this::visitMethodDeclaration);
        addVisit("MethodDeclareMain", this::visitMethodDeclarationMain);
        setDefaultVisit(this::defaultVisitor); //methods_fields
    }


    private Boolean visitMethodDeclaration(JmmNode methodDeclaration, Map<String, Triple<Type,List<Symbol>,List<Symbol>>> methods_fields) {
        if (methodDeclaration.getKind().equals("MethodDeclare")){
            List<Symbol> params = new ArrayList<Symbol>(),
                vars= new ArrayList<>();

            Type methodType = new Type(
                    methodDeclaration.getJmmChild(0).get("name")
                    ,parseBoolean(methodDeclaration.getJmmChild(0).get("isArray"))
            );

            for (int i=1; i<methodDeclaration.getChildren().size();i++){
                JmmNode node = methodDeclaration.getChildren().get(i);
                if (node.getKind().equals("Param")){
                    visitParamOrVar(node,params);
                }
                else if(node.getKind().equals("VarDeclaration")){
                    visitParamOrVar(node,vars);
                }
                //else{} //node is statement or return
            }

            methods_fields.put(
                    methodDeclaration.get("name"),
                    new Triple<>(methodType,params,vars)
            );
            System.out.println(methodDeclaration.get("name"));
        }
        else{
            //not visiting a MethodDeclare
        }
        return true;
    }

    private Boolean visitMethodDeclarationMain(JmmNode methodDeclaration, Map<String, Triple<Type,List<Symbol>,List<Symbol>>> methods_fields) {
        if (methodDeclaration.getKind().equals("MethodDeclareMain")){
            List<Symbol> params = new ArrayList<Symbol>(),
                    vars= new ArrayList<>();

            Type methodType = new Type("void",false);
            params.add(new Symbol(
                    new Type("String",true)
                    ,methodDeclaration.get("parameter"))
            );
            for (int i=1; i<methodDeclaration.getChildren().size();i++){
                JmmNode node = methodDeclaration.getChildren().get(i);
                if(node.getKind().equals("VarDeclaration")){
                    visitParamOrVar(node,vars);
                }
            }
            methods_fields.put(
                    "main",
                    new Triple<>(methodType,params,vars)
            );
            System.out.println("main");
        }
        else{
            //not visiting a MethodDeclareMain
        }
        return true;
    }

    private Boolean defaultVisitor(JmmNode node, Map<String, Triple<Type,List<Symbol>,List<Symbol>>> methods_fields) {
        for (JmmNode child : node.getChildren()) {
            visit(child, methods_fields);
        }
        return true;
    }
    private Boolean visitParamOrVar(JmmNode node, List<Symbol> list){
        JmmNode typeNode=node.getJmmChild(0);
        Symbol symbol = new Symbol(
                new Type(
                        typeNode.get("name"),
                        parseBoolean(typeNode.get("isArray"))
                )
                ,
                node.get("name")
        );
        list.add(symbol);
        return true;
    }
}
