package com.adaptavist.confluence.pageFamilyTagCloud;

import bucket.core.actions.PaginationSupport;
import com.adaptavist.plm.api.License;
import com.adaptavist.plm.api.LicensingState;
import com.adaptavist.plm.api.PluginLicenseService;
import com.adaptavist.plm.service.LicenseServiceTracker;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.labels.Label;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.search.actions.SearchBean;
import com.atlassian.confluence.search.actions.SearchQueryBean;
import com.atlassian.confluence.search.actions.SearchResultWithExcerpt;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import com.opensymphony.util.TextUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.*;

import static com.atlassian.confluence.core.ConfluenceActionSupport.getTextStatic;
import static com.atlassian.confluence.util.GeneralUtil.getGlobalSettings;


public class PageFamilyTagCloudMacro4 extends BaseMacro implements  Macro {

    public static final String MAX_LABELS_PARAM = "max";
    public static final String SORT_PARAM = "sort";

    public static final String LABEL_TEXT_SORT = "label";
    public static final String LABEL_COUNT_SORT = "count";

    public static final String ROOT_PAGE_PARAM = "rootPage";

    public static final String REVERSE_PARAM = "reverse";

    public static final String INCLUDE_ROOT_PARAM = "includeRootPage";

    public static final String LABEL_LINK_PARAM = "labelLink";

    private PageManager pageManager;
    protected LicenseServiceTracker licenseServiceTracker;
    protected UserAccessor userAccessor;

    private static final Logger log = Logger.getLogger(PageFamilyTagCloudMacro4.class);


    private int highestCount = 1;
    protected SettingsManager settingsManager;

    private void updateLabelCount(List labels, Map labelCount){

        for (Iterator iterator = labels.iterator(); iterator.hasNext();) {

            Label label = (Label) iterator.next();

            if(labelCount.containsKey(label)){
                // Already in the Map, update the count
                Integer count = (Integer)labelCount.get(label);
                count = new Integer(count.intValue() + 1);
                labelCount.put(label, count);

                if(count.intValue() > highestCount){
                    highestCount = count.intValue();
                }
            }else{
                // Not in the map, put label in with a count of one.
                labelCount.put(label, new Integer(1));
            }
        }
    }

    public void setUserAccessor(UserAccessor userAccessor) {
        this.userAccessor = userAccessor;
    }

    public void setLicenseServiceTracker(LicenseServiceTracker licenseServiceTracker) {
        this.licenseServiceTracker = licenseServiceTracker;
    }

    protected boolean isLicensed() {
        return  licenseServiceTracker.isLicensed();
    }

    private String getMessage() {
        if (!licenseServiceTracker.isServicePresent()) {
            return getTextStatic("license.noplm");
        }
        if (((PluginLicenseService) licenseServiceTracker.getPluginLicenseService()).getPluginLicensing().getLicenses().isEmpty()) {
            return getTextStatic("license.no-license", new String[]{getGlobalSettings().getBaseUrl()});
        }
        return getTextStatic("license.expired", new String[]{getGlobalSettings().getBaseUrl()});
    }

    protected String unlicensed() throws MacroException {
        if (userAccessor.isSuperUser(AuthenticatedUserThreadLocal.getUser()) ) {
            return getMessage();
        } else {
            return "";
        }
    }

    public String execute(Map parameters, String body, RenderContext renderContext)	throws MacroException {
        if (!isLicensed()) {
            log.warn("Page Family Tag Cloud Plugin is unlicensed");
            return unlicensed();
        }

        String rootPageId = "";
        HashMap labelCountMap = new HashMap();

        String sort = getSortParm(parameters);
        boolean includeRootPage = MapUtils.getBooleanValue(parameters, INCLUDE_ROOT_PARAM, true);
        String labelLink = MapUtils.getString(parameters, LABEL_LINK_PARAM);
        int maxLabels = getMaxLabelsParam(parameters);

        // Sort alpha only for now

        String rootPageName = MapUtils.getString(parameters, ROOT_PAGE_PARAM);
        String pageTitle = null;
        String spaceKey = null;

        if(StringUtils.isNotEmpty(rootPageName)){
            // Try to split out the space key if present
            if(rootPageName.contains(":")){
                String[] pageIdentifiers = rootPageName.split("\\:");
                if(pageIdentifiers.length == 2){
                    spaceKey = pageIdentifiers[0];
                    pageTitle = pageIdentifiers[1];
                }
            }else{
                pageTitle = rootPageName;
            }
        }

        // If the page or space key was't specified get them from the current
        // page render context
        if(renderContext instanceof PageContext){
            PageContext pageContext = (PageContext)renderContext;
            if(StringUtils.isEmpty(pageTitle)){
                pageTitle = pageContext.getPageTitle();
            }
            if(StringUtils.isEmpty(spaceKey)){
                spaceKey = pageContext.getSpaceKey();
            }
        }

        Page rootPage = pageManager.getPage(spaceKey, pageTitle);
        if(rootPage != null){
            rootPageId = rootPage.getIdAsString();
        }else{
            throw new MacroException(ConfluenceActionSupport.getTextStatic("pageFamilyTagCloud.page.not.found", new String[]{pageTitle, spaceKey}));
        }


        User localUser = AuthenticatedUserThreadLocal.getUser();

        if(log.isDebugEnabled()){ log.debug("building tag cloud for parent page: " + rootPageId); }

        StringBuffer queryString = new StringBuffer();
        queryString.append("ancestorIds:(" + rootPageId + ") ");
        queryString.append(" AND ");
        queryString.append("type:(page)");
        if(!includeRootPage){
            queryString.append("AND NOT handle:com.atlassian.confluence.pages.Page-" + rootPageId );
        }

        SearchQueryBean searchQuery = new SearchQueryBean();
        ContainerManager.autowireComponent( searchQuery );

        try {
            searchQuery.setQueryString(queryString.toString());
            Query query = searchQuery.buildQuery();

            SearchBean searchBean = new SearchBean();
            ContainerManager.autowireComponent( searchBean );
            searchBean.setPaginationSupport(new PaginationSupport(100));


            if(log.isDebugEnabled()){ log.debug("search query is: " + searchQuery.toString()); }

            List results = searchBean.search(query);

            for (Iterator iterator = results.iterator(); iterator.hasNext();) {
                Object object = (Object) iterator.next();

                if (object instanceof SearchResultWithExcerpt) {
                    SearchResultWithExcerpt result = (SearchResultWithExcerpt) object;
                    if (result.getResultObject() instanceof Page) {

                        Page page = (Page)result.getResultObject();

                        updateLabelCount(page.getLabelsForDisplay(localUser), labelCountMap);

                    }
                }
            }

        } catch (IOException e) {
            throw new MacroException(e);
        }


        // Sort the labelCount objects by the label count, throwing away any
        // that are too small (as specified by the max param)
        NavigableSet orderedLabelCounts = new TreeSet(LabelCount.COUNT_COMPARATOR);
        boolean customLabelLink = TextUtils.stringSet(labelLink);
        for (Iterator iterator = labelCountMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();

            LabelCount labelCount = new LabelCount((Label)mapEntry.getKey(), ((Integer)mapEntry.getValue()).intValue());
            if(customLabelLink){
                labelCount.setLink(GeneralUtil.htmlEncode(labelLink.replaceAll("%label%", labelCount.getLabel().getDisplayTitle())));
            }


            orderedLabelCounts.add(labelCount);

            // Make sure the set doesn't get bigger then the max
            if(orderedLabelCounts.size() > maxLabels){
                orderedLabelCounts.remove(orderedLabelCounts.first());
            }
        }

        // If the user wants to sort by the label text rather then the count do that here
        if(LABEL_TEXT_SORT.equalsIgnoreCase(sort)){
            TreeSet alphaOrderedLabelCounts = new TreeSet(LabelCount.LABEL_COMPARATOR);
            alphaOrderedLabelCounts.addAll(orderedLabelCounts);
            orderedLabelCounts = alphaOrderedLabelCounts;
        }

        boolean reverse = MapUtils.getBooleanValue(parameters, REVERSE_PARAM, false);
        if(reverse){
            orderedLabelCounts = orderedLabelCounts.descendingSet();
        }


        Map velocityContext = MacroUtils.defaultVelocityContext();
        velocityContext.put("labelCounts", orderedLabelCounts);
        velocityContext.put("highestCount", new Integer(highestCount));


        return VelocityUtils.getRenderedTemplate("/templates/pageFamilyTagCloud.vm", velocityContext);

    }

    public RenderMode getBodyRenderMode() {
        return RenderMode.NO_RENDER;
    }

    public boolean hasBody() {
        return false;
    }

    public boolean isInline() {
        return false;
    }

    public boolean suppressMacroRenderingDuringWysiwyg() {
        return false;
    }

    public boolean suppressSurroundingTagDuringWysiwygRendering() {
        return false;
    }

    public void setPageManager(PageManager pageManager) {
        this.pageManager = pageManager;
    }


    /**
     * Helper method to validate the max labels parameter.
     *
     * @param parameters
     *            the map of macro parameters
     * @return the int value for the max labels param, or the default 25 if not
     *         specified.
     * @throws com.atlassian.renderer.v2.macro.MacroException
     *             if the input param is not a valid integer or is less then
     *             zero.
     */
    protected int getMaxLabelsParam(Map parameters) throws MacroException{

        String maxLabelsString = MapUtils.getString(parameters, MAX_LABELS_PARAM, "25");
        int maxLabels = 25;
        try{
            maxLabels = Integer.parseInt(maxLabelsString);
        }catch(NumberFormatException nfe){
            throw new MacroException(ConfluenceActionSupport.getTextStatic("pageFamilyTagCloud.maxLabelsParam.invalid", new String[]{maxLabelsString}));
        }
        if(maxLabels < 0){
            throw new MacroException(ConfluenceActionSupport.getTextStatic("pageFamilyTagCloud.maxLabelsParam.invalid", new String[]{maxLabelsString}));
        }

        return maxLabels;
    }

    /**
     * Helper method to validate the sort parameter
     *
     * @param parameters
     *            the map of macro parameters
     * @return the sort parameters string, if the parameter is not specified the
     *         default value {@link #LABEL_TEXT_SORT} will be returned.
     * @throws com.atlassian.renderer.v2.macro.MacroException
     *             if the input param is not a valid sort
     */
    protected String getSortParm(Map parameters) throws MacroException{
        String sort = MapUtils.getString(parameters, SORT_PARAM, LABEL_TEXT_SORT);
        if(! (sort.equals(LABEL_TEXT_SORT) || sort.equals(LABEL_COUNT_SORT))){
            throw new MacroException(ConfluenceActionSupport.getTextStatic("pageFamilyTagCloud.sortParam.invalid"));
        }
        return sort;
    }



    public String execute(Map<String, String> params, String body, ConversionContext conversionContext) throws MacroExecutionException {
        
        try{
            if (!(conversionContext instanceof DefaultConversionContext)){
                return "";
            }
            DefaultConversionContext defaultConversionContext = (DefaultConversionContext) conversionContext;
            RenderContext renderContext = defaultConversionContext.getRenderContext();

            return execute(params, body, renderContext);
        } catch (MacroException e){
            throw new MacroExecutionException(e);
        }
    }

    public void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

}
