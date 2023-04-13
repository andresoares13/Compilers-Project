package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;

public class MyJmmOptimization implements JmmOptimization {
    private  Map<String, Triple<Boolean,Type,String>> fieldsState = new HashMap<>();
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        return JmmOptimization.super.optimize(semanticsResult);
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        StringBuilder codeBuilder = new StringBuilder();
        List< Report > reports = new ArrayList<>();
        SymbolTableStore st = (SymbolTableStore) jmmSemanticsResult.getSymbolTable();
        for(String s:st.getImports()) {
            codeBuilder.append("import "+s+";\n");
        }
        codeBuilder.append('\n');
        codeBuilder.append(
                st.getClassName() +(st.getSuper().equals("")?"":" extends "+st.getSuper())+ " {\n");
        for(Symbol v:st.getFields()){
            codeBuilder.append(".field private "+v.getName()+toOllirType(v.getType()) + ";\n");
            fieldsState.put(v.getName(), new Triple(false,v.getType(),v.getName()));
        }
                codeBuilder.append(".construct "+st.getClassName()+"().V {\n" +
                        "invokespecial(this, \"<init>\").V;\n" +
                    "}\n\n");
        for(String methodName :st.getMethods()){
            codeBuilder.append(
                    ".method ");
            if(st.getMethodIsPublic(methodName))
                codeBuilder.append("public ");
            codeBuilder.append(methodName+"(");
            boolean first=true;
            for(Symbol p:st.getParameters(methodName)){
                if(first)
                    first=false;
                else
                    codeBuilder.append(", ");
                codeBuilder.append(p.getName() + toOllirType(p.getType()));
            }
            codeBuilder.append(")");
            codeBuilder.append(toOllirType(st.getReturnType(methodName)));
            codeBuilder.append(" {\n");
            //method statements
            //TODO
            JmmNode methodNode = getMethodNode(jmmSemanticsResult,methodName);
            codeBuilder.append(methodVisitor(methodNode,jmmSemanticsResult));

            codeBuilder.append("}\n");
        }
        codeBuilder.append("}\n");

        String ollirCode = codeBuilder.toString();
        OllirResult result = new OllirResult(ollirCode,jmmSemanticsResult.getConfig());

        return result;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return JmmOptimization.super.optimize(ollirResult);
    }

    String toOllirType(Type type){
        StringBuilder sb=new StringBuilder();
        if(type.isArray())
            sb.append(".array");

        switch(type.getName()){
            case "int":
                sb.append(".i32");
                break;
            case "boolean":
                sb.append(".bool");
                break;
            case "void":
                sb.append(".V");
                break;
            default:
                sb.append("."+type.getName());
        }
        return sb.toString();
    }
    Type getOllirType(String s){
        String end = s.substring(s.lastIndexOf('.'));
        boolean isArray = false;
        if(end.equals(".array")){
            isArray=true;
            end = s.substring(0,s.lastIndexOf('.'));
            end = end.substring(end.lastIndexOf('.'));
        }
        if(end.equals(".i32"))
            return new Type("int",isArray);
        if(end.equals(".bool"))
            return new Type("boolean",isArray);
        return new Type(end.substring(1),isArray);
    }
    String getTypeDefaultValue(String type){
        switch(type){
            //TODO

        }
        return "";
    }
    JmmNode getMethodNode(JmmSemanticsResult semanticsResult, String method ){
        JmmNode root= semanticsResult.getRootNode();
        for(JmmNode rootChild : root.getChildren()){
            if(rootChild.getKind().equals("ClassDeclare")) {
                for(JmmNode classNode : rootChild.getChildren()) {
                    if(classNode.getKind().equals("MethodDeclare")){
                        if(classNode.get("name").equals(method))
                            return classNode;
                    }
                    if(method.equals("main") && classNode.getKind().equals("MethodDeclareMain")){
                        return classNode;
                    }
                }
            }
        }
        return null;
    }
    Symbol getMethodVariable(JmmSemanticsResult semanticsResult, String method, String localVariable){
        Optional<Symbol> first = semanticsResult.getSymbolTable().getLocalVariables(method).stream().filter((n) -> n.getName().equals(localVariable)).findFirst();
        if(first.isPresent())
            return first.get();
        return null;
    }
    Pair<Boolean,Triple<Boolean,Type,String>> getVariableState(String variableName, Map<String,Triple<Boolean,Type,String>> methodVarsState){
        var local = methodVarsState.get(variableName);
        if(local!=null)
            return new Pair(false, // not a field
                    local);
        var field = fieldsState.get(variableName);
        return new Pair(true, // is a field
                field);
    }
    Triple<Boolean,Type,String> addTemporaryVariable(Map<String, Triple<Boolean,Type,String>> localVarsState, Type type){
        for(int i=0;;++i){
            if(localVarsState.get( "tmp"+ i)==null) {
                String name = "tmp"+1;
                var triple = new Triple<>(true,type,name); //assumed to be immediately initialized
                localVarsState.put(name,triple);
                return triple;
            }
        }
    }
    Pair<String,ArrayList<String>> expressionVisitor(JmmNode node, JmmSemanticsResult semanticsResult, Map<String, Triple<Boolean,Type,String>> localVarsState, String methodName){
        ArrayList<String> previousStatements = new ArrayList<>();
        String result="0.i32";
        switch (node.getKind()){//TODO
            case "BinaryOp": {
                /*
                : expression op='&&' expression #BinaryOp
                | expression op='<' expression #BinaryOp
                | expression op=('+' | '-') expression #BinaryOp
                | expression op=('*' | '/') expression #BinaryOp
                */
                Pair<String,ArrayList<String>> exp1 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName),
                        exp2 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName);
                previousStatements = exp1.b;
                for(String s: exp2.b)
                    previousStatements.add(s);
                String op = node.get("op");
                Type opType;
                if(op.equals("<") || op.equals("&&"))
                    opType = new Type("boolean",false);
                else
                    opType = new Type("int",false);
                var tmp = addTemporaryVariable(localVarsState,opType);
                previousStatements.add(
                        tmp.c + toOllirType(tmp.b) + " :=" + toOllirType(tmp.b) + " "
                        + exp1.a + " " + op + toOllirType(opType) + " " + exp2.a + ";\n"
                );
                result = tmp.c + toOllirType(tmp.b);
                break;
            }
            case "IndexOp":break;
            case "LengthOp":break;
            case "FuncOp":break;
            case "NewArr":break;
            case "NewFunc":
                result = "new(" + node.get("name") + ")." + node.get("name");
                break;
            case "NegationOp": {
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState, methodName);
                var tmp = addTemporaryVariable(localVarsState, getOllirType(expression.a));
                previousStatements = expression.b;
                previousStatements.add(tmp.c + toOllirType(tmp.b) + " :=" + toOllirType(tmp.b) + " " + expression.a + ";\n");
                result = tmp.c + toOllirType(tmp.b);
                break;
            }
            case "ParOp":
                return expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName);
            case "Integer":
                result = node.get("value")+".i32";
                break;
            case "Identifier": {
                var varState = getVariableState(node.get("value"), localVarsState);
                if (varState.a) { //is Field
                    result = "getfield(this," + varState.b.c + toOllirType(varState.b.b) + ")" + toOllirType(varState.b.b);
                } else {
                    if (varState.a) { //not initialized
                        previousStatements.add(
                                varState.b.c + toOllirType(varState.b.b)
                                        + " :=" + toOllirType(varState.b.b)
                                        + " " + getTypeDefaultValue(varState.b.b.getName()) + ";\n"
                        );
                    }
                    result = node.get("value") + toOllirType(varState.b.b);
                }
                break;
            }
            case "This":
                result = "this."+semanticsResult.getSymbolTable().getClassName(); //TODO confirm
                break;
            case "Bool":
                result = node.get("value")+".bool";
                break;
        }
        return new Pair<>(result,previousStatements);
    }
    String methodVisitor(JmmNode methodNode, JmmSemanticsResult semanticsResult){
        String methodName = methodNode.get("name");
        Map<String, Triple<Boolean,Type,String>> localVarsState = new HashMap<>();
        for(var aVar:semanticsResult.getSymbolTable().getLocalVariables(methodName))
            localVarsState.put(aVar.getName(), new Triple<>(false, aVar.getType(),aVar.getName()));
        for(int i=0; i<semanticsResult.getSymbolTable().getParameters(methodName).size() ;++i) {
            var aVar= semanticsResult.getSymbolTable().getParameters(methodName).get(i);
            localVarsState.put(aVar.getName(), new Triple<>(true, aVar.getType(), "$"+i ));
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(JmmNode child : methodNode.getChildren()) {
            switch (child.getKind()){//TODO
                case "VarDeclareStatement": {
                    var state = localVarsState.get(child.get("name"));
                    var state2 = new Triple<>(true, state.b,state.c);
                    localVarsState.put(child.get("name"), state2);

                    var expression = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState,methodNode.get("name"));
                    for (String s : expression.b)
                        stringBuilder.append(s);
                    String type = toOllirType(getMethodVariable(semanticsResult, methodName, child.get("name")).getType());
                    stringBuilder.append(
                            child.get("name") + type
                            + " :=" + type
                            + " " + expression.a + ";\n"
                    );
                    break;
                }
                case "SemiColon": {
                    var statements = expressionVisitor(child,semanticsResult,localVarsState,methodName);
                    for(String s : statements.b)
                        stringBuilder.append(s);
                    stringBuilder.append(
                            statements.a + ";\n"
                    );
                    break;
                }
                case "Brackets":
                case "IfElseStatement":
                case "WhileStatement":
                case "ArrayAccess":
                    break;


                //catch all expressions as the node is from the return statement.
                case "BinaryOp":
                case "IndexOp":
                case "LengthOp":
                case "FuncOp":
                case "NewArr":
                case "NewFunc":
                case "NegationOp":
                case "ParOp":
                case "Integer":
                case "Bool":
                case "Identifier":
                case "This": {
                    var expression = expressionVisitor(child, semanticsResult, localVarsState,methodName);
                    for (String s : expression.b)
                        stringBuilder.append(s);
                    var retType = toOllirType(semanticsResult.getSymbolTable().getReturnType(methodName));
                    stringBuilder.append("ret" + retType
                        + " " + expression.a +";\n"
                    );

                    break;
                }

            }
        }
        return stringBuilder.toString();
    }
}

/*TODO
    initialize variables on first use.
    array out of bounds?
 */