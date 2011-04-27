package it.com.adaptavist.confluence.pageFamilyTagCloud;

import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;
import com.atlassian.confluence.plugin.functest.JWebUnitConfluenceWebTester;
import com.atlassian.confluence.plugin.functest.TesterConfiguration;
import com.atlassian.confluence.plugin.functest.helper.IndexHelper;
import com.atlassian.confluence.plugin.functest.helper.PageHelper;
import com.atlassian.confluence.plugin.functest.helper.SpaceHelper;
import junit.framework.Assert;
import net.sourceforge.jwebunit.exception.TestingEngineResponseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractPageFamilyTagCloudTestCase extends AbstractConfluencePluginWebTestCase {

    protected static final String TESTSPACE = "TESTSPACE";

    protected static final String HEATMAP_XPATH = "//div[@class='heatmap pagefamilytagcloud']";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        SpaceHelper spaceHelper = getSpaceHelper();
        spaceHelper.setKey(TESTSPACE);
        spaceHelper.setName("TEST SPACE NAME");
        spaceHelper.setDescription("This is the description of the space");
        assertTrue("Error creating test space.",spaceHelper.create());

    }

    @Override
    public void tearDown() throws Exception {
        SpaceHelper spaceHelper = getSpaceHelper();
        spaceHelper.setKey(TESTSPACE);
        assertTrue("Error deleting " + TESTSPACE, spaceHelper.delete());
        super.tearDown();
    }

    protected JWebUnitConfluenceWebTester createConfluenceWebTester()
    {
        try
        {
            return new JWebUnitConfluenceWebTester(new TesterConfiguration()) {
               public boolean gotoPageWithEscalatedPrivileges(String destination, String urlEncoding)
                {
                    try {
                        return super.gotoPageWithEscalatedPrivileges(destination, urlEncoding);
                    } catch(TestingEngineResponseException e) {
                        // no websudo
                        gotoPage(destination);
                        return true;
                    }
                }
            };
        }
        catch (IOException ioe)
        {
            Assert.fail("Unable to create tester: " + ioe.getMessage());
            return null;
        }
    }

    protected long createPage(String spaceKey, String title, String content, long parentId, List<String> labels) {
        PageHelper helper = getPageHelper();

        helper.setSpaceKey(spaceKey);
        helper.setTitle(title);
        helper.setContent(content);
        if(labels != null)
            helper.setLabels(labels);
        if(parentId > 0)
            helper.setParentId(parentId);
        assertTrue(helper.create());

        // return the generated id for the new page
        return helper.getId();
    }

    protected void createTestData(String macro) {
        long parentId = createPage(TESTSPACE,"Root Page",macro,0, Arrays.asList("rootlabel"));
        createPage(TESTSPACE,"Child Page 1","Test Page",parentId, Arrays.asList("biglabel"));
        createPage(TESTSPACE,"Child Page 2","Test Page",parentId,Arrays.asList("biglabel","smalllabel"));
        createPage(TESTSPACE,"Child Page 3","Test Page",parentId,Arrays.asList("biglabel"));
        createPage(TESTSPACE,"Child Page 4","Test Page",parentId,Arrays.asList("biglabel"));
        createPage(TESTSPACE,"Child Page 5","Test Page",parentId,Arrays.asList("biglabel"));
        createPage(TESTSPACE,"Child Page 6","Test Page",parentId,Arrays.asList("xlabel"));
        createPage(TESTSPACE,"Child Page 7","Test Page",parentId,Arrays.asList("xlabel"));
        createPage(TESTSPACE,"Child Page 8","Test Page",parentId,Arrays.asList("xlabel"));

        IndexHelper indexHelper = getIndexHelper();
        indexHelper.update();

        gotoPage("display/" + TESTSPACE + "/Root+Page");
    }

    protected void assertDefaultTestDataLabels() {
        String bigLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[1]");
        String rootLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[2]");
        String smallLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[3]");
        String xLabel = getElementTextByXPath(HEATMAP_XPATH + "/ul/li[4]");

        assertEquals("biglabel",bigLabel);
        assertEquals("smalllabel",smallLabel);
        assertEquals("rootlabel",rootLabel);
        assertEquals("xlabel", xLabel);
    }
}
