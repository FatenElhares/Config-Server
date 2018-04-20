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



            stage('Test and Build') {
                steps {
                    sh 'make testbuild'
                }
            }

            stage('Create Docker Image') {
                steps {
                    sh 'make create'
                }
            }


            stage('Push Image') {
                steps {
                    sh 'make push'
                }
            }

            stage('Clean Env') {
                steps {
                    sh 'make clean'
                }
            }

            stage('Run Image') {
                steps {
                    sh 'make release'
                }
            }


        } catch (err) {
            currentBuild.result = 'FAILED'
            throw err
        }
    }
}
