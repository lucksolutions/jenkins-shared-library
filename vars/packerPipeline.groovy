def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    //Skip the build if this was caused by branch indexing
    sh "Build cause was ${env.BUILD_CAUSE}"
    if (env.BUILD_CAUSE == 'Branch indexing') {
        currentBuild.result = 'SUCCESSFUL'
        return
    }

    node {
        properties([
            pipelineTriggers([
                pollSCM('*/5 * * * *')
            ]),
            disableConcurrentBuilds()
        ])

        try {
            stage('Checkout SCM') {
                def scmVars = checkout scm
            }

            packerBuild {
                directory = config.directory
                vars = config.vars
                packerFile = config.packerFile
            }
        } finally {
            //Send build notifications if needed
            notifyBuild(currentBuild.result)
        }
    }

}