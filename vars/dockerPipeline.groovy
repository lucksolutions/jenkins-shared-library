def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    node {
        stage('Checkout SCM') {
            checkout scm
        }
        stage('Build Image') {
            dir("${config.directory}") {
                docker.withServer('tcp://ip-10-247-80-62.us-gov-west-1.compute.internal:2375') {
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
                        def image = docker.build("${config.imageName}:${BRANCH_NAME}")
                    }
                }
            }
        }
        stage('Deploy Image') {
            sh 'echo "Deploying Docker Container..."'
        }
        stage('Test Image') {
            sh 'echo "Executing test cases..."'
        }
        stage('Push to Registry') {
            dir("${config.directory}") {
                docker.withServer('tcp://ip-10-247-80-62.us-gov-west-1.compute.internal:2375') {
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