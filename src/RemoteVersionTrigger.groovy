import groovy.json.JsonSlurper
import groovy.transform.BaseScript

@BaseScript SharedFunctions mainScript

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
    storeVersionInfosLocal(mavenRepos)
} else{
    new JsonSlurper().parseText("{}")
    storeVersionInfosLocal(mavenRepos)
    return false
}

def needToTrigger = mavenRepos.findAll { repo ->
    println "Project ${repo.namespace}/${repo.name}"
    def localVersion = localVersions[repo.name]
    println "Local version $localVersion"
    println "Remove version ${repo.versionInfo.versionTag}"
    if(localVersion!=repo.versionInfo.versionTag){
        println "Remote an local version do not match. Trigger build for $repo"
        return true;
    } else{
        return false
    }
}


newSpoonizeJobs.each {
    println "Posting trigger for $it"
    jenkinsApi.postText("job/${it.info.spoonizeProjectName()}/build","")
}