@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.5' )
import org.apache.http.client.HttpClient
import groovy.xml.XmlUtil
import groovy.transform.BaseScript
@BaseScript SharedFunctions mainScript



println "Building the project for Git-Repo: ${gitUrl()}"


def templateXml = jenkinsApi.getXml('/job/Template-Monitor-Git-And-Create-Job/config.xml')

templateXml.disabled = "false"


def gitConfig = templateXml.depthFirst().find {
    println(it.name)
    it.name() == "hudson.plugins.git.UserRemoteConfig"
}
gitConfig.url = gitUrl()

def user = "spoon-jenkins-user"
def mainRepoName = getRepoName(gitUrl())
def jobname = "z-auto-$user-$mainRepoName-job-control"


jenkinsApi.postXml("/createItem?name=$jobname",templateXml)

jenkinsApi.postText("/job/$jobname/build","")



