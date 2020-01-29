#!groovy

// null is min supported version of jenkins from plugin pom.xml
def minRequiredForPlugin = null

// LTS releases https://jenkins.io/changelog-stable/
def lts = '2.204.2'

buildPlugin(jenkinsVersions: [minRequiredForPlugin, lts], failFast: false)
