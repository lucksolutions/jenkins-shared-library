def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    pipeline {
        agent any

        triggers {
            //Check SCM every 5 minutes
            pollSCM('*/5 * * * *')
        }
        
        stages {
            stage('Docker Build') {
                steps {
                    dir("${config.directory}") {
                        script {
                            docker.withServer('tcp://ip-10-247-80-51.us-gov-west-1.compute.internal:2375') {
                                docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
                                    def image = docker.build("${config.imageName}:${BRANCH_NAME}")
                                    image.push()
                                    if (env.BRANCH_NAME == 'development') {
                                        image.push('latest')
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}