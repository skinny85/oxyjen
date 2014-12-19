
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class EmbeddedIvy {
    public static File resolveArtifact(String groupId, String artifactId, String version) throws IOException, ParseException {
        //creates clear ivy settings
        IvySettings ivySettings = new IvySettings();
        //url resolver for configuration of maven repo
        URLResolver resolver = new URLResolver();
        resolver.setM2compatible(true);
        resolver.setName("central");
        //you can specify the url resolution pattern strategy
//        resolver.addArtifactPattern(
//                "http://repo1.maven.org/maven2/[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]");
        //adding maven repo resolver
        ivySettings.addResolver(resolver);
        //set to the default resolver
        ivySettings.setDefaultResolver(resolver.getName());
        //creates an Ivy instance with settings
        Ivy ivy = Ivy.newInstance(ivySettings);

        File ivyfile = File.createTempFile("ivy", ".xml");
        ivyfile.deleteOnExit();

        String[] dep = new String[]{groupId, artifactId, version};

        DefaultModuleDescriptor md =
                DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep[0],
                        dep[1] + "-caller", "working"));

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
                ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
        md.addDependency(dd);

        //creates an ivy configuration file
        XmlModuleDescriptorWriter.write(md, ivyfile);

        String[] confs = new String[]{"default"};
        ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs);

        //init resolve report
        ResolveReport report = ivy.resolve(ivyfile.toURL(), resolveOptions);

        //so you can get the jar library
        File jarArtifactFile = report.getAllArtifactsReports()[0].getLocalFile();

        return jarArtifactFile;
    }

    public static void main(String[] args) throws IOException, ParseException {
        File jarArtifactFile = resolveArtifact("log4j", "log4j", "1.2.16");
        System.out.println("jarArtifactFile = " + jarArtifactFile.getAbsolutePath());
    }
}
