package pt.up.fe.comp;

import org.junit.Test;
import org.specs.comp.ollir.ArrayType;
import org.specs.comp.ollir.ClassType;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Type;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MyOllirTest {
    static OllirResult getOllirResult(String filename) {
        return new OllirResult(SpecsIo.getResource("pt/up/fe/comp/ollir/" + filename), new HashMap<>());
    }
    static OllirResult getMyOllirResult(String filename){
        JmmOptimization optimization= TestUtils.getJmmOptimization();
        return optimization.toOllir(TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/ollir/"+filename)));
    }

    @Test
    public void testFac() {
        OllirResult result = getMyOllirResult("Fac.input"),
            expected = getOllirResult("Fac.ollir");
        assertEquals(result.getOllirClass(), expected.getOllirClass());
        assertEquals(result.getOllirCode(), expected.getOllirCode());

    }

    @Test
    public void testClassType() {
        var ollirClass = getOllirResult("Fac.ollir")
                .getOllirClass();

        var mainMethod = ollirClass.getMethod(2);
        for (var elem : mainMethod.getParams()) {
            System.out.println("ELEM: " + getType(elem.getType()));

        }

    }

    public String getType(Type type) {
        if (type.getTypeOfElement() == ElementType.OBJECTREF) {
            var classType = (ClassType) type;
            return classType.getName();
        }

        if (type.getTypeOfElement() == ElementType.ARRAYREF) {
            var arrayType = (ArrayType) type;
            System.out.println("TYPE OF ELEMENT: " + arrayType.getTypeOfElement());
            System.out.println("TYPE OF ELEMENTS: " + arrayType.getTypeOfElements());
            return arrayType.getTypeOfElements().toString();
        }

        System.out.println("Not yet implemented: " + type.getTypeOfElement());
        return type.toString();
    }

    @Test
    public void testMyclass1() {
        var result = getOllirResult("myclass1.ollir");

        // result.getOllirClass().get

        var methodName = getOllirResult("myclass1.ollir")
                .getOllirClass().getMethod(1).getMethodName();
        assertEquals("sum", methodName);
    }

    @Test
    public void testMyclass2() {
        var className = getOllirResult("myclass2.ollir")
                .getOllirClass().getClassName();
        assertEquals("myClass", className);
    }

    @Test
    public void testMyclass3() {
        var className = getOllirResult("myclass3.ollir")
                .getOllirClass().getClassName();
        assertEquals("myClass", className);
    }

    @Test
    public void testMyclass4() {
        var className = getOllirResult("myclass4.ollir")
                .getOllirClass().getClassName();
        assertEquals("myClass", className);
    }

    @Test
    public void testIfs() {
        var className = getOllirResult("ifs.ollir")
                .getOllirClass().getClassName();
        assertEquals("myClass", className);
    }

    @Test
    public void failCallInIf() {
        try {
            getOllirResult("fail_call_in_if.ollir");
            fail();
        } catch (Exception e) {
            // Good
            assertEquals(
                    "Error in line 10: Found invalid expression in 'if' condition of type CallInstruction, only accepts SingleOpInstruction or OpInstruction",
                    e.getCause().getMessage());
        }
    }

    @Test
    public void failIntInIf() {
        try {
            getOllirResult("fail_int_in_if.ollir");
            fail();
        } catch (Exception e) {
            // Good
            assertEquals(
                    "Error in line 10: found SingleOpInstruction in 'if' condition that is not a BOOLEAN, is INT32 instead",
                    e.getCause().getMessage());
        }
    }

    @Test
    public void failNotBoolOpInIf() {
        try {
            getOllirResult("fail_not_bool_op_in_if.ollir");
            fail();
        } catch (Exception e) {
            // Good
            assertEquals(
                    "Error in line 10: found OpInstruction in 'if' condition that is not a BOOLEAN, is INT32 instead",
                    e.getCause().getMessage());
        }
    }

    @Test
    public void failBoolLiteral() {
        try {
            getOllirResult("fail_bool_literal.ollir");
            fail();
        } catch (Exception e) {
            // Good
            assertEquals(
                    "Invalid value '2' for a boolean literal, can only be 0 or 1",
                    e.getCause().getMessage());
        }
    }

    @Test
    public void testClassArray() {
        var method = getOllirResult("classArray.ollir")
                .getOllirClass().getMethod(1);
        assertEquals("classArray", ((ArrayType) method.getParam(0).getType()).getElementClass());
    }
}
