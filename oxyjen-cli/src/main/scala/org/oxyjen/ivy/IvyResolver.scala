package org.oxyjen.ivy

import java.io.File

import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.{DefaultDependencyDescriptor, DefaultModuleDescriptor}
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.util.{DefaultMessageLogger, Message}

object IvyResolver {
  private val RESOLVER_NAME = "oxyjen"
  private val (ivySettings, resolveOptions) = init()

  def resolve(groupId: String, artifactId: String, version: String): Option[File] = {
    val tmpModule = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance(groupId, artifactId + "-caller", "working"))

    val dependency = new DefaultDependencyDescriptor(tmpModule,
      ModuleRevisionId.newInstance(groupId, artifactId, version), false, false, true)
    dependency.addDependencyConfiguration("default", "default")

    tmpModule.addDependency(dependency)

    val ivy = Ivy.newInstance(ivySettings)
    val report = ivy.resolve(tmpModule, resolveOptions)
    val artifacts = report.getAllArtifactsReports
    if (artifacts.length > 0)
      Option(artifacts(0).getLocalFile)
    else
      None
  }

  private def init() = {
    // limit Ivy console logging
    Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_ERR))

    val resolver = new IBiblioResolver
    resolver.setM2compatible(true)
    resolver.setName(RESOLVER_NAME)
//    resolver.setRoot(s"http://oxyjenartifactory-skinny.rhcloud.com/artifactory/$RESOLVER_NAME")
    resolver.setRoot(s"http://localhost:8081/artifactory/$RESOLVER_NAME")
    val ivySettings = new IvySettings
    ivySettings.addResolver(resolver)
    ivySettings.setDefaultResolver(RESOLVER_NAME)
    val resolveOptions = new ResolveOptions()
      .setConfs(Array("default"))
    (ivySettings, resolveOptions)
  }
}
