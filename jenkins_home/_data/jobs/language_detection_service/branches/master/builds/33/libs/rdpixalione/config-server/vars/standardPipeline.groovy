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






            stage ('Tests') {
                parallel 'Test and Build': {
                    sh 'make testbuild'
                },
                        'Test with Sonar': {
                            sh 'make SonarTest'
                        }
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
