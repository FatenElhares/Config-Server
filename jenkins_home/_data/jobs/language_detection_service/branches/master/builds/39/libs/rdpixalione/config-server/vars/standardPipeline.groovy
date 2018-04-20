def call(body) {


    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    println "Yay! I can finally be expressive now!"
    node {
        // Clean workspace before doing anything
        deleteDir()

        try {
            stage ('Clone') {
                checkout scm
            }


            stage ('Build') {
                sh 'make build'
            }



            stage ('Tests') {
                parallel 'Test and Build': {
                    sh 'make test'
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
