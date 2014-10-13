
import groovy.json.JsonSlurper
import groovy.transform.Immutable
import groovy.transform.BaseScript
@BaseScript SharedFunctions mainScript

def spoonizeTemplate = jenkinsApi.getXmlText('/job/Template-Spoonize-Release/config.xml')
def testTemplate = jenkinsApi.getXmlText('/job/Template-Test-Spoonized-Release/config.xml')
def BuildTriggerConfig = jenkinsApi.getXmlText('/job/Template-Trigger-MavenUpdate/config.xml')

itemsToBuild = readAllProjects()

println "We are going to build: "

itemsToBuild.each{
    println "- "+it
}

println "Store local version info"
storeVersionInfosLocal(itemsToBuild)


def newSpoonizeJobs = itemsToBuild.collect { info ->
    def xml = buildSpoonizeXml(info,spoonizeTemplate)
    [xml:xml,info:info]
}

def newTestJobs = itemsToBuild.collect { info ->
    def xml = buildTestXml(info,testTemplate)
    [xml:xml,info:info]
}

def newSpecialTrigger = itemsToBuild.collect { info ->
    def xml = buildTriggerXml(info,BuildTriggerConfig)
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

println "Posting additional trigger jobs now"
newSpecialTrigger.each {
    jenkinsApi.updateOrCreateJob(it.info.triggerProjectName(),it.xml)
}

println "Trigger build job now"
newSpoonizeJobs.each {
    //jenkinsApi.postText("job/${it.info.spoonizeProjectName()}/build","")
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

def buildTriggerXml(BuildInfo projectInfo, theTemplate){
    println "Building Trigger Job for: "+projectInfo
    def configXml = new XmlSlurper().parseText(theTemplate )

    baseXmlSetup(configXml, projectInfo)

    def nextJob = tagByName(configXml,"hudson.plugins.parameterizedtrigger.BuildTriggerConfig")
    nextJob.projects = projectInfo.testProjectName()

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
        .replace("{version}",projectInfo.versionInfo.versionTag)
        .replace("{working-directory}",projectInfo.workingDirectory)
}
