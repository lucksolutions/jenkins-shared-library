def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    node {
        properties([
            pipelineTriggers([
                pollSCM('*/5 * * * *')
            ]),
            disableConcurrentBuilds()
        ])

        // if (env.BUILD_CAUSE == 'Branch indexing') {
        //     currentBuild.result = 'SUCCESSFUL'
        //     return
        // }

        try {
            stage('Checkout SCM') {
                def scmVars = checkout scm
            }

            stage('Check Cause') {
                //Skip the build if this was caused by branch indexing
                def causes = currentBuild.rawBuild.getCauses()
                sh "echo 'Build cause was: ${causes.getShortDescription()}'"
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