package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.backend.JmmBackend;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;
import utils.ProjectTestUtils;

import java.util.Collections;

public class JasminTest {

    @Test
    public void ollirToJasminBasic() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminBasic.ollir");
    }

    @Test
    public void ollirToJasminArithmetics() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminArithmetics.ollir");
    }

    @Test
    public void ollirToJasminInvoke() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminInvoke.ollir");
    }

    @Test
    public void ollirToJasminFields() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminFields.ollir");
    }

    @Test
    public void ollirToJasminSimple() {
        testOllirToJasmin("pt/up/fe/comp/cp2/apps/example_ollir/Simple.ollir");
    }

    @Test
    public void ollirToJasminArray() {
        testOllirToJasmin("pt/up/fe/comp/cp2/apps/example_ollir/Array.ollir");
    }

    public static void testOllirToJasmin(String resource, String expectedOutput) {
        SpecsCheck.checkArgument(resource.endsWith(".ollir"), () -> "Expected resource to end with .ollir: " + resource);

        // If AstToJasmin pipeline, change name of the resource and execute other test
        if (TestUtils.hasAstToJasminClass()) {

            // Rename resource
            var jmmResource = SpecsIo.removeExtension(resource) + ".jmm";

            // Test Jmm resource
            var result = TestUtils.backend(SpecsIo.getResource(jmmResource));
            ProjectTestUtils.runJasmin(result, expectedOutput);

            return;
        }

        var ollirResult = new OllirResult(SpecsIo.getResource(resource), Collections.emptyMap());

        //var result = TestUtils.backend(ollirResult);

        JmmBackend backend = new JmmBackend();
        var my_result = backend.toJasmin(ollirResult);

        ProjectTestUtils.runJasmin(my_result, expectedOutput);
    }

    public static void testOllirToJasmin(String resource) {
        testOllirToJasmin(resource, null);
    }
}
