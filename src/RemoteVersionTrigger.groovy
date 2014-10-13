import groovy.json.JsonSlurper
import groovy.transform.BaseScript
@BaseScript SharedFunctions mainScript

def workAroundClassLoader(){
    // No clue why needed, since it works in 'BuildJobFromRepo'
    // The 'dyanmic' groovy script lookup is kind of 'meh'
    println(BuildInfo.class)
    println(BuildInfo.StaticVersion.class)
    println(BuildInfo.MavenVersion.class)
}

println "Check if need to build!"
def allRepos = readAllProjects()

def mavenRepos = readAllProjects().findAll{ it.versionInfo.type =="maven" }
if(mavenRepos.size()==0){
    println "No dynamic version check found. Skipping!"
    return false
}


def localVersions = null
if(localVersionInfoFile.exists()){
    localVersions = new JsonSlurper().parse(localVersionInfoFile)
} else{
    new JsonSlurper().parseText("{}")
    storeVersionInfosLocal(mavenRepos)
    return false
}

def needToTrigger = mavenRepos.findAll { repo ->
    println "Project ${repo.namespace}/${repo.name}"
    def localVersion = localVersions[repo.name]
    println "Local version $localVersion"
    println "Remote version ${repo.versionInfo.versionTag}"
    if(localVersion!=repo.versionInfo.versionTag){
        println "Remote an local version do not match. Trigger build for $repo"
        return true;
    } else{
        return false
    }
}


needToTrigger.each {
    println "Posting trigger for $it"
    jenkinsApi.postText("job/${it.spoonizeProjectName()}/build","")
}

storeVersionInfosLocal(mavenRepos)