def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }
    if (config.imageName == null) {
        error "imageName parameter was not specified for dockerBuild"
    }

    dir("${config.directory}") {
        stage("Build ${config.imageName}") {
            docker.withServer('tcp://ip-10-247-80-40.us-gov-west-1.compute.internal:2375') {
                docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
                    def image = docker.build("${config.imageName}:${BRANCH_NAME}")
                }
            }
        }
        stage("Deploy ${config.imageName}") {
            sh 'echo "Deploying Docker Container..."'
        }
        stage("Test ${config.imageName}") {
            sh 'echo "Executing test cases..."'
        }
        stage("Push ${config.imageName} to Registry") {
            docker.withServer('tcp://ip-10-247-80-40.us-gov-west-1.compute.internal:2375') {
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