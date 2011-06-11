package com.adaptavist.amps;

import com.atlassian.plugin.*;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.BundleContextAware;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class DependencyInstaller implements LifecycleAware, BundleContextAware {

    private static final Logger log = LoggerFactory.getLogger(DependencyInstaller.class);

    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;
    private BundleContext bundleContext;

    public DependencyInstaller(PluginAccessor pluginAccessor, PluginController pluginController) {
        this.pluginAccessor = pluginAccessor;
        this.pluginController = pluginController;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void onStart() {
        try {
            for (File eachPluginFile : getPluginFiles()) {
                installOrEnable(eachPluginFile);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<File> getPluginFiles() throws IOException {
        List<File> pluginFiles = new ArrayList<File>();

        File bundleFile = new File(URI.create(bundleContext.getBundle().getLocation()));
        if (bundleFile.isFile()) {
            JarFile bundleJar = new JarFile(bundleFile);
            Enumeration<JarEntry> entries = bundleJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();

                if (jarEntry.getName().startsWith("META-INF"+ File.separator +"dependencies") && jarEntry.getName().endsWith(".jar")) {
                    File plugin = File.createTempFile("plugin-dependency", ".jar");
                    IOUtils.copyLarge(bundleJar.getInputStream(jarEntry), new FileOutputStream(plugin));
                    pluginFiles.add(plugin);
                }
            }
        } else if (bundleFile.isDirectory()) {
            File file = new File(new File(bundleFile, "META-INF"), "dependencies");
            if (file.isDirectory()) {
                File[] pluginFileArray = file.listFiles( new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                } );
                pluginFiles.addAll( Arrays.asList( pluginFileArray ) );
            }
        }

        return pluginFiles;
    }

    private void installOrEnable(File eachPluginFile) throws IOException {
        try {
            String pluginKey = getPluginKeyFromJar( eachPluginFile );
            Plugin plugin = pluginAccessor.getPlugin(pluginKey);
            if (plugin == null) {
                PluginArtifact pluginArtifact = new JarPluginArtifact(eachPluginFile);
                pluginController.installPlugins(pluginArtifact);

                // NOTE: This would be a bit more accurate, but we know that all plugin artifacts will be Jars
                // pluginController.installPlugins(new DefaultPluginArtifactFactory().create(eachPluginFile.toURI()));
            } else if (!pluginAccessor.isPluginEnabled(pluginKey)) {
                pluginController.enablePlugin(pluginKey);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private String getPluginKeyFromJar(File pluginFile) throws IOException {
        JarFile pluginJarFile = new JarFile(pluginFile);
        ZipEntry entry = pluginJarFile.getEntry("atlassian-plugin.xml");
        if (entry == null) throw new RuntimeException(pluginFile +" is not an Atlassian Plugin file! No atlassian-plugin.xml file found.");

        InputStream apxInputStream = pluginJarFile.getInputStream(entry);
        try {
            SAXReader reader = new SAXReader();
            Document apxDoc = reader.read(apxInputStream);
            apxInputStream.close();

            return apxDoc.selectSingleNode("//atlassian-plugin/@key").getText();
        } catch (DocumentException e) {
            throw new RuntimeException("Unable to parse the key out of the atlassian-plugin.xml file in "+ pluginFile, e);
        }
    }

}
