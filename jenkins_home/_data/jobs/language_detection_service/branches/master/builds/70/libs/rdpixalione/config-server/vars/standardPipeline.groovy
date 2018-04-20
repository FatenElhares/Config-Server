
def call(body) {


    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()



    node {
        // Clean workspace before doing anything
        deleteDir()
        try {


            stage ('Clone') {
                checkout scm
                echo "heeere ${config.projectName}"
            }


            stage ('Build') {
                echo "Compiling and Building..."
                sh 'mvn compile '
            }



            stage ('Tests') {
                parallel 'Test and Build': {
                    echo "Testing..."
                    sh 'mvn test'
                },
                        'Test with Sonar': {
                            echo"Testing With Sonar Server ..."
                            sh ' mvn sonar:sonar -Dsonar.host.url=http://213.32.75.99:9000 -Dsonar.login=4f71b9d7d33b0f4e07d40d3acc7c26dad5e95fdc'

                        }
            }


            stage('Create Docker Image') {
                echo "Cleanning Docker Containers ..."
                sh 'if [[ $(ssh root@213.32.75.99 docker ps -a -q --filter ancestor=${config.projectName} --format="{{.ID}}"  | xargs docker stop 2>/dev/null | xargs docker rm -f) -ne 0 ]]; then exit $(ssh root@213.32.75.99 docker ps -a -q --filter ancestor=${config.projectName} --format="{{.ID}}"  | xargs docker stop 2>/dev/null | xargs docker rm -f); fi '
                echo "Creating Service Image..."
                sh 'mvn package'
            }


            stage('Push Image') {
                echo "Pushing Service Docker Image ..."
                sh ' docker save ${config.projectName} | bzip2 | pv | ssh root@213.32.75.99  ` bunzip2 | docker load` '
                echo "Stage Test and Build Complete"
            }


            stage('Clean Env') {
                echo "Destroying the build environment..."
                sh 'if [[ $(ssh root@213.32.75.99  docker images -f dangling=true -q | xargs docker rmi) -ne 0 ]]; then exit $(ssh root@213.32.75.99  docker images -f dangling=true -q | xargs docker rmi); fi '
                echo "Clean Complete"
            }

            stage('Run Image') {
                echo " Starting Release phase..."
                sh 'ssh root@213.32.75.99  docker run -d -p 7072:7072 languagedetection --rm '
                echo "Realising is Done ! ..."
            }



        } catch (err) {
            currentBuild.result = 'FAILED'
            throw err
        }
    }
}
