
def call(body) {


    def config = [:]
    def sonarIp = "http://213.32.75.99:9000"
    def deployServerIp = "213.32.75.99"
    def ssh = "root@${deployServerIp}"
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()


    node {
        // Clean workspace before doing anything
        deleteDir()
        try {


            stage ('Git Clone and Setup') {
                checkout scm
            }

            stage ('First Build') {
                echo "Compiling and Building..."
                sh "mvn compile"
            }

            stage ('Dev Tests') {
                parallel 'Unit Test': {
                        echo "Testing..."
                        sh "mvn test"
                },
                    ' Sonar Test': {
                        echo"Testing With Sonar Server ..."
                        sh "mvn sonar:sonar -Dsonar.host.url=${sonarIp} -Dsonar.login=4f71b9d7d33b0f4e07d40d3acc7c26dad5e95fdc"

                }
            }


            stage('Create Docker Image') {
                echo "Cleaning Docker Containers ..."
               // def cmd=" ssh ${ssh} docker ps -a -q --filter ancestor=${config.projectName} --format='{{.ID}}'  | xargs docker stop 2>/dev/null | xargs docker rm -f "

                try {
                    sh "ssh ${ssh} docker ps -a -q --filter ancestor=${config.projectName} --format=\'{{.ID}}\'  | xargs docker stop 2>/dev/null | xargs docker rm -f"
                    echo "Delete Container successfully"
                }catch (Exception exception){
                    echo "No Container to delete"
                }

                echo "Creating Service Image..."
                sh "mvn package"

            }

            stage('Publish and Tag Docker Image : Dev') {}


            stage('Push Image To Dev ') {
                echo "Pushing Service Docker Image ..."
                sh "docker save ${config.projectName} | bzip2 | pv | ssh ${ssh} 'bunzip2 | docker load' "
                echo "Stage Test and Build Complete"
            }


            stage('Cleanup Dev Env') {
                echo "Destroying the build environment..."
                def cmd = "ssh ${ssh}  docker images -f dangling=true -q | xargs docker rmi"
                try {
                   "${cmd}".execute()
                    echo "Cleaning ENV : successfully"
                }catch (Exception exception){
                    echo "Failed Cleaning ENV"
                }
                echo "Clean Complete"
            }

            stage('Deploy Image to Dev') {
                echo " Starting Release phase..."
                sh "ssh ${ssh}  docker run -d -p ${config.serverDomain}:${config.serverDomain} ${config.projectName}"
                echo "Realising is Done ! ..."
            }



            stage('Performance Tests') {}
            stage('Publish and Tag Docker Image : Pre-Prod') {}
            stage('Cleanup Pre-Prod Env') {}
            stage('Deploy Image To PreProd') {}

            stage('Manual Validation') {}
            stage('Publish and Tag Docker Image : Prod ') {}

        } catch (err) {
            currentBuild.result = 'FAILED'
            throw err
        }
    }
}
