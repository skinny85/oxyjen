import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.IBiblioResolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class EmbeddedIvy {
    public static File resolveArtifact(String groupId, String artifactId, String version) throws IOException, ParseException {
        //creates clear ivy settings
        IvySettings ivySettings = new IvySettings();

        //url resolver for configuration of maven repo
//        URLResolver resolver = new URLResolver();
//        resolver.setM2compatible(true);
//        resolver.setName("central");
//        //you can specify the url resolution pattern strategy
//        resolver.addArtifactPattern(
//                "http://repo1.maven.org/maven2/[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]");

        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setM2compatible(true);
        resolver.setName("oxyjen");
        resolver.setRoot("http://localhost:8081/artifactory/oxyjen");

        //adding maven repo resolver
        ivySettings.addResolver(resolver);
        //set to the default resolver
        ivySettings.setDefaultResolver(resolver.getName());

        DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(
                ModuleRevisionId.newInstance(
                        groupId, artifactId + "-caller", "working"));

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                md,
                ModuleRevisionId.newInstance(groupId, artifactId, version),
                false, false, true);
        dd.addDependencyConfiguration("default", "default");

        md.addDependency(dd);

        ResolveOptions resolveOptions = new ResolveOptions()
                .setConfs(new String[]{"default"});

        //creates an Ivy instance with settings
        Ivy ivy = Ivy.newInstance(ivySettings);
        //init resolve report
        ResolveReport report = ivy.resolve(md, resolveOptions);

        //so you can get the jar library

        return report.getAllArtifactsReports()[0].getLocalFile();
    }

    public static void main(String[] args) throws IOException, ParseException {
        File jarArtifactFile = resolveArtifact("log4j", "log4j", "1.2.16");

        System.out.println("jarArtifactFile = " + jarArtifactFile.getAbsolutePath());
    }
}
