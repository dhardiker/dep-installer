package it.com.adaptavist.confluence.pageFamilyTagCloud;

public class PageFamilyTagCloudTest extends AbstractPageFamilyTagCloudTestCase {

    public void test_Basic_Macro_Usage() {
        createTestData("{pagefamily-tagcloud}");
        assertDefaultTestDataLabels();
    }

    public void test_Include_Root_Page_Labels() {
        createTestData("{pagefamily-tagcloud:includeRootPage=true}");
        assertDefaultTestDataLabels();
    }

    public void test_Include_False_Root_Page_Labels() {
        createTestData("{pagefamily-tagcloud:includeRootPage=false}");

        String bigLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[1]");
        String smallLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[2]");
        String xLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[3]");

        assertEquals("biglabel",bigLabel);
        assertEquals("smalllabel",smallLabel);
        assertEquals("xlabel", xLabel);
        
        assertElementNotPresentByXPath(HEATMAP_XPATH + "/ul/li[4]");
    }

    public void test_Invalid_Sort_By_Label() {
        createTestData("{pagefamily-tagcloud:sort=madeup}");

        assertTextPresent("Invalid label sort param");
        assertElementNotPresentByXPath(HEATMAP_XPATH);
    }

    public void test_Sort_By_Label() {
        createTestData("{pagefamily-tagcloud:sort=label}");
        
        assertDefaultTestDataLabels();
    }

    public void test_Sort_By_Count() {
        createTestData("{pagefamily-tagcloud:sort=count}");
        
        String rootLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[1]");
        String smallLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[2]");
        String xLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[3]");
        String bigLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[4]");

        assertEquals("rootlabel",rootLabel);
        assertEquals("biglabel",bigLabel);
        assertEquals("smalllabel",smallLabel);
        assertEquals("xlabel",xLabel);
    }

    public void test_Reverse_Sort_By_Label() {
        createTestData("{pagefamily-tagcloud:sort=label|reverse=true}");

        String bigLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[4]");
        String rootLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[3]");
        String smallLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[2]");
        String xLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[1]");

        assertEquals("rootlabel",rootLabel);
        assertEquals("biglabel",bigLabel);
        assertEquals("smalllabel",smallLabel);
        assertEquals("xlabel",xLabel);
    }

    public void test_Reverse_Sort_By_Count() {
        createTestData("{pagefamily-tagcloud:sort=count|reverse=true}");
        
        String rootLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[4]");
        String smallLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[3]");
        String xLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[2]");
        String bigLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[1]");

        assertEquals("rootlabel",rootLabel);
        assertEquals("biglabel",bigLabel);
        assertEquals("smalllabel",smallLabel);
        assertEquals("xlabel",xLabel);
    }

    public void test_Max_Labels() {
        createTestData("{pagefamily-tagcloud:max=1}");
        
        String bigLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[1]");

        assertEquals("biglabel",bigLabel);
        assertTextNotPresent("smalllabel");
        assertTextNotPresent("xlabel");
        assertElementNotPresentByXPath(HEATMAP_XPATH + "/ul/li[2]");
    }

    public void test_Invalid_Max_Labels() {
        createTestData("{pagefamily-tagcloud:max=-1}");
        
        assertTextPresent("The max labels param is invalid");
        assertElementNotPresentByXPath(HEATMAP_XPATH);
    }

    public void test_Not_Number_Max_Labels() {
        createTestData("{pagefamily-tagcloud:max=five}");

        assertTextPresent("The max labels param is invalid");
        assertElementNotPresentByXPath(HEATMAP_XPATH);
        
    }

    public void test_LabelLink() {
        createTestData("{pagefamily-tagcloud:labelLink=/test-label/%label% }");
        
        String labelUrl = getElementAttributByXPath(HEATMAP_XPATH + "/ul/li[1]/a","href");
        assertEquals("/test-label/biglabel",labelUrl);
    }
    
    public void test_LabelLink_XSS() {
        createTestData("{pagefamily-tagcloud:labelLink=\">Uh Oh/test-label/%label% }");
        
        assertTextNotPresent("Uh Oh");
    }

    public void test_Root_Page() {
        createTestData("{pagefamily-tagcloud:rootPage=Child Page 7}");

        String xLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[1]");
        
        assertEquals("xlabel",xLabel);
        assertTextNotPresent("smalllabel");
        assertTextNotPresent("biglabel");
    }

    public void test_Invalid_Root_Page() {
        createTestData("{pagefamily-tagcloud:rootPage=Does Not Exist}");
        assertTextPresent("Could not find a page called: Does Not Exist in space");
    }

}
