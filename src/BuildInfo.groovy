public class BuildInfo {
    String workingDirectory
    String namespace
    String name
    String email
    String platform
    VersionSourceBase versionInfo


    def jobcontrolProjectName(){
        "$namespace.$name-job_control"
    }

    def spoonizeProjectName(){
        "$namespace.$name-spoonize"
    }
    def testProjectName(){
        "$namespace.$name-test"
    }
    def triggerProjectName(){
        "$namespace.$name-trigger"
    }


    @Override
    public String toString() {
        return "BuildInfo{" +
                "workingDirectory='" + workingDirectory + '\'' +
                ", namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", platform='" + platform + '\'' +
                ", versionInfo=" + versionInfo +
                '}';
    }



    static abstract class VersionSourceBase{
        public VersionSourceBase(String type) {
            this.type = type
        }
        String type
        String versionTag

        @Override
        public String toString() {
            return "VersionSourceBase{" +
                    "type='" + type + '\'' +
                    ", versionTag='" + versionTag + '\'' +
                    '}';
        }
    }

    static class StaticVersion extends BuildInfo.VersionSourceBase{
        StaticVersion(File root, File project, json) {
            super("static")
            versionTag = "head"
            def versionFile = SharedFunctions.configFileOrParent(root,project,"autobuild.version.txt")
            if(versionFile.exists()){
                versionTag = versionFile.getText("UTF-8")
            }
        }

    }

    static class MavenVersion extends BuildInfo.VersionSourceBase{
        String group;
        String artifactId;
        String repo;

        MavenVersion(File root, File project, json) {
            super("maven")
            this.group = json.group
            this.artifactId = json.artifactId
            this.repo = json.repo ?: "http://repo1.maven.org/maven2/"

            def metaDataXml = repo+group.replace('.','/')+"/"+artifactId +"/maven-metadata.xml"
            def remoteEntries = new HttpOperations().downloadXml(metaDataXml)
            def allVersions = extractVersions(remoteEntries)
            versionTag = allVersions.last()
        }

        def readVersion(){
        }

        def extractVersions(xml){
            def versionsTag = xml.versioning.versions
            def versions = versionsTag.version.collect{v -> v.text()}

            versions.sort()
        }


        @Override
        public String toString() {
            return "MavenVersion{" +
                    "type='" + type + '\'' +
                    ", versionTag='" + versionTag + '\'' +
                    ", group='" + group + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    ", repo='" + repo + '\'' +
                    '}';
        }
    }
}
