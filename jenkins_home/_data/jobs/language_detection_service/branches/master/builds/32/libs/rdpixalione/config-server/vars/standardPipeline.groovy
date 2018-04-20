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
                sh "echo 'building ${config.projectName} ...'"
            }
            stage ('Tests') {
                parallel 'static': {
                    sh "echo 'shell scripts to run static tests...'"
                },
                        'unit': {
                            sh "echo 'shell scripts to run unit tests...'"
                        },
                        'integration': {
                            sh "echo 'shell scripts to run integration tests...'"
                        }
            }
            stage ('Deploy') {
                sh "echo 'deploying to server ${config.serverDomain}...'"
            }

            stage('Test and Build') {
                sh 'make testbuild'
            }

            stage('Create Docker Image') {
                    sh 'make create'
            }
            stage('Push Image') {
                    sh 'make push'
            }


            stage('Clean Env') {
                    sh 'make clean'
            }

            stage('Run Image') {
                    sh 'make release'
            }




        } catch (err) {
            currentBuild.result = 'FAILED'
            throw err
        }
    }
}
