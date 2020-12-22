# ec2-spot-jenkins-plugin

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/ec2-fleet-plugin/master)](https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Fec2-fleet-plugin/activity) [![](https://img.shields.io/jenkins/plugin/v/ec2-fleet.svg)](https://github.com/jenkinsci/ec2-fleet-plugin/releases)
[![Gitter](https://badges.gitter.im/ec2-fleet-plugin/community.svg)](https://gitter.im/ec2-fleet-plugin/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/ec2-fleet.svg?color=blue)](https://plugins.jenkins.io/ec2-fleet)

Use [jenkinsci/ec2-fleet-plugin](https://github.com/jenkinsci/ec2-fleet-plugin) instead of [awslabs/ec2-spot-jenkins-plugin](https://github.com/awslabs/ec2-spot-jenkins-plugin)


The EC2 Spot Jenkins plugin launches EC2 Spot instances as worker nodes for Jenkins CI server, 
automatically scaling the capacity with the load. 

* [Jenkins Page](https://wiki.jenkins.io/display/JENKINS/Amazon+EC2+Fleet+Plugin)
* [Report Issue](https://github.com/jenkinsci/ec2-fleet-plugin/issues/new)
* [Overview](#overview)
* [Features](#features)
* [Change Log](#change-log)
* [Usage](#usage)
  * [Setup](#setup)
  * [Scaling](#scaling)
  * [Groovy](#groovy)
  * [Preconfigure Slave](#preconfigure-slave)
  * [Label Based Configuration (beta)](docs/LABEL-BASED-CONFIGURATION.md)
  * [Windows Slave](docs/SETUP-WINDOWS-SLAVE.md)
  * [Configuration As Code](docs/CONFIGURATION-AS-CODE.md)
* [Development](#development)

# Overview

This plugin uses [EC2 Spot Fleet](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet.html) or [Auto Scaling Group](https://docs.aws.amazon.com/autoscaling/ec2/userguide/AutoScalingGroup.html) to launch instances instead of directly launching them by itself. 
Both maintain your fleet's target capacity as Spot prices change to maintain
the fleet within the specified price range. For more information, see 
[How Spot Fleet Works](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet.html).

# Features

- Support EC2 Spot Fleet or Auto Scaling Group as Jenkins Workers
- Supports all features provided by EC2 Spot Fleet or Auto Scaling Groups
- Auto resubmit failed jobs caused by Spot interruptions
- No delay scale up strategy: enable ```No Delay Provision Strategy``` in configuration
- Add tags to EC2 instances used by plugin, for easy search, tag format ```ec2-fleet-plugin:cloud-name=<MyCloud>```
- Allow custom EC2 API endpoint
- Auto Fleet creation based on Job label ([details](docs/LABEL-BASED-CONFIGURATION.md))

## Comparison to EC2-Plugin

[EC2-Plugin](https://plugins.jenkins.io/ec2/) is a similar Jenkins plugin that will request EC2 instances when excess jobs are
detected. The main difference between the two plugins is that EC2-Fleet-Plugin uses EC2 Spot Fleet and ASG to request and manage
instances instead of doing it manually with EC2 RunInstances. This gives EC2-Fleet-Plugin all the benefits of Spot Fleet and ASG:
allocation strategies, automatic availability zone rebalancing (ASG only), access to launch configurations and launch templates
, instance weighting, etc. 

| EC2-Fleet-Plugin | EC2-Plugin |
| --- | --- |
| Supports On-Demand & Spot Instances | Supports On-Demand & Spot Instances |
| Scales with EC2 Spot Fleet or ASG | Scales with RunInstances |
| EC2 Spot Fleet and ASG Allocation Strategies | No Allocation Strategies |
| Use launch config/template to set instance settings | Manually set instances settings within plugin |
| Custom instance weighting | No custom instance weighting |

# Change Log

This plugin is using [SemVersion](https://semver.org/) which means that each plugin version looks like 
```
<major>.<minor>.<bugfix>

major = increase only if non back compatible changes
minor = increase when new features
bugfix = increase when bug fixes
```

As a result, you can safely update the plugin to any version until the first number is different than what you have.

Releases: https://github.com/jenkinsci/ec2-fleet-plugin/releases

# Usage

## Setup

#### 1. Create AWS Account

Go to [AWS account](http://aws.amazon.com/ec2/) and follow instructions.

#### 2. Create IAM User

Specify ```programmatic access``` during creation and record the credentials. These will
be used by Jenkins EC2 Fleet Plugin to connect to your Spot Fleet.

*Alternatively, you may use [AWS EC2 instance roles](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2.html)*

#### 3. Configure User permissions

Add an inline policy to the IAM user or EC2 instance role to allow it to use EC2 Spot Fleet and Auto Scaling Group.
[AWS documentation about this](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet-requests.html#spot-fleet-prerequisites)

```json
{
   "Version":"2012-10-17",
   "Statement":[
      {
         "Effect":"Allow",
         "Action":[
            "ec2:DescribeSpotFleetInstances",
            "ec2:ModifySpotFleetRequest",
            "ec2:CreateTags",
            "ec2:DescribeRegions",
            "ec2:DescribeInstances",
            "ec2:TerminateInstances",
            "ec2:DescribeInstanceStatus",
            "ec2:DescribeSpotFleetRequests"
         ],
         "Resource":"*"
      },
      {
         "Effect":"Allow",
         "Action":[
            "autoscaling:DescribeAutoScalingGroups",
            "autoscaling:UpdateAutoScalingGroup"
         ],
         "Resource":"*"
      },
      {
         "Effect":"Allow",
         "Action":[
            "iam:ListInstanceProfiles",
            "iam:ListRoles",
            "iam:PassRole"
         ],
         "Resource":"*"
      }
   ]
}
```

#### 4. Create EC2 Fleet / Auto-Scaling Group

*Create EC2 Spot Fleet*

https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet-requests.html#create-spot-fleet

Make sure that you:
- Check ```Maintain target capacity``` [why](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-fleet-configuration-strategies.html#ec2-fleet-request-type)
- Specify an SSH key that will be used later by Jenkins.

*Alternatively, create an Auto Scaling Group*

https://docs.aws.amazon.com/autoscaling/ec2/userguide/GettingStartedTutorial.html

#### 5. Configure Jenkins

Once the Spot Fleet or ASG is ready, you can use it by adding a new **EC2 Fleet** cloud in the Jenkins configuration.

1. Goto ```Manage Jenkins > Plugin Manager```
1. Install ```EC2 Fleet Jenkins Plugin```
1. Goto ```Manage Jenkins > Configure System```
1. Click ```Add a new cloud``` and select ```Amazon EC2 Fleet```
1. Configure AWS credentials, or alternatively leave empty to use the EC2 instance role
1. Specify EC2 Spot Fleet or Auto Scaling Group which you want to use

## Scaling
You can specify the scaling limits in your cloud settings. By default, Jenkins will try to scale the fleet up
if there are enough tasks waiting in the build queue and scale down idle nodes after a specified idleness period.

You can use the History tab in the AWS console to view the scaling history.

## Groovy

Below is a Groovy script to setup EC2 Spot Fleet Plugin for Jenkins and configure it. You can
run the script with [Jenkins Script Console](https://wiki.jenkins.io/display/JENKINS/Jenkins+Script+Console).

```groovy
import com.amazonaws.services.ec2.model.InstanceType
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl
import hudson.plugins.sshslaves.SSHConnector
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.Domain
import hudson.model.*
import com.amazon.jenkins.ec2fleet.EC2FleetCloud
import jenkins.model.Jenkins

// just modify this config other code just logic
config = [
    region: "us-east-1",
    // EC2 Spot Fleet ID
    // or Auto Scaling Group Name
    fleetId: "...", 
    idleMinutes: 10,
    minSize: 0,
    maxSize: 10,
    numExecutors: 1,
    awsKeyId: "...",
    secretKey: "...",
    ec2PrivateKey: '''-----BEGIN RSA PRIVATE KEY-----
...
-----END RSA PRIVATE KEY-----'''
]

// https://github.com/jenkinsci/aws-credentials-plugin/blob/aws-credentials-1.23/src/main/java/com/cloudbees/jenkins/plugins/awscredentials/AWSCredentialsImpl.java
AWSCredentialsImpl awsCredentials = new AWSCredentialsImpl(
  CredentialsScope.GLOBAL,
  "aws-credentials",
  config.awsKeyId,
  config.secretKey,
  "my aws credentials"
)
 
BasicSSHUserPrivateKey instanceCredentials = new BasicSSHUserPrivateKey(
  CredentialsScope.GLOBAL,
  "instance-ssh-key",
  "ec2-user",
  new DirectEntryPrivateKeySource(config.ec2PrivateKey),
  "", 
  "my private key to ssh ec2 for jenkins"
)
 
// find detailed information about parameters on plugin config page or
// https://github.com/jenkinsci/ec2-fleet-plugin/blob/master/src/main/java/com/amazon/jenkins/ec2fleet/EC2FleetCloud.java
EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
  "", // fleetCloudName 
  awsCredentials.id,
  "",
  config.region,
  config.fleetId,
  "ec2-fleet",  // labels
  "", // fs root
  new SSHConnector(22, 
                   instanceCredentials.id, "", "", "", "", null, 0, 0, 
                   // consult doc for line below, this one say no host verification, but you can use more strict mode
                   // https://github.com/jenkinsci/ssh-slaves-plugin/blob/master/src/main/java/hudson/plugins/sshslaves/verifiers/NonVerifyingKeyVerificationStrategy.java
                   new NonVerifyingKeyVerificationStrategy()),
  false, // if need to use privateIpUsed
  false, // if need alwaysReconnect
  config.idleMinutes, // if need to allow downscale set > 0 in min
  config.minSize, // minSize
  config.maxSize, // maxSize
  config.numExecutors, // numExecutors
  false, // addNodeOnlyIfRunning
  false, // restrictUsage allow execute only jobs with proper label
)
 
// get Jenkins instance
Jenkins jenkins = Jenkins.getInstance()
// get credentials domain
def domain = Domain.global()
// get credentials store
def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
// add credential to store
store.addCredentials(domain, awsCredentials)
store.addCredentials(domain, instanceCredentials)
// add cloud configuration to Jenkins
jenkins.clouds.add(ec2FleetCloud)
// save current Jenkins state to disk
jenkins.save()
```

## Preconfigure Slave

Sometimes you need to prepare a slave (an EC2 instance) before Jenkins can use it.
For example, you need to install some software which is required by your builds like Maven, etc.

For those cases you have a few options, described below:

### Amazon EC2 AMI

[AMI](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html) allows you to 
create custom images for your EC2 instances. For example, you can create an image with
Linux plus Java, Maven etc. Then, when EC2 Fleet launches new EC2 instances with
this AMI they will automatically get all the required software. Nice =)

1. Create a custom AMI as described [here](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html#creating-an-ami)
1. Create EC2 Spot Fleet with this AMI

### EC2 Instance User Data

EC2 instances allow you to specify a [User Data](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html)
script that is executed when an instance first launches. This allows you to customize the setup for a particular instance.

#### SSH Prefix Verification

EC2 instances don't provide any information about the User Data script execution status,
so Jenkins could start a task on a new instance while the script is still in progress. Most of the time Jenkins will
repeatedly try to connect to the instance during this time and print out errors until the script completes and Jenkins
can connect.

To avoid those errors, you can use the Jenkins SSH Launcher ```Prefix Start Agent Command``` setting
to specify a command which should fail if User Data is not finished. In that way Jenkins will
not be able to connect to the instance until the User Data script is done. More information on configuring the SSH
launcher can be found [here](https://github.com/jenkinsci/ssh-slaves-plugin/blob/master/doc/CONFIGURE.md).

1. Open Jenkins
1. Go to ```Manage Jenkins > Configure System```
1. Find proper fleet configuration and click ```Advanced...``` for SSH Launcher
1. Add checking command into field ```Prefix Start Slave Command```
   - example ```java -version && ```
1. To apply for existing instances, restart Jenkins or Delete Nodes from Jenkins so they will be reconnected

# Development

Plugin usage statistics per Jenkins version can be found [here](https://stats.jenkins.io/pluginversions/ec2-fleet.html)

## Releasing

https://jenkins.io/doc/developer/publishing/releasing/

```bash
mvn release:prepare release:perform
```

### Jenkins 2 can't connect by SSH 

https://issues.jenkins-ci.org/browse/JENKINS-53954

### Install Java 8 on EC2 instance 

Regular script:

```bash
sudo yum install java-1.8.0 -y
sudo yum remove java-1.7.0-openjdk -y
java -version 
```

User Data Script:

*Note* ```sudo``` is not required, ```-y``` suppresses confirmation.
Don't forget to encode with Base64

```bash
#!/bin/bash
yum install java-1.8.0 -y && yum remove java-1.7.0-openjdk -y && java -version
```

