
def call(body) {


    def config = [:]
    def sonarIp = "http://213.32.75.99:9000"
    def deployServerIp = "213.32.75.99"
    def preprodServerIp = "137.74.200.56"

    def ssh = "root@${deployServerIp}"
    def preprodssh = "root@${preprodServerIp}"

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

            stage ('Build') {
                echo "Compiling and Building..."
                sh "mvn compile"
            }

            stage ('Dev Tests') {
                        echo "Testing..."
                        sh "mvn test"

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




            stage('Publish & Tag Image : Dev') {
                echo "Tagging Dev Image..."
                sh "docker tag ${config.projectName} ${config.projectName}:${config.Tag} "
                //envoyer vers Nexus 3
            }


            stage('Push Image To Dev ') {
                echo "Pushing Service Docker Image ..."
                sh "docker save ${config.projectName}:${config.Tag} | bzip2 | pv | ssh ${ssh} 'bunzip2 | docker load' "
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
                sh "ssh ${ssh}  docker run -d -p ${config.serverDomain}:${config.serverDomain} ${config.projectName}:${config.Tag}"
                echo "Realising is Done ! ..."
            }



            stage ('Pre-Prod Tests') {
                parallel 'Unit Test': {
                    echo "Testing..."
                    sh "mvn test"
                },
                        ' Sonar Test': {
                            echo"Testing With Sonar Server ..."
                            sh "mvn sonar:sonar -Dsonar.host.url=${sonarIp} -Dsonar.login=4f71b9d7d33b0f4e07d40d3acc7c26dad5e95fdc"

                        },
                        'Performance Tests':{


                        }
            }


            stage('Publish & Tag Image : Pre-Prod') {
                echo "Tagging Pre-Prod Image..."
                sh "docker tag ${config.projectName} ${config.projectName}:${config.Tag} "
                //envoyer vers Nexus 3

            }


            stage('Push Image To Pre-Prod ') {
                echo "Pushing Service Docker Image ..."
                try {
                    sh "ssh ${preprodssh} Clean-Container ${config.projectName}"
                    echo "Delete Container successfully"
                }catch (Exception exception){
                    echo "No Container to delete"
                }

                sh "docker save ${config.projectName} | bzip2 | pv | ssh ${preprodssh} 'bunzip2 | docker load' "
                sh "ssh ${preprodssh} docker tag ${config.projectName} ${config.projectName}:${config.Tag} "
                echo "Stage Test and Build Complete"
            }


            stage('Cleanup Pre-Prod Env') {
                echo "Destroying the build environment..."
                def cmd = "ssh ${preprodssh}  Clean-Image"
                try {
                    "${cmd}".execute()
                    echo "Cleaning ENV : successfully"
                }catch (Exception exception){
                    echo "Failed Cleaning ENV"
                }
                echo "Clean Complete"

            }
            stage('Deploy Image To PreProd') {
                echo " Starting Release phase..."



                sh "ssh ${preprodssh}  docker run -d -p ${config.serverDomain}:${config.serverDomain} ${config.projectName}:${config.Tag}"
                echo "Realising is Done ! ..."
            }



            stage('Manual Validation') {}
            stage('Publish and Tag Docker Image : Prod ') {}

        } catch (err) {
            currentBuild.result = 'FAILED'
            throw err
        }
    }
}
