
import groovy.json.JsonSlurper
import groovy.transform.Immutable
import groovy.xml.XmlUtil
import groovy.transform.BaseScript
@BaseScript SharedFunctions mainScript

def spoonizeTemplate = jenkinsApi.getXml('/job/Template-Spoonize-Release/config.xml')
def testTemplate = jenkinsApi.getXml('/job/Template-Test-Spoonized-Release/config.xml')

def listOfProjects = []
def targetDir = new File("./target-repo")
if(!targetDir.exists()) {
    targetDir = new File("../target-repo")
}
targetDir.traverse { item ->
    if(item.isFile() && item.getName()=="spoon.me"){
        listOfProjects.add(item.getParentFile())
    }
}

listOfProjects.each { item ->
    println "Building $item"
}

def itemsToBuild =  listOfProjects.collect{
    readInfo(targetDir,it)
}

def newSpoonizeJobs = itemsToBuild.collect { info ->
    def xml = buildSpoonizeXml(info,spoonizeTemplate)
    [xml:xml,info:info]
}

def newTestJobs = itemsToBuild.collect { info ->
    def xml = buildTestXml(info,testTemplate)
    [xml:xml,info:info]
}

println "Creating spoonize jobs now"
newSpoonizeJobs.each {
    jenkinsApi.updateOrCreateJob(it.info.spoonizeProjectName(),it.xml)
}

println "Creating testing jobs now"
newTestJobs.each {
    jenkinsApi.updateOrCreateJob(it.info.testProjectName(),it.xml)
}

println "Trigger build job now"
newSpoonizeJobs.each {
    jenkinsApi.postText("job/${it.info.testProjectName()}/build")
}
println "Done. Building now"

def buildSpoonizeXml(BuildInfo projectInfo, templateXml){
    println "Building Spoonizing Job for: "+projectInfo
    def configXml = new XmlParser().parseText( XmlUtil.serialize( templateXml ) )

    baseXmlSetup(templateXml, projectInfo)

    templateXml.assignedNode = projectInfo.platform+"-spoonizer"

    def batchFile = tagByName(templateXml,"hudson.tasks.BatchFile")
    batchFile.command = copyInSpoonizeCommand(projectInfo,"spoonize-project-template.bat");

    def nextJob = tagByName(templateXml,"hudson.plugins.parameterizedtrigger.BuildTriggerConfig")
    nextJob.projects = projectInfo.testProjectName()

    templateXml
}

def baseXmlSetup(templateXml, BuildInfo projectInfo) {
    templateXml.disabled = "false"

    def version = templateXml.depthFirst().find {
        it.name() == "hudson.model.StringParameterDefinition" && it.name.text() == "VERSION"
    }
    version.defaultValue = projectInfo.version

    def gitConfig = tagByName(templateXml, "hudson.plugins.git.UserRemoteConfig")
    gitConfig.url = gitUrl()

    def email = tagByName(templateXml, "hudson.tasks.Mailer")
    email.recipients = projectInfo.email
}

def buildTestXml(BuildInfo projectInfo, templateXml){
    println "Building Test Job for: "+projectInfo
    def configXml = new XmlParser().parseText( XmlUtil.serialize( templateXml ) )

    baseXmlSetup(templateXml, projectInfo)

    def batchFile = tagByName(templateXml,"hudson.tasks.BatchFile")
    batchFile.command = copyInSpoonizeCommand(projectInfo,"test-project-template.bat");


    def testMachines = tagByName(templateXml,"org.jenkinsci.plugins.elasticaxisplugin.ElasticAxis")
    testMachines.label = projectInfo.platform+"-test"

    templateXml
}

def copyInSpoonizeCommand(BuildInfo projectInfo,String templateName){
    def possiblePlaces = [
        new File("../scripts/src/$templateName"),
        new File("./scripts/src/$templateName"),
        new File("./src/$templateName")
    ]

    def scriptFile = possiblePlaces.find {it.exists()}
    def text = scriptFile.getText("UTF-8")
    text.replace("{namespace}",projectInfo.namespace)
        .replace("{repo-name}",projectInfo.name)
}

def readInfo(File root, File project){
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
    def name = project.getName()
    if(name=="target-repo"){
        name = getRepoName(gitUrl())
    }
    def testFolder = ""
    if(new File(root,"test").exists()){
        testFolder = "test"
    }
    new BuildInfo(
            namespace:"spoonbrew",
            name:name,
            email:email,
            platform: platform,
            version:versionTag)
}

@Immutable class BuildInfo {
    String namespace
    String name
    String email
    String platform
    String version


    def spoonizeProjectName(){
        "z-auto-$namespace-$name-spoonize"
    }
    def testProjectName(){
        "z-auto-$namespace-$name-test"
    }
}