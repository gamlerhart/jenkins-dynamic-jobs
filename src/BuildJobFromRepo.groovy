
import groovy.json.JsonSlurper
import groovy.transform.Immutable
import groovy.xml.XmlUtil
import groovy.transform.BaseScript
@BaseScript SharedFunctions mainScript

def spoonizeTemplate = jenkinsApi.getXmlText('/job/Template-Spoonize-Release/config.xml')
def testTemplate = jenkinsApi.getXmlText('/job/Template-Test-Spoonized-Release/config.xml')

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

println "We are going to build: "

itemsToBuild.each{
    println "- "+it
}

def newSpoonizeJobs = itemsToBuild.collect { info ->
    def xml = buildSpoonizeXml(info,spoonizeTemplate)
    [xml:xml,info:info]
}

def newTestJobs = itemsToBuild.collect { info ->
    def xml = buildTestXml(info,testTemplate)
    [xml:xml,info:info]
}

println "Posting spoonize jobs now"
newSpoonizeJobs.each {
    jenkinsApi.updateOrCreateJob(it.info.spoonizeProjectName(),it.xml)
}

println "Posting testing jobs now"
newTestJobs.each {
    jenkinsApi.updateOrCreateJob(it.info.testProjectName(),it.xml)
}

println "Trigger build job now"
newSpoonizeJobs.each {
    jenkinsApi.postText("job/${it.info.spoonizeProjectName()}/build","")
}
println "Done. Building now"

def buildSpoonizeXml(BuildInfo projectInfo, theTemplate){
    println "Building Spoonizing Job for: "+projectInfo
    def configXml = new XmlSlurper().parseText( theTemplate )

    baseXmlSetup(configXml, projectInfo)

    configXml.assignedNode = projectInfo.platform+"-spoonizer"

    def batchFile = tagByName(configXml,"hudson.tasks.BatchFile")
    batchFile.command = copyInSpoonizeCommand(projectInfo,"spoonize-project-template.bat");

    def nextJob = tagByName(configXml,"hudson.plugins.parameterizedtrigger.BuildTriggerConfig")
    nextJob.projects = projectInfo.testProjectName()

    configXml
}

def baseXmlSetup(templateXml, BuildInfo projectInfo) {
    templateXml.disabled = "false"

    def gitConfig = tagByName(templateXml, "hudson.plugins.git.UserRemoteConfig")
    gitConfig.url = gitUrl()

    def email = tagByName(templateXml, "hudson.tasks.Mailer")
    email.recipients = projectInfo.email
}

def buildTestXml(BuildInfo projectInfo, theTemplate){
    println "Building Test Job for: "+projectInfo
    def configXml = new XmlSlurper().parseText(theTemplate )

    baseXmlSetup(configXml, projectInfo)

    def batchFile = tagByName(configXml,"hudson.tasks.BatchFile")
    batchFile.command = copyInSpoonizeCommand(projectInfo,"test-project-template.bat");


    def testMachines = tagByName(configXml,"org.jenkinsci.plugins.elasticaxisplugin.ElasticAxis")
    testMachines.label = projectInfo.platform+"-test"

    configXml
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
        .replace("{version}",projectInfo.version)
        .replace("{working-directory}",projectInfo.workingDirectory)
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
    def name = project.getName()
    if(name=="target-repo"){
        name = getRepoName(gitUrl())
    }

    def buildInfoFile = new File(project,"autobuild.config.json")
    if(buildInfoFile.exists()){
        def buildInfo =json.parse(buildInfoFile)
        platform = buildInfo.platform ?:platform
        email = buildInfo.email ?:email
        name = buildInfo.name ?:name
    }
    def testFolder = ""
    if(new File(root,"test").exists()){
        testFolder = "test"
    }
    def relativePath = root.toPath().relativize(project.toPath())
    def workingDir = relativePath.toString()
    if(workingDir.isEmpty()){
        workingDir = "."
    }
    new BuildInfo(
            workingDirectory:workingDir,
            namespace:"spoon-jenkins-user",
            name:name,
            email:email,
            platform: platform,
            version:versionTag)
}

@Immutable class BuildInfo {
    String workingDirectory
    String namespace
    String name
    String email
    String platform
    String version


    def spoonizeProjectName(){
        "$namespace.$name-spoonize"
    }
    def testProjectName(){
        "$namespace.$name-test"
    }
}