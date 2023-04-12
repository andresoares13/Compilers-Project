package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class MyJmmOptimization implements JmmOptimization {
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
            codeBuilder.append("\t.field public "+v.getName()+toOllirType(v.getType()) + ";\n");
        }
                codeBuilder.append("\t.construct "+st.getClassName()+"().V {\n" +
                        "\t\tinvokespecial(this, \"<init>\").V;\n" +
                    "\t}\n\n");
        for(String methodName :st.getMethods()){
            codeBuilder.append(
                    "\t.method ");
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
            codeBuilder.append(methodVisitor(methodNode,jmmSemanticsResult, "\t"));

            codeBuilder.append("\t}\n");
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
    JmmNode getMethodNode(JmmSemanticsResult semanticsResult, String method ){
        JmmNode root= semanticsResult.getRootNode();
        for(JmmNode rootChild : root.getChildren()){
            if(rootChild.getKind().equals("ClassDeclare")) {
                for(JmmNode classNode : rootChild.getChildren()) {
                    if(classNode.getKind().startsWith("MethodDeclare")){
                        if(classNode.getJmmChild(0).get("name").equals(method))
                            return classNode;
                    }
                }
            }
        }
        return null;
    }
    String methodVisitor(JmmNode methodNode, JmmSemanticsResult semanticsResult, String indentation ){
        for(JmmNode child : methodNode.getChildren()) {

        }
    }
}
