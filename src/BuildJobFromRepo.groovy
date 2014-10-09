import groovy.json.JsonSlurper
import groovy.transform.Immutable
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1' )
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ContentType


def listOfProjects = []
new File("./target-repo").traverse { item ->
    if(item.isFile() && item.getName()=="spoon.me"){
        listOfProjects.add(item.getParentFile())
    }
}

listOfProjects.each { item ->
    println "Building $item"
}

def newJobs = listOfProjects.collect { item ->
    buildProjectXml(item,templateXml)
}


def buildProjectXml(File project, templateXml){
    def configXml = new XmlParser().parseText( XmlUtil.serialize( templateXml ) )

    def readProjectInfo = readInfo(project)

    templateXml.project.disabled = "false"

    def gitConfig = templateXml.depthFirst().find {
        println(it.name)
        it.name() == "hudson.plugins.git.UserRemoteConfig"
    }
    gitConfig.url = env['GIT_URL']

    return "hello"
}

def readInfo(File project){
    def versionTag = "head"
    def versionFile = new File(project,"autobuild.version.txt")
    if(versionFile.exists()){
        versionTag = versionFile.getText("UTF-8")
    }

    def json = new JsonSlurper()

    def platform = "32-bit"
    def email = "roman@spoon.net"

    def buildInfoFile = new File(project,"autobuild.build-info.txt")
    if(buildInfoFile.exists()){
        def buildInfo =json.parse(buildInfo)
        platform = buildInfo.platform ?:platform
        email = buildInfo.email ?:email
    }
    new BuildInfo(email:email, platform: platform,  version:versionTag)
}

@Immutable class BuildInfo {
    String email
    String plattform
    String version

}