@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1' )
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.ContentType
import groovy.xml.XmlUtil



def env = System.getenv()

if(null==env['GIT_URL']){
    throw new EnvironmentVariableMissing("GIT_URL")
}
if(null==env['JENKINS_URL']){
    throw new EnvironmentVariableMissing("JENKINS_URL")
}

println "Building the project for this Git-Repo: ${env['GIT_REPO']} and will notify ${env['NOTIFY_EMAIL']}"

def jenkinsApi = new HTTPBuilder(env['JENKINS_URL'])


def templateXml = jenkinsApi.get(path: '/job/Template-Monitor-Git-And-Create-Job/config.xml',
        contentType : ContentType.XML)

templateXml.project.disabled = "false"


def gitConfig = templateXml.depthFirst().find {
    println(it.name)
    it.name() == "hudson.plugins.git.UserRemoteConfig"
}
gitConfig.url = env['GIT_REPO']

def user = "spoon-jenkins-user"
def mainRepoName = new URI(env['GIT_URL']).getPath().split("/").last().split("\\.").first()
def jobname = "z-auto-$user-$mainRepoName-job-control"


def bodyTxt = XmlUtil.serialize(templateXml)
jenkinsApi.request( Method.POST, ContentType.XML ) {
    uri.path = '/createItem'
    uri.query = [name:jobname]
    body = bodyTxt

}

jenkinsApi.request(Method.POST, ContentType.TEXT) {
    uri.path = "/job/$jobname/build"
    body = ""

}





class EnvironmentVariableMissing extends Exception{
    EnvironmentVariableMissing(String variableName) {
        super("Missing enviroment variable $variableName")
    }
}