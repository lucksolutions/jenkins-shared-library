def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config

    if (config.directory == null) {
        config.directory = '.'
    }

    node {
        properties([
            pipelineTriggers([
                pollSCM('*/5 * * * *')
            ])
        ])

        try {
            stage('Checkout SCM') {
                checkout scm
            }
            
            dockerBuild {
                directory = config.directory
                imageName = config.imageName
            }

            body()
        } finally {
            //Send build notifications if needed
            notifyBuild(currentBuild.result)
        }
    }
}