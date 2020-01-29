#!groovy

// null is min supported version of jenkins from plugin pom.xml
def minRequiredForPlugin = null

// from LTS releases https://jenkins.io/changelog-stable/
def lts = '2.164.1'

buildPlugin(jenkinsVersions: [minRequiredForPlugin, lts], failFast: false)
