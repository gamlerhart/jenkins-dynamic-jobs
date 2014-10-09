@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1' )
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.ContentType
import groovy.xml.XmlUtil




def env = System.getenv()

if(null==env['GIT_REPO']){
    throw new EnvironmentVariableMissing("GIT_REPO")
}
if(null==env['JENKINS_URL']){
    throw new EnvironmentVariableMissing("JENKINS_URL")
}

println "Building the project for this Git-Repo: ${env['GIT_REPO']} and will notify ${env['NOTIFY_EMAIL']}"

def jenkinsApi = new HTTPBuilder(env['JENKINS_URL'])


def templateXml = jenkinsApi.get(path: '/job/Template-Monitor-Git-And-Create-Job/config.xml',
        contentType : ContentType.XML)




def gitConfig = templateXml.depthFirst().find {
    println(it.name)
    it.name() == "hudson.plugins.git.UserRemoteConfig"
}
gitConfig.url = env['GIT_REPO']

def user = "spoon-jenkins-user"
def mainRepoName = new URI(env['GIT_REPO']).getPath().split("/").last()
def jobname = "z-auto-$user-$mainRepoName-job-control"

def bodyTxt = XmlUtil.serialize(templateXml)
jenkinsApi.request( Method.POST, ContentType.XML ) {
    uri.path = '/createItem'
    uri.query = [name:'AA-Test-2']
    body = bodyTxt

}

jenkinsApi.request(Method.POST, ContentType.XML) {
    uri.path = '/job/'
    uri.query = [name:'AA-Test-2']
    body = bodyTxt

}





class EnvironmentVariableMissing extends Exception{
    EnvironmentVariableMissing(String variableName) {
        super("Missing enviroment variable $variableName")
    }
}