import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.3.5')
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils


abstract class SharedFunctions extends Script {
    public static def jenkinsApi = new JenkinsOperations(jenkinsUrl())

    def localVersionInfoFile = new File("last-version-infos.json")

    static def getRepoName(String url) {
        new URI(url).getPath().split("/").last().split("\\.").first()
    }

    static def tagByName(xmlNode, String name) {
        def node = xmlNode.depthFirst().find {
            it.name() == name
        }
        node
    }

    static def gitUrl() {
        def env = System.getenv()
        if (null == env['GIT_URL']) {
            throw new EnvironmentVariableMissing("GIT_URL")
        }
        env['GIT_URL']
    }

    static def jenkinsUrl() {

        def env = System.getenv()
        if (null == env['JENKINS_URL']) {
            throw new EnvironmentVariableMissing("JENKINS_URL")
        }
        env['JENKINS_URL']
    }

    def readAllProjects(){
        def listOfProjects = []
        def targetDir = new File("./target-repo")
        if(!targetDir.exists()) {
            throw new FileNotFoundException("Expect project at "+targetDir)
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
            readConfigOfProject(targetDir,it)
        }

        itemsToBuild
    }


    def readConfigOfProject(File root, File project){
        def json = new JsonSlurper()

        def platform = "32-bit"
        def email = "roman@spoon.net"
        def name = project.getName()
        if(name=="target-repo"){
            name = getRepoName(gitUrl())
        }

        def versionInfo = new BuildInfo.StaticVersion(root, project, null)
        def buildInfoFile = configFileOrParent(root,project,"autobuild.config.json");
        if(buildInfoFile.exists()){
            def buildInfo =json.parse(buildInfoFile)
            platform = buildInfo.platform ?:platform
            email = buildInfo.email ?:email
            name = buildInfo.name ?:name
            name = buildInfo.name ?:name
            name = buildInfo.name ?:name
            versionInfo = extractVersionReader(root, project,buildInfo.'version-reader')
        }
        def testFolder = ""
        def testFolderTest = configFileOrParent(root,project,"test")
        if(testFolderTest.exists()){
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
                versionInfo:versionInfo)
    }

    static def configFileOrParent(File root, File project, String configFileName){
        if(project==root){
            return new File(project,configFileName)
        } else {
            def configFile = new File(project,configFileName)
            if(configFile.exists()){
                return configFile
            } else{
                configFileOrParent(root,project.getParentFile(),configFileName)
            }
        }

    }

    def extractVersionReader(File root, File project,json){
         if(json.type=="maven"){
             new BuildInfo.MavenVersion(root,project,json)
         } else{
             new BuildInfo.StaticVersion(root,project,json)
         }
    }

    def storeVersionInfosLocal(projectInfos){
        def json = new HashMap()
        projectInfos.each{it ->
            if(it.versionInfo.type=="maven"){
                json[it.name] = it.versionInfo.versionTag
            }
        }
        localVersionInfoFile.write(new JsonBuilder(json).toPrettyString(),"UTF-8")
    }

}






def class HttpOperations{
    def httpclient = HttpClients.createDefault()

    def downloadText(String url){
        def request = new HttpGet(url)
        def response = httpclient.execute(request)
        if (response.getStatusLine().getStatusCode() == 200) {
            def content = EntityUtils.toString(response.getEntity())
            response.close()
            content
        } else {
            throw new WebException("Failed $fullUrl with status code: " + response.getStatusLine().getStatusCode())
        }

    }

    def downloadXml(String url){
        def rawXml = downloadText(url)
        def xml = new XmlSlurper().parseText(rawXml)
        xml
    }
}

class JenkinsOperations {
    String jenkinsUrl;
    def httpOps = new HttpOperations()

    JenkinsOperations(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl
    }

    def getXml(String path) {
        def fullUrl = jenkinsUrl + path
        httpOps.downloadXml(fullUrl)
    }
    def getXmlText(String path) {
        def fullUrl = jenkinsUrl + path
        httpOps.downloadText(fullUrl)
    }

    def postXml(String path, body) {
        def bodyTxt = XmlUtil.serialize(body)
        def fullUrl = jenkinsUrl + path
        def request = new HttpPost(fullUrl)
        request.setHeader("Content-Type", "application/xml")
        request.setEntity(new StringEntity(bodyTxt))
        def response = httpOps.httpclient.execute(request)
        def status = response.getStatusLine().getStatusCode()
        if (status >= 200 && status < 300) {
            EntityUtils.consume(response.getEntity())
            response.close()
        } else {
            throw new WebException("Failed $fullUrl with status code: " + response.getStatusLine().getStatusCode())
        }
    }

    def postText(String path, String body) {
        def fullUrl = jenkinsUrl + path
        def request = new HttpPost(fullUrl)
        request.setHeader("Content-Type", "application/xml")
        request.setEntity(new StringEntity("text/plain"))
        def response = httpOps.httpclient.execute(request)
        def status = response.getStatusLine().getStatusCode()
        if (status >= 200 && status < 300) {
            EntityUtils.consume(response.getEntity())
            response.close()
        } else {
            throw new WebException("Failed $fullUrl with status code: " + response.getStatusLine().getStatusCode())
        }
    }

    def updateOrCreateJob(String projectName, body) {
        if (projectExists(projectName)) {
            postXml("/job/$projectName/config.xml", body)
        } else {
            postXml("/createItem?name=$projectName", body)
        }
    }

    private def projectExists(projectName) {
        def request = new HttpGet(jenkinsUrl + "/job/$projectName/config.xml")
        def response = httpOps.httpclient.execute(request)
        def success = response.getStatusLine().getStatusCode() == 200
        response.close()
        success
    }
}

class WebException extends IOException {
    WebException(String error) {
        super(error)
    }
}

class EnvironmentVariableMissing extends Exception {
    EnvironmentVariableMissing(String variableName) {
        super("Missing enviroment variable $variableName")
    }
}