import groovy.transform.BaseScript
@BaseScript SharedFunctions mainScript


def workAroundClassLoader(){
    // No clue why needed, since it works in 'BuildJobFromRepo'
    // The 'dyanmic' groovy script lookup is kind of 'meh'
    println(BuildInfo.class)
    println(BuildInfo.StaticVersion.class)
    println(BuildInfo.MavenVersion.class)
}

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
def jobname = "$user.$mainRepoName-job_control"


jenkinsApi.postXml("/createItem?name=$jobname",templateXml)

jenkinsApi.postText("/job/$jobname/build","")



