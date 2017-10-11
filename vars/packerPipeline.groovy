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

        try {
            stage('Checkout SCM') {
                def scmVars = checkout scm
                
                //Skip the build if this was caused by branch indexing
                def causes = currentBuild.rawBuild.getCauses()
                for (int i=0; i<causes.size(); ++i) {
                    if (causes[i].getShortDescription().contains('Branch indexing')) {
                        //Don't build since this was just branch indexing
                        currentBuild.result = 'SUCCESSFUL'
                        error('Stopping since we dont want to build on every index...')
                        return
                    }
                }
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