package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.visitors.ClassesVisitor;
import pt.up.fe.comp2023.visitors.ImportsVisitor;
import pt.up.fe.comp2023.visitors.MethodsVisitor;
import pt.up.fe.comp2023.visitors.ProgramVisitor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTableStore implements SymbolTable {
    public class StringReference{
        public String string;
        public StringReference(String string){
            this.string=string;
        }
        public String toString(){
            return string;
        }
        public void setString(String string) {
            this.string = string;
        }
    }
    private List<String> imports = new ArrayList<>();

    private StringReference className = new StringReference("");
    private StringReference superName = new StringReference("");
    private List<Symbol> fields = new ArrayList<>();
    private Map<String, Triple<Type,List<Symbol>,List<Symbol>>> methods_parameters = new HashMap<>();



    public SymbolTableStore(JmmParserResult parserResult) {
        Map<String,Object> programInfo = new HashMap<>();
        programInfo.put("imports", imports);
        programInfo.put("className", className);
        programInfo.put("superName", superName);
        programInfo.put("fields", fields);
        programInfo.put("methods_parameters", methods_parameters);
        ProgramVisitor programVisitor = new ProgramVisitor();
        programVisitor.visit(parserResult.getRootNode(), programInfo);
    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className.string;
    }

    @Override
    public String getSuper() {
        return superName.string;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(methods_parameters.keySet());
    }

    @Override
    public Type getReturnType(String methodName) {
        return methods_parameters.get(methodName).a;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return methods_parameters.get(methodName).b;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return methods_parameters.get(methodName).c;
    }

    public void printImports(){

        for (int i = 0; i<this.imports.size();i++){
            System.out.println(this.imports.get(i));
        }
    }
}
