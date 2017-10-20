def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    //Optional Parameters
    if (config.directory == null) {
        config.directory = '.'
    }

    def buildStatus =  currentBuild.result ?: 'SUCCESSFUL'
    echo "Build Status: ${currentBuild.result}" 

    if (!isPullRequest() && buildStatus == 'SUCCESSFUL') {
        //Create a milestone that will abort older builds when a newer build passes this stage.
        milestone()
        def versions = input(id: "versions", message: "Release this build?", parameters: [
            [$class: 'StringParameterDefinition', defaultValue: '', description: 'Release Version', name: 'release'],
            [$class: 'StringParameterDefinition', defaultValue: '', description: 'Next Development Version', name: 'development']
        ])
        milestone()

        node {
            stage('Build Info') {
                echo "Branch Name: ${env.BRANCH_NAME}"
                echo "Change ID: ${env.CHANGE_ID}"
                echo "Change URL: ${env.CHANGE_URL}"
                echo "Change Target: ${env.CHANGE_TARGET}"
                echo "ChangeSet Size: ${currentBuild.changeSets.size()}"
                echo "Pull Request?: ${isPullRequest()}"
                echo ("Release Version: "+versions['release'])
                echo ("Next Development Version: "+versions['development'])
            }

            stage('Checkout SCM') {
                checkout scm
            }

            stage('Check master branch') {
                sh 'git branch -r'

                //Compare to master branch to look for any unmerged changes
                def commitsBehind = sh(returnStdout: true, script: "git rev-list --right-only --count ${env.BRANCH_NAME}...origin/master").trim().toInteger()
                if (commitBehind > 0) {
                    error("Master Branch has changesets not included on this branch. Please merge master into your branch before releaseing.")
                } else {
                    echo "Branch is up to date with changesets on master. Proceeding with release..."
                }
            }

            stage('Set Release Version') {
                //Set release version
                //Check for any snapshot versions remaining

                //Commit changes locally
            }

            stage('Tag Release') {
                //Tag release
                sh "git tag -a ${versions['release']} -m \"Release version ${versions['release']}\""
                //Commit changes locally
            }

            stage('Run Build Steps') {
                //Run the build steps for this project type
            }

            stage('Set Next Development Version') {
                //Set the next dev version
                //Commit changes locally

                //Push to Github
            }
        }
    }
}