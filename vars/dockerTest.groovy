def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    node {
        stage('Test Docker') {
            sh 'whoami'
            sh 'docker ps'
        }
    }
}