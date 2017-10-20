def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    //Required Parameters
    if (config.releaseVersion == null) {
        error('Release version number is required.')
    }

    if (config.devVersion == null) {
        error('Next development version number is required.')
    }

    //Optional Parameters

    if (config.directory == null) {
        config.directory = '.'
    }

    node {
        stage('Check master branch') {
            //Compare to master branch to look for any unmerged changes
        }

        stage('Set Release Version') {
            //Set release version
            //Check for any snapshot versions remaining

            //Commit changes locally
        }

        stage('Tag Release') {
            //Tag release
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