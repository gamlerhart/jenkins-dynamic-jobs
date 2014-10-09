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
    return "hello"
}

def readInfo(File project){
    def versionTag = "head"
    def versionFile = new File(project,"version.jenkins.txt")
    if(versionFile.exists()){
        versionTag = versionFile.getText("UTF-8")
    }

    def buildInfo = ""
}