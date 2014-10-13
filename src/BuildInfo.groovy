public class BuildInfo {
    String workingDirectory
    String namespace
    String name
    String email
    String platform
    VersionSourceBase versionInfo


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
}
