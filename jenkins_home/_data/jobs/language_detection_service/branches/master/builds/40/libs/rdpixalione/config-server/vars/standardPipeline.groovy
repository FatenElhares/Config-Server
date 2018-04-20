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
            }


            stage ('Build') {
                sh ' echo -e "\\e[36mCompiling and Building..." '
                sh 'mvn compile '
            }



            stage ('Tests') {
                parallel 'Test and Build': {
                    sh 'echo -e "\\e[36mTesting..." '
                    sh 'mvn test'
                },
                        'Test with Sonar': {
                            sh 'echo -e "\\e[36mTesting With Sonar Server ..."'
                            sh ' mvn sonar:sonar -Dsonar.host.url=http://213.32.75.99:9000 -Dsonar.login=4f71b9d7d33b0f4e07d40d3acc7c26dad5e95fdc'

                        }
            }


            stage('Create Docker Image') {
                sh 'echo -e "\\e[36mCleanning Docker Containers ..."'
                sh 'CLEAN_CONTAINER =$$(ssh root@213.32.75.99 docker ps -a -q --filter ancestor=languagedetection --format="{{.ID}}"  | xargs docker stop 2>/dev/null | xargs docker rm -f)'
                sh 'if [[ $(CLEAN_CONTAINER) -ne 0 ]]; then exit $(CLEAN_CONTAINER); fi '
                sh 'echo -e "\\e[36mCreating Language Detection Image..."'
                sh 'mvn package'
            }


            stage('Push Image') {
                sh 'echo -e "\\e[36mPushing languageDetection Docker Image ..."'
                sh ' docker save languagedetection | bzip2 | pv | ssh root@213.32.75.99  "bunzip2 | docker load" '
                sh 'echo -e "\\e[36mStage Test and Build Complete"'
            }


            stage('Clean Env') {
                sh 'echo -e "\\e[36mDestroying the build environment..."'
                sh' ssh root@213.32.75.99  docker images -f dangling=true -q | xargs docker rmi'
                sh 'echo -e "\\e[36mClean Complete"'
            }

            stage('Run Image') {
                sh 'echo -e "\\e[36mStarting Release phase..."'
                sh' ssh root@213.32.75.99  docker run -d -p 7072:7072 languagedetection --rm'
                sh 'echo -e "\\e[36mRealising is Done ! ..."'
            }




        } catch (err) {
            currentBuild.result = 'FAILED'
            throw err
        }
    }
}
