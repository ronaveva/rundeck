#!/bin/bash

set -euo pipefail

main() {
    ENV=development

    if [[ ! -z "${RUNDECK_TAG:-}" ]] ; then
        ENV=release
    fi

    case "${1}" in
        build) build ;;
        buildFork) buildFork ;;
        buildDocker) buildDocker ;;
        publish) publish ;;
    esac
}

build() {
    ./gradlew -Penvironment="${ENV}" -x check install
    groovy testbuild.groovy --buildType="${ENV}"
    make ENV="${ENV}" rpm deb
}

buildFork() {
    ./gradlew -Penvironment="${ENV}" install
    groovy testbuild.groovy --buildType="${ENV}"
    make ENV="${ENV}" rpm deb
}

buildDocker() {
    docker_login

    local CLEAN_TAG=$(echo $RUNDECK_BRANCH | tr '/' '-')
    local BRANCH_AS_TAG=branch${CLEAN_TAG}

    local ECR_BUILD_TAG=${ECR_REPO}:build-${RUNDECK_BUILD_NUMBER}
    local ECR_BRANCH_TAG=${ECR_REPO}:${BRANCH_AS_TAG}

    local CI_BRANCH_TAG=rundeck/ci:${CLEAN_TAG}

    ./gradlew officialBuild -PdockerTags=latest,SNAPSHOT

    docker tag rundeck/rundeck:latest $ECR_BUILD_TAG
    docker tag rundeck/rundeck:latest $ECR_BRANCH_TAG
    docker tag rundeck/rundeck:latest $CI_BRANCH_TAG

    docker push $ECR_BUILD_TAG
    docker push $ECR_BRANCH_TAG
    docker push $CI_BRANCH_TAG

    if [[ "${RUNDECK_MASTER_BUILD}" = true ]] ; then
        ./gradlew officialPush -PdockerTags=SNAPSHOT
    else
        # Nothing
    fi
}

publish() {
    ./gradlew \
        -Penvironment="${ENV}" \
        -PdryRun="${DRY_RUN}" \
        -PbintrayUseExisting="true" \
        -PbintrayUser="${BINTRAY_USER}" \
        -PbintrayApiKey="${BINTRAY_API_KEY}" \
        -PsigningPassword="${RUNDECK_SIGNING_PASSWORD}" \
        -PsonatypeUsername="${SONATYPE_USERNAME}" \
        -PsonatypePassword="${SONATYPE_PASSWORD}" \
        bintrayUpload --info
}

main "${@}"
