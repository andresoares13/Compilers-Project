package pt.up.fe.comp2023.backend;

import org.specs.comp.ollir.ArrayType;
import org.specs.comp.ollir.ClassType;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Type;

public class JasminUtils {

    public static String translateType(ClassUnit ollirClass, Type type) {
        ElementType elementType = type.getTypeOfElement();

        switch (elementType) {
            case ARRAYREF:
                return "[" + translateType(((ArrayType) type).getArrayType());
            case OBJECTREF:
            case CLASS:
                return "L" + getFullClassName(ollirClass, ((ClassType) type).getName()) + ";";
            default:
                return translateType(elementType);
        }
    }

    public static String translateType(ElementType type){
        switch (type){
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case THIS:
                return "this";
            case VOID:
                return "V";
            default:
                return "";
        }
    }

    public static String getFullClassName(ClassUnit ollirClass, String className) {
        if (ollirClass.isImportedClass(className)) {
            for (String fullImport : ollirClass.getImports()) {
                int lastSeparatorIndex = className.lastIndexOf(".");

                if (lastSeparatorIndex < 0 && fullImport.equals(className)) {
                    return className;
                } else if (fullImport.substring(lastSeparatorIndex + 1).equals(className)) {
                    return fullImport;
                }
            }
        }

        return className;
    }
}
