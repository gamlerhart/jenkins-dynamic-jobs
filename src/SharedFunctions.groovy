import groovy.xml.XmlUtil
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.3.5')
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils


abstract class SharedFunctions extends Script {
    public static def jenkinsApi = new JenkinsOperations(jenkinsUrl())

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
}

class JenkinsOperations {
    def httpclient = HttpClients.createDefault()
    String jenkinsUrl;

    JenkinsOperations(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl
    }

    def getXml(String path) {
        def fullUrl = jenkinsUrl + path
        def request = new HttpGet(fullUrl)
        def response = httpclient.execute(request)
        if (response.getStatusLine().getStatusCode() == 200) {
            def content = EntityUtils.toString(response.getEntity())
            def result = new XmlSlurper().parseText(content)
            response.close()
            result
        } else {
            throw new WebException("Failed $fullUrl with status code: " + response.getStatusLine().getStatusCode())
        }
    }

    def postXml(String path, body) {
        def bodyTxt = XmlUtil.serialize(body)
        def fullUrl = jenkinsUrl + path
        def request = new HttpPost(fullUrl)
        request.setHeader("Content-Type", "application/xml")
        request.setEntity(new StringEntity(bodyTxt))
        def response = httpclient.execute(request)
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
        def response = httpclient.execute(request)
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
        def response = httpclient.execute(request)
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