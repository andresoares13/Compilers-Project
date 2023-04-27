package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;
import java.util.stream.Collectors;

public class MyJmmOptimization implements JmmOptimization {
    private final Map<String, VarState> fieldsState = new HashMap<>();
    private int ifLabelCount= 0, whileLabelCount =0;

    private int rFlagValue = -1;
    private boolean oFlagValue = false;
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        return JmmOptimization.super.optimize(semanticsResult);
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return JmmOptimization.super.optimize(ollirResult);
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        StringBuilder codeBuilder = new StringBuilder();
        {
            String regAllocation = jmmSemanticsResult.getConfig().get("registerAllocation");
            if (regAllocation != null) {
                rFlagValue = Integer.parseInt(regAllocation);
            }
            String optimize = jmmSemanticsResult.getConfig().get("optimize");
            if (optimize != null) {
                oFlagValue = Boolean.parseBoolean(optimize);
            }
        }
        SymbolTable st = jmmSemanticsResult.getSymbolTable();
        for(String s:st.getImports()) {
            codeBuilder.append("import ").append(s).append(";\n");
        }
        codeBuilder.append(st.getClassName()).append(st.getSuper().equals("") ? "" : " extends " + st.getSuper()).append(" {\n");
        for(Symbol v:st.getFields()){
            codeBuilder.append(".field private ").append(v.getName()).append(typeToOllir(v.getType())).append(";\n");
            fieldsState.put(v.getName(), new VarState(false,v.getType(),v.getName(),false, false, false));
        }
        codeBuilder.append(".construct ").append(st.getClassName()).append("().V {\n").append("invokespecial(this, \"<init>\").V;\n").append("}\n");

        ArrayList<JmmNode> methodNodeList = new ArrayList<>(
                    jmmSemanticsResult.getRootNode().getChildren().stream().filter(
                        (n) -> n.getKind().equals("ClassDeclare")
                    ).collect(
                        ArrayList<JmmNode>::new,
                        (arr, node) -> arr.addAll(
                            node.getChildren().stream().filter(
                                    (m) -> m.getKind().startsWith("MethodDeclare")
                                ).collect(Collectors.toList())),
                        ArrayList::addAll
                    )
        );
        for(JmmNode methodNode : methodNodeList) {
            String methodName = methodNode.get("name");
            codeBuilder.append(".method ");
            if(methodNode.get("isPublic").equals("true")) {
                codeBuilder.append("public ");
            }
            else {
                codeBuilder.append("private ");
            }

            if(methodName.equals("main"))
                codeBuilder.append("static ");

            codeBuilder.append(methodName).append("(");
            boolean first=true;
            for(Symbol p:st.getParameters(methodName)){
                if(first)
                    first=false;
                else
                    codeBuilder.append(", ");
                codeBuilder.append(p.getName()).append(typeToOllir(p.getType()));
            }
            codeBuilder.append(")");
            codeBuilder.append(typeToOllir(st.getReturnType(methodName)));
            codeBuilder.append(" {\n");

            codeBuilder.append(methodVisitor(methodNode,jmmSemanticsResult));

            codeBuilder.append("}\n");
        }
        codeBuilder.append("}\n");

        String ollirCode = codeBuilder.toString();

        //return new OllirResult(ollirCode,jmmSemanticsResult.getConfig());
        return new OllirResult(jmmSemanticsResult,ollirCode,jmmSemanticsResult.getReports());
    }

    static class VarState {
        public boolean isInitialized; //is initialized
        public Type type; //type of var
        public String name; //name of var
        public boolean isOpen; //is open for reuse
        public boolean isConstant; //is constant
        public String value;
        public boolean isTemporary;
        public boolean isRead = false;

        public VarState(boolean init, Type type, String name, boolean open, boolean constant, boolean temporary){
            isInitialized = init;
            this.type = type;
            this.name = name;
            isOpen = open;
            isConstant = constant;
            isTemporary = temporary;
            value = "";
        }
        public VarState(boolean init, Type type, String name, boolean open, boolean constant, boolean temporary, String value){
            isInitialized =init;
            this.type =type;
            this.name =name;
            this.isOpen =open;
            isConstant =constant;
            isTemporary = temporary;
            this.value = value;
        }
    }
    String typeToOllir(Type type){
        StringBuilder sb=new StringBuilder();
        if(type.isArray())
            sb.append(".array");
        switch (type.getName()) {
            case "int" -> sb.append(".i32");
            case "boolean" -> sb.append(".bool");
            case "void" -> sb.append(".V");
            default -> sb.append(".").append(type.getName());
        }
        return sb.toString();
    }
    Type ollirToType(String s){
        int lastPeriod = s.lastIndexOf('.'),
                secondLastPeriod = s.lastIndexOf('.',lastPeriod-1);
        boolean isArray = false;
        if(lastPeriod == -1) {
            throw new RuntimeException("String is not an Ollir Type.");
        }
        if(secondLastPeriod != -1){
            if(s.substring(secondLastPeriod,lastPeriod).equals(".array"))
                isArray = true;
        }
        String end = s.substring(lastPeriod);
        if(end.equals(".i32"))
            return new Type("int",isArray);
        if(end.equals(".bool"))
            return new Type("boolean",isArray);
        return new Type(end.substring(1),isArray);
    }
    String getTypeDefaultValue(Type type){
        return switch (type.getName()) {
            case "int" -> "0.i32";
            case "boolean" -> "1.bool";
            default -> "";
        };
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
        return first.orElse(null);
    }
    Pair<Boolean, VarState> getVariableState(String variableName, Map<String, VarState> methodVarsState){
        var local = methodVarsState.get(variableName);
        if(local!=null)
            return new Pair<>(false, // not a field
                    local);
        var field = fieldsState.get(variableName);
        if(field!=null)
            return new Pair<>(true, // is a field
                field);
        else
            return null; //check if it's an import.
    }
    void setVariableInitialized(String variableName, Map<String, VarState> methodVarsState){
        boolean isLocal = methodVarsState.containsKey(variableName);
        VarState state;
        if(isLocal){
            state = methodVarsState.get(variableName);
        }else{
            state = fieldsState.get(variableName);
        }
        state.isInitialized = true;
    }
    VarState addTemporaryVariable(Map<String, VarState> localVarsState, Type type){
        for(int i=0;;++i){
            VarState state = localVarsState.get("tmp" + i);
            if(state == null) {
                String name = "tmp" + i;
                VarState addedTemp = new VarState(true,type,name,false, false, true);
                localVarsState.put(name,addedTemp); //assumed to be immediately initialized
                return addedTemp;
            }
            if(state.isOpen) {
                state.isOpen = false;
                state.type = type;
                return state;
            }
        }
    }
    void releaseTemporaryVariable(VarState var){
        if(rFlagValue != -1)
            var.isOpen = true;
    }
    ExpressionVisitResult expressionVisitor(JmmNode node, JmmSemanticsResult semanticsResult, Map<String, VarState> localVarsState){
        ArrayList<String> previousStatements = new ArrayList<>();
        ArrayList<VarState> previousReleases = new ArrayList<>();
        String result="undefined";
        Boolean isConstant = false;
        Type resultType = null;
        switch (node.getKind()){
            case "BinaryOp": { //TODO identities
                ExpressionVisitResult exp1 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState),
                        exp2 = expressionVisitor(node.getJmmChild(1),semanticsResult,localVarsState);
                previousStatements = exp1.previousStatements;
                previousStatements.addAll(exp2.previousStatements);
                previousReleases = exp1.freeVars;
                previousReleases.addAll(exp2.freeVars);
                String op = node.get("op");
                if(op.equals("<") || op.equals("&&"))
                    resultType = new Type("boolean",false);
                else
                    resultType = new Type("int",false);

                isConstant = exp1.isConstant && exp2.isConstant && oFlagValue;
                if(isConstant){
                    switch(op){
                        case "<":
                            result = (Integer.parseInt(exp1.result.substring(0,exp1.result.lastIndexOf("."))) < Integer.parseInt(exp2.result.substring(0,exp2.result.lastIndexOf("."))))+ ".bool";
                            break;
                        case "&&":
                            result = (exp1.result.equals("true.bool") && exp2.result.equals("true.bool")) + ".bool";
                            break;
                        case "+":
                            result = (Integer.parseInt(exp1.result.substring(0,exp1.result.lastIndexOf("."))) + Integer.parseInt(exp2.result.substring(0,exp2.result.lastIndexOf(".")))) + ".i32";
                            break;
                        case "-":
                            result = (Integer.parseInt(exp1.result.substring(0,exp1.result.lastIndexOf("."))) - Integer.parseInt(exp2.result.substring(0,exp2.result.lastIndexOf(".")))) + ".i32";
                            break;
                        case "/":
                            if(exp2.result.equals("0.i32")){
                                //divide by 0 Error.
                            }
                            result = Integer.parseInt(exp1.result.substring(0, exp1.result.lastIndexOf("."))) / Integer.parseInt(exp2.result.substring(0, exp2.result.lastIndexOf("."))) + ".i32";
                            break;
                        case "*":
                            result = Integer.parseInt(exp1.result.substring(0, exp1.result.lastIndexOf("."))) * Integer.parseInt(exp2.result.substring(0, exp2.result.lastIndexOf("."))) + ".i32";
                            break;
                    }

                }
                result = exp1.result + " " + op + typeToOllir(resultType) + " " + exp2.result;
                if(oFlagValue){
                    switch (op){
                        case "&&":
                            if((exp1.isConstant && exp1.result.equals("false.bool")) || (exp2.isConstant && exp2.result.equals("false.bool"))){
                                isConstant = true;
                                result = "false.bool";
                            }
                            break;
                        case "+":
                            if(exp1.isConstant && exp1.result.equals("0.i32")){
                                result = exp2.result;
                                break;
                            }if(exp2.isConstant && exp2.result.equals("0.i32")){
                            result = exp1.result;
                            break;
                        }
                            break;
                        case "-":
                            if(exp2.isConstant && exp2.result.equals("0.i32")){
                                result = exp1.result;
                                break;
                            }
                            break;
                        case "/":
                            if(exp2.isConstant && exp2.result.equals("1.i32")){
                                result = exp1.result;
                                break;
                            }
                            if(exp2.result.equals("0.i32")){
                                //divide by 0 Error.
                            }
                            break;
                        case "*":
                            if(exp1.isConstant && exp1.result.equals("1.i32")){
                                result = exp2.result;
                                break;
                            }if(exp2.isConstant && exp2.result.equals("1.i32")){
                            result = exp1.result;
                            break;
                        }
                        default:
                    }
                }
                if(    !     (resultType.getName().equals("int")
                        &&
                                ((node.getJmmParent().getKind().equals("IndexOp") && node.getJmmParent().getJmmChild(1).equals(node))
                            ||
                                (node.getJmmParent().getKind().equals("ArrayAccess") && node.getJmmParent().getJmmChild(0).equals(node))
                                )
                            )
                    && !node.getJmmParent().getKind().equals("VarDeclareStatement")){ //needs to be put in a temp variable.
                    var tmp = addTemporaryVariable(localVarsState, resultType);
                    previousReleases.add(tmp);

                    previousStatements.add(
                            tmp.name + typeToOllir(tmp.type) + " :=" + typeToOllir(tmp.type) + " "
                                    + exp1.result + " " + op + typeToOllir(resultType) + " " + exp2.result + ";\n"
                    );
                    result = tmp.name + typeToOllir(tmp.type);
                }
                break;
            }
            case "IndexOp":{
                ExpressionVisitResult exp1 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState),
                        exp2 = expressionVisitor(node.getJmmChild(1),semanticsResult,localVarsState);
                if( ! ollirToType(exp2.result).getName().equals("int"))
                    {result = "Non integer used as array index.";break;}
                //throw new RuntimeException("Non integer used as array index.");
                if(! ollirToType(exp1.result).isArray())
                    {result = "Using index operator[] on a non array.";break;}
                    //throw new RuntimeException("Using index operator[] on a non array.");
                previousStatements = exp1.previousStatements;
                previousStatements.addAll(exp2.previousStatements);
                previousReleases = exp1.freeVars;
                previousReleases.addAll(exp2.freeVars);
                var typeExp1 = ollirToType(exp1.result);
                String target;
                if(exp2.isConstant && oFlagValue){
                    var tmp=addTemporaryVariable(localVarsState,new Type("int",false));
                    previousReleases.add(tmp);
                    target = tmp.name + typeToOllir(tmp.type);
                    previousStatements.add(target + " :=.i32 " + exp2.result + ";\n");
                }else{
                    target = exp2.result;
                }
                result = exp1.result.substring(0,exp1.result.length()-typeToOllir(typeExp1).length()) + "["+target+"].i32";
                if(
                        (node.getJmmParent().getKind().equals("ArrayAccess")  && node.getJmmParent().getJmmChild(0) == node)
                        || node.getJmmParent().getKind().equals("IndexOp")
                ){ //creates a temp variable if this IndexOp is itself an IndexOp
                    var tmp=addTemporaryVariable(localVarsState,new Type("int",false));
                    previousReleases.add(tmp);
                    target = tmp.name + typeToOllir(tmp.type);
                    previousStatements.add(target + " :=.i32 " + result + ";\n");
                    result = tmp.name + typeToOllir(tmp.type);
                }
                resultType = new Type("int",false);
                break;
            }
            case "LengthOp": {
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState);
                previousStatements = expression.previousStatements;
                previousReleases = expression.freeVars;
                var tmp = addTemporaryVariable(localVarsState,ollirToType(".i32"));
                previousReleases.add(tmp);
                previousStatements.add(tmp.name + typeToOllir(tmp.type)+" :=.i32 arraylength(" + expression.result + ").i32;\n");
                result = tmp.name + typeToOllir(tmp.type);
                resultType = new Type("int",false);
                break;
            }
            case "FuncOp":{
                var calledExpression = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
                previousStatements = calledExpression.previousStatements;
                previousReleases = calledExpression.freeVars;
                String invokeType,
                        target = calledExpression.result;
                List<String> arguments = new ArrayList<>();
                if(calledExpression.result.equals("this")){
                    var calledMethod = getMethodNode(semanticsResult,node.get("name"));
                    if(calledMethod == null){
                        result = "Method does not exist";
                        break;
                        //throw new RuntimeException("Method does not exist");
                    }
                    if(calledMethod.get("isPublic").equals("true")){
                        invokeType = "virtual";
                    }else{
                        invokeType = "special";
                    }
                    resultType = semanticsResult.getSymbolTable().getReturnType(node.get("name"));
                }else {
                    if(calledExpression.type.isArray()) {
                        //throw new RuntimeException("Calling function of array");
                        result = "Calling function of array";break;
                    }
                    switch(calledExpression.type.getName()){
                        case "int":
                        case "boolean":
                            //throw new RuntimeException("Calling function of a primitive type);
                        case "import":
                            invokeType="static";
                            resultType = new Type("void",false);
                            break;
                        default:
                            invokeType="virtual";
                            if(calledExpression.type.getName().equals(semanticsResult.getSymbolTable().getClassName()))
                                resultType = semanticsResult.getSymbolTable().getReturnType(node.get("name"));
                            else
                                resultType = new Type("void",false);
                            break;
                    }
                }
                for(int i =1;i<node.getChildren().size();++i){
                    var expressionResult = expressionVisitor(node.getJmmChild(i),semanticsResult,localVarsState);
                    previousStatements.addAll(expressionResult.previousStatements);
                    previousReleases.addAll(expressionResult.freeVars);
                    if(node.getJmmChild(i).getKind().equals("IndexOp")){
                        var tmp = addTemporaryVariable(localVarsState,ollirToType(expressionResult.result));
                        previousReleases.add(tmp);
                        previousStatements.add(tmp.name +typeToOllir(tmp.type)+" :="+typeToOllir(tmp.type) + " " + expressionResult.result + ";\n");
                        arguments.add(tmp.name +typeToOllir(tmp.type));
                    }else {
                        arguments.add(expressionResult.result);
                    }
                }

                if (invokeType.equals("static") || node.getJmmParent().getKind().equals("SemiColon")) {
                    result = "invoke"+ invokeType + "(" + target + ", \"" + node.get("name") + (arguments.size()>0?"\", ":"\"") + arguments.stream().reduce((String s1,String s2)->s1+", "+s2).orElse("") +")" + typeToOllir(resultType);
                } else {
                    var tmp = addTemporaryVariable(localVarsState, resultType);
                    previousReleases.add(tmp);
                    previousStatements.add(tmp.name + typeToOllir(tmp.type) + " :=" + typeToOllir(tmp.type) +
                            " invoke" + invokeType + "(" + target + ", \"" + node.get("name") + (arguments.size() > 0 ? "\", " : "\"") + arguments.stream().reduce((String s1, String s2) -> s1 + ", " + s2).orElse("") + ")" + typeToOllir(resultType) + ";\n");
                    result = tmp.name + typeToOllir(tmp.type);
                }
                break;
            }
            case "NewArr":{
                var expression = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
                previousStatements = expression.previousStatements;
                previousReleases = expression.freeVars;
                result = "new(array, " + expression.result + ").array.i32";
                resultType = new Type("int",true);
                break;
            }
            case "NewFunc": {
                var tmp = addTemporaryVariable(localVarsState, new Type(node.get("name"), false));
                previousReleases.add(tmp);
                previousStatements.add(tmp.name + typeToOllir(tmp.type) + " :=" + typeToOllir(tmp.type) + " new(" + tmp.type.getName() + ")" + typeToOllir(tmp.type) + ";\n");
                previousStatements.add("invokespecial(" + tmp.name + typeToOllir(tmp.type) + ", \"<init>\").V;\n");
                result = tmp.name + typeToOllir(tmp.type);
                resultType = new Type(node.get("name"), false);
                break;
            }
            case "NegationOp": {
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState);
                if( ! ollirToType(expression.result).getName().equals("boolean")){
                    result = "Negation operator on a non boolean.";break;
                    //throw new RuntimeException("Negation operator on a non boolean.");
                }
                if(expression.isConstant && oFlagValue){
                    result = String.valueOf(!expression.result.equals("true.bool")) + ".bool";
                    isConstant = true;
                    break;
                }
                var tmp = addTemporaryVariable(localVarsState, ollirToType(expression.result));
                previousReleases.add(tmp);
                previousStatements = expression.previousStatements;
                previousReleases = expression.freeVars;
                previousStatements.add(tmp.name + ".bool" + " :=.bool !.bool " + expression.result + ";\n");
                result = tmp.name + ".bool";
                resultType = new Type("boolean",false);
                break;
            }
            case "ParOp":
                return expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
            case "Integer":
                result = node.get("value")+".i32";
                isConstant=true;
                resultType = new Type("int",false);
                break;
            case "Identifier": {
                var varState = getVariableState(node.get("value"), localVarsState);
                if(varState == null){ //check imports
                    var imported = semanticsResult.getSymbolTable().getImports().stream().filter((String n)-> n.equals(node.get("value"))).findFirst();
                    if(imported.isEmpty()) {
                        result = "Undeclared identifier.";break;
                        //throw new RuntimeException("Undeclared identifier.");
                    }
                    else {
                        result = imported.get();
                        resultType = new Type("import",false);//.import is a local tag, not added to ollir. Important for FuncOp
                    }
                    break;
                }
                if (varState.a) { //is Field
                    var tmp = addTemporaryVariable(localVarsState,varState.b.type);
                    previousReleases.add(tmp);
                    previousStatements.add(tmp.name + typeToOllir(tmp.type) + " :=" + typeToOllir(tmp.type) + " " + "getfield(this," + varState.b.name + typeToOllir(varState.b.type) + ")" + typeToOllir(varState.b.type) + ";\n");
                    result = tmp.name + typeToOllir(tmp.type);
                    resultType = tmp.type;
                } else {
                    resultType = varState.b.type;
                    varState.b.isRead = true;
                    if(varState.b.isConstant && oFlagValue){ //return constant value instead.
                        result = varState.b.value + typeToOllir(varState.b.type);
                        isConstant = true;
                        break;
                    }
                    if (!varState.b.isInitialized) { //not initialized
                        previousStatements.add(
                                varState.b.name + typeToOllir(varState.b.type)
                                        + " :=" + typeToOllir(varState.b.type)
                                        + " " + getTypeDefaultValue(varState.b.type) + ";\n"
                        );
                    }
                    result = varState.b.name + typeToOllir(varState.b.type);
                    resultType = varState.b.type;
                }
                break;
            }
            case "This":
                result = "this";
                resultType = new Type(semanticsResult.getSymbolTable().getClassName(),false);
                break;
            case "Bool":
                result = node.get("value")+".bool";
                resultType = new Type("boolean" , false);
                break;
        }
        if(result.equals("undefined"))
            result = "node:[" + node.getKind() + "]"; //throw new RuntimeException();
        return new ExpressionVisitResult(result,previousStatements, previousReleases,isConstant,resultType);
    }
    StatementVisitResult statementsVisitor(List<JmmNode> nodeList, JmmSemanticsResult semanticsResult, Map<String, VarState> localVarsState, String methodName){
        StringBuilder stringBuilder = new StringBuilder();
        ArrayList<String> previousStatements = new ArrayList<>();
        ArrayList<String> branchingConstants = new ArrayList<>();
        for(JmmNode child : nodeList){
            switch (child.getKind()) {
                case "VarDeclareStatement": {
                    var state = getVariableState(child.get("name"),localVarsState);
                    var expression = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState);
                    state.b.isConstant=false;
                    for (String s : expression.previousStatements)
                        stringBuilder.append(s);
                    for(VarState openVar : expression.freeVars)
                        releaseTemporaryVariable(openVar);
                    state.b.isInitialized = true;
                    if(expression.result.equals(state.b.name + typeToOllir(state.b.type)) && oFlagValue){
                        break; // if lhs == rhs, skip instruction.
                    }
                    if (state.a){
                        stringBuilder.append("putfield(this, " + state.b.name + typeToOllir(state.b.type) + ", " + expression.result + ").V;\n");
                        break;
                    }
                    if(expression.isConstant && oFlagValue){
                        state.b.isConstant = true;
                        state.b.isInitialized = false;
                        state.b.value = expression.result.substring(0,expression.result.lastIndexOf("."));
                        branchingConstants.add(state.b.name + typeToOllir(state.b.type) +" :=" + typeToOllir(state.b.type) + " " + state.b.value + typeToOllir(state.b.type) + ";\n");
                    }else{
                        stringBuilder.append(state.b.name).append(typeToOllir(state.b.type)).append(" :=").append(typeToOllir(state.b.type)).append(" ").append(expression.result).append(";\n");
                    }
                    //if(! (getOllirType(expression.a).getName().equals(state.b.getName()) && getOllirType(expression.a).isArray() == state.b.isArray()))
                    //throw new RuntimeException("Assigning mismatching type.");
                    break;
                }
                case "SemiColon": {
                    while(child.getJmmChild(0).getKind().equals("ParOp")) //ParOp inside SemiColon is to be ignored.
                        child=child.getJmmChild(0);
                    if(child.getJmmChild(0).getKind().matches("(NewFunc|FuncOp)")) { //only FuncOp is not a NoOp. TODO confirm newArr is not required.
                        var statements = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState);
                        for (String s : statements.previousStatements)
                            stringBuilder.append(s);
                        if (!statements.result.equals(""))
                            stringBuilder.append(statements.result).append(";\n");
                        for (VarState openVar : statements.freeVars)
                            releaseTemporaryVariable(openVar);
                    }
                    break;
                }
                case "Brackets": {
                    StatementVisitResult res = statementsVisitor(child.getChildren(), semanticsResult, localVarsState, methodName);
                    stringBuilder.append(res.result);
                    branchingConstants = res.branchingConstants;
                    previousStatements = res.previousStatements;
                    break;
                }
                case "ArrayAccess": {
                    var arrayVar = getVariableState(child.get("name"), localVarsState);
                    ExpressionVisitResult expIndex = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState),
                            expValue = expressionVisitor(child.getJmmChild(1), semanticsResult, localVarsState);
                    for (String s : expIndex.previousStatements)
                        stringBuilder.append(s);
                    for (String s : expValue.previousStatements)
                        stringBuilder.append(s);
                    String target;
                    if(child.getJmmChild(0).getKind().equals("Integer")){
                        var tmp = addTemporaryVariable(localVarsState,new Type("int",false));
                        expIndex.freeVars.add(tmp);
                        target = tmp.name + typeToOllir(tmp.type);
                        stringBuilder.append(target).append(" :=").append(typeToOllir(tmp.type)).append(" ").append(expIndex.result).append(";\n");
                    }else{
                        target = expIndex.result;
                    }
                    stringBuilder.append(arrayVar.b.name).append("[").append(target).append("]").append(typeToOllir(new Type(arrayVar.b.type.getName(), false)))
                            .append(" :=").append(typeToOllir(new Type(arrayVar.b.type.getName(),false))).append(" ")
                            .append(expValue.result).append(";\n");
                    for(VarState openVar : expIndex.freeVars)
                        releaseTemporaryVariable(openVar);
                    for(VarState openVar : expValue.freeVars)
                        releaseTemporaryVariable(openVar);
                    break;
                }
                case "IfElseStatement": {
                    var ifExpression = expressionVisitor(child.getJmmChild(0),semanticsResult,localVarsState);
                    if(ifExpression.isConstant && oFlagValue){
                        if(ifExpression.result.equals("true.bool")){ // skip branching and labels.
                            stringBuilder.append(statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))),semanticsResult,localVarsState,methodName).result);
                        }else{
                            stringBuilder.append(statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(2))),semanticsResult,localVarsState,methodName).result);
                        }
                        break;
                    }
                    for(String s: ifExpression.previousStatements)
                        stringBuilder.append(s);
                    for(VarState openVar : ifExpression.freeVars)
                        releaseTemporaryVariable(openVar);
                    int ifLabel = ifLabelCount;
                    ifLabelCount++;

                    ArrayList<Map<String,VarState>> branches = new ArrayList<>();
                    branches.add(duplicateVarsState(localVarsState));
                    branches.add(duplicateVarsState(localVarsState));
                    StatementVisitResult s1,s2;
                    while(true) {
                        s1 = statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))), semanticsResult, branches.get(0), methodName);
                        var check = checkValidState(localVarsState,branches.get(0));
                        if(check == null){
                            break;
                        }
                        branches.add(0,check.a);
                        for(String s:check.b) {
                            VarState varState = localVarsState.get(s);
                            stringBuilder.append(varState.name).append(typeToOllir(varState.type)).append(" :=").append(typeToOllir(varState.type)).append(" ").append(varState.value).append(typeToOllir(varState.type)).append(";\n");
                            varState.isInitialized = true;
                            varState.isConstant = false;
                        }
                    }
                    for(String s:s1.previousStatements)
                        stringBuilder.append(s);
                    while(true) {
                        s2 = statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(2))), semanticsResult, branches.get(1), methodName);
                        var check = checkValidState(localVarsState,branches.get(0));
                        if(check == null){
                            break;
                        }
                        branches.add(1,check.a);
                        for(String s:check.b) {
                            VarState varState = localVarsState.get(s);
                            stringBuilder.append(varState.name).append(typeToOllir(varState.type)).append(" :=").append(typeToOllir(varState.type)).append(" ").append(varState.value).append(typeToOllir(varState.type)).append(";\n");
                            varState.isInitialized = true;
                            varState.isConstant = false;
                        }
                    }
                    for(String s:s2.previousStatements)
                        stringBuilder.append(s);
                    stringBuilder.append("if(").append(ifExpression.result).append(") goto ifbody_").append(ifLabel).append(";\n");
                    stringBuilder.append(s2.result.toString());
                    for(String s:s2.branchingConstants)
                        stringBuilder.append(s);
                    stringBuilder.append("goto endif_").append(ifLabel).append(";\n");
                    stringBuilder.append("ifbody_").append(ifLabel).append(":\n");
                    stringBuilder.append(s1.result.toString());
                    for(String s:s1.branchingConstants)
                        stringBuilder.append(s);
                    stringBuilder.append("endif_").append(ifLabel).append(":\n"); //TODO append NoOp if last instruction in method.
                    mergeVarsState(localVarsState,branches);
                    break;
                }
                case "WhileStatement": {
                    Map<String,VarState> dupe = duplicateVarsState(localVarsState);
                    StringBuilder previousInitializers = new StringBuilder();
                    int whileLabel = whileLabelCount;
                    whileLabelCount++;
                    ExpressionVisitResult whileExpression;
                    StatementVisitResult statementsVisit;
                    while(true){
                        statementsVisit= statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))), semanticsResult, dupe, methodName);
                        var check = checkValidState(localVarsState, dupe);

                        if(check==null) {
                            whileExpression = expressionVisitor(child.getJmmChild(0),semanticsResult,dupe);
                            break;
                        }
                        dupe=check.a;
                        for(String s:check.b) {
                            VarState varState = localVarsState.get(s);
                            previousInitializers.append(varState.name).append(typeToOllir(varState.type)).append(" :=").append(typeToOllir(varState.type)).append(" ").append(varState.value).append(typeToOllir(varState.type)).append(";\n");
                            varState.isInitialized = true;
                            varState.isConstant = false;
                        }
                    }
                    mergeVarsState(localVarsState,new ArrayList<>(Collections.singleton(dupe)));

                    if(whileExpression.isConstant && oFlagValue){
                        if(whileExpression.result.equals("false.bool")) // skip whileblock
                            break;

                        StatementVisitResult visit;
                        /*while(true){
                            visit= statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))), semanticsResult, dupe, methodName);
                            var check = checkValidState(localVarsState, dupe);

                            if(check==null) {
                                break;
                            }
                            dupe=check.a;
                            for(String s:check.b) {
                                VarState varState = localVarsState.get(s);
                                previousInitializers.append(varState.name).append(typeToOllir(varState.type)).append(" :=").append(typeToOllir(varState.type)).append(" ").append(varState.value).append(typeToOllir(varState.type)).append(";\n");
                                varState.isInitialized = true;
                                varState.isConstant = false;
                            }
                        }*/
                        //for(String s: visit.previousStatements)
                         //   stringBuilder.append(s);
                        stringBuilder.append("whilebody_").append(whileLabel).append(":\n");
                        stringBuilder.append(statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))), semanticsResult, dupe, methodName).result);
                        stringBuilder.append("goto while_body_").append(whileLabel).append(";\n");
                        mergeVarsState(localVarsState,new ArrayList<>(Collections.singleton(dupe)));
                        break;
                    }
                    stringBuilder.append(previousInitializers);
                    stringBuilder.append("goto whilestart_").append(whileLabel).append(";\n")
                            .append("whilebody_").append(whileLabel).append(":\n");
                    stringBuilder.append(statementsVisit.result);
                    for(String s:statementsVisit.branchingConstants)
                        stringBuilder.append(s);
                    stringBuilder.append("whilestart_").append(whileLabel).append(":\n");
                    for(String s: whileExpression.previousStatements)
                        stringBuilder.append(s);
                    stringBuilder.append("if(").append(whileExpression.result).append(") goto whilebody_").append(whileLabel).append(";\n");
                    for(VarState openVar : whileExpression.freeVars)
                        releaseTemporaryVariable(openVar);
                    break;
                }

                    //catch all 'expression' types, as this means the node is a return statement.
                case "BinaryOp": case "IndexOp": case "LengthOp": case "FuncOp": case "NewArr":
                case "NewFunc": case "NegationOp": case "ParOp": case "Integer": case "Bool":
                case "Identifier": case "This": {
                    var expression = expressionVisitor(child, semanticsResult, localVarsState);
                    for (String s : expression.previousStatements)
                        stringBuilder.append(s);
                    var retType = typeToOllir(semanticsResult.getSymbolTable().getReturnType(methodName));
                    stringBuilder.append("ret").append(retType).append(" ").append(expression.result).append(";\n");
                    for(VarState openVar : expression.freeVars)
                        releaseTemporaryVariable(openVar);
                    break;
                }
            }
        }
        StatementVisitResult ret = new StatementVisitResult();
        ret.result = stringBuilder;
        ret.previousStatements = previousStatements;
        ret.branchingConstants = branchingConstants;
        return ret;
    }
    String methodVisitor(JmmNode methodNode, JmmSemanticsResult semanticsResult){
        String methodName = methodNode.get("name");
        Map<String, VarState> localVarsState = new HashMap<>();
        for(var aVar:semanticsResult.getSymbolTable().getLocalVariables(methodName))
            localVarsState.put(aVar.getName(), new VarState(false, aVar.getType(),aVar.getName(),false, false,false));
        for(int i=0; i<semanticsResult.getSymbolTable().getParameters(methodName).size() ;++i) {
            var aVar= semanticsResult.getSymbolTable().getParameters(methodName).get(i);
            localVarsState.put(aVar.getName(), new VarState(true, aVar.getType(), "$"+(i+1)+"."+aVar.getName(),false, false, false));
        }
        StatementVisitResult stmtVisit = statementsVisitor(methodNode.getChildren(),semanticsResult,localVarsState,methodName);
        StringBuilder stringBuilder = stmtVisit.result;
        if(stringBuilder.length()>2 && stringBuilder.charAt(stringBuilder.length()-2) == ':'){
            stringBuilder.append("ret.V;\n");
        }
        return stringBuilder.toString();
    }
    Map<String,VarState> duplicateVarsState(Map<String, VarState> localVarsState) {
        Map<String, VarState> ret = new HashMap<>();
        for (Map.Entry<String, VarState> p : localVarsState.entrySet()) {
            VarState dupe = new VarState(p.getValue().isInitialized,
                    new Type(p.getValue().type.getName(), p.getValue().type.isArray()),
                    p.getValue().name,
                    p.getValue().isOpen,
                    p.getValue().isConstant,
                    p.getValue().isTemporary,
                    p.getValue().value
            );
            ret.put(p.getKey(), dupe);
        }
        return ret;
    }
    Pair<Map<String,VarState>,ArrayList<String>> checkValidState(Map<String,VarState> origState, Map<String,VarState> branchState){
        var ret = duplicateVarsState(origState);
        var toInitialize = new ArrayList<String>();
        boolean isValid = true;
        for(Map.Entry<String,VarState> e : ret.entrySet()) {
            VarState oVar = e.getValue(), nVar = branchState.get(e.getKey());
            if(oVar.isOpen || oVar.isTemporary) continue;
            if(oVar.isConstant && (!nVar.isConstant || !nVar.value.equals(oVar.value))){
                oVar.isConstant = false;
                isValid = false;
                if(nVar.isRead)
                    toInitialize.add(e.getKey());
                e.setValue(new VarState(true,oVar.type,oVar.name,oVar.isOpen,false, oVar.isTemporary, oVar.value));
                origState.get(e.getKey()).isConstant = false;
            }
        }
        if(isValid)
            return null; // state was valid; Otherwise, repeat branch with returned map.
        else
            return new Pair<>(ret,toInitialize);
    }
    void mergeVarsState(Map<String,VarState> origState, List<Map<String,VarState>> branchStates){
        for(Map.Entry<String,VarState> e : origState.entrySet()){
            VarState oVar = e.getValue();
            if(oVar.isOpen || (oVar.isInitialized && !oVar.isConstant))
                continue;
            boolean isConstant = true,
                    isInitalized = true;
            String value = oVar.value;

            List<VarState> branchVars = new ArrayList<>(branchStates.size());
            for(var branch:branchStates)
                branchVars.add(branch.get(e.getKey()));
            for(var branched:branchVars){
                if(!isConstant && !isInitalized)
                    break;
                if(!branched.isInitialized){
                    isInitalized = false;
                }
                if(!branched.isConstant) {
                    isConstant = false;
                }
                else{
                    if(value==null) {
                        value = branched.value;
                    }else {
                        if (!value.equals(branched.value)){
                            isConstant = false;
                            break;
                        }
                    }
                }
            }
            e.setValue(new VarState(isInitalized,oVar.type,oVar.name,oVar.isOpen,isConstant,oVar.isTemporary,value));
        }
    }
    private class ExpressionVisitResult{
        //Quartet<String,ArrayList<String>,ArrayList<VarState>,Boolean>
        String result;
        ArrayList<String> previousStatements;
        ArrayList<VarState> freeVars;
        Boolean isConstant;

        Type type;



        ExpressionVisitResult(String p1, ArrayList<String> p2, ArrayList<VarState> p3, Boolean p4, Type p5){
            result = p1;
            previousStatements = p2;
            freeVars = p3;
            isConstant = p4;
            type = p5;
        }
    }
    private class StatementVisitResult{
        public StringBuilder result = null;
        public ArrayList<String> previousStatements = null;
        public ArrayList<String> branchingConstants = null;
    }
}
